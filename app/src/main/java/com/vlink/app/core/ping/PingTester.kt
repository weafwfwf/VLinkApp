package com.vlink.app.core.ping

import com.vlink.app.core.vpn.ConfigBuilder
import com.vlink.app.core.vpn.TunnelCore
import com.vlink.app.data.model.ConfigProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Measures latency for configs, two ways:
 *
 *  - "Real" ping: spins up a throwaway local SOCKS proxy for just this one
 *    config via [TunnelCore.startEphemeral] and times an HTTP request
 *    through it — this is what v2rayNG shows, since it reflects the actual
 *    proxied path, not just "can I open a TCP socket to the server".
 *    Only works once a real TunnelCore is wired in (see TunnelCore.kt).
 *
 *  - TCP fallback: a plain handshake to (address, port). Used automatically
 *    whenever the real core isn't available yet — still useful as a basic
 *    reachability check.
 */
object PingTester {

    private const val TIMEOUT_MS = 3000
    private val core: TunnelCore = TunnelCore.create()

    suspend fun testOne(config: ConfigProfile): Long = withContext(Dispatchers.IO) {
        if (core.isReal) {
            testThroughCore(config)?.let { return@withContext it }
        }
        testTcpHandshake(config)
    }

    suspend fun testMany(configs: List<ConfigProfile>): Map<String, Long> = coroutineScope {
        configs.map { config ->
            async { config.id to testOne(config) }
        }.map { it.await() }.toMap()
    }

    private fun testTcpHandshake(config: ConfigProfile): Long = runCatching {
        val start = System.currentTimeMillis()
        Socket().use { it.connect(InetSocketAddress(config.address, config.port), TIMEOUT_MS) }
        System.currentTimeMillis() - start
    }.getOrDefault(-1L)

    /** Returns null (not -1) when the real path itself couldn't be attempted at all,
     *  so the caller falls back to TCP instead of reporting a false timeout. */
    private fun testThroughCore(config: ConfigProfile): Long? {
        val port = Random.nextInt(20000, 40000)
        val configJson = runCatching { ConfigBuilder.build(config, socksInboundPort = port) }.getOrNull()
            ?: return null

        if (!core.startEphemeral(configJson, port)) return null

        return try {
            val client = OkHttpClient.Builder()
                .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port)))
                .connectTimeout(TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .build()
            val request = Request.Builder().url("https://www.gstatic.com/generate_204").build()

            val start = System.currentTimeMillis()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 204) {
                    System.currentTimeMillis() - start
                } else -1L
            }
        } catch (e: Exception) {
            -1L
        } finally {
            core.stopEphemeral()
        }
    }
}
