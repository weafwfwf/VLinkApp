package com.vlink.app.core.parser

import android.util.Base64
import com.vlink.app.data.model.ConfigProfile
import com.vlink.app.data.model.Protocol
import org.json.JSONObject
import java.net.URI
import java.util.UUID

/**
 * Parses share-links (vmess://, vless://, trojan://, ss://) into a
 * [ConfigProfile]. Does NOT build the actual V2Ray/Xray core JSON config
 * yet — that happens in the vpn module right before starting the tunnel,
 * using rawLink as the source of truth.
 */
object ConfigParser {

    fun parse(link: String, subscriptionId: String? = null): ConfigProfile? {
        val trimmed = link.trim()
        return when {
            trimmed.startsWith("vmess://") -> parseVmess(trimmed, subscriptionId)
            trimmed.startsWith("vless://") -> parseVless(trimmed, subscriptionId)
            trimmed.startsWith("trojan://") -> parseTrojan(trimmed, subscriptionId)
            trimmed.startsWith("ss://") -> parseShadowsocks(trimmed, subscriptionId)
            else -> null
        }
    }

    /** Parses a full subscription payload: base64 blob of newline-separated links. */
    fun parseSubscriptionPayload(payload: String, subscriptionId: String): List<ConfigProfile> {
        val decoded = runCatching {
            String(Base64.decode(payload.trim(), Base64.DEFAULT))
        }.getOrDefault(payload) // some subs are already plaintext

        return decoded.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { parse(it, subscriptionId) }
    }

    private fun parseVmess(link: String, subscriptionId: String?): ConfigProfile? = runCatching {
        val b64 = link.removePrefix("vmess://")
        val json = JSONObject(String(Base64.decode(b64, Base64.DEFAULT)))
        ConfigProfile(
            id = UUID.randomUUID().toString(),
            name = json.optString("ps", "vmess"),
            protocol = Protocol.VMESS,
            address = json.getString("add"),
            port = json.getString("port").toInt(),
            subscriptionId = subscriptionId,
            rawLink = link
        )
    }.getOrNull()

    private fun parseVless(link: String, subscriptionId: String?): ConfigProfile? = runCatching {
        val uri = URI(link)
        ConfigProfile(
            id = UUID.randomUUID().toString(),
            name = uri.fragment?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "vless",
            protocol = Protocol.VLESS,
            address = uri.host,
            port = uri.port,
            subscriptionId = subscriptionId,
            rawLink = link
        )
    }.getOrNull()

    private fun parseTrojan(link: String, subscriptionId: String?): ConfigProfile? = runCatching {
        val uri = URI(link)
        ConfigProfile(
            id = UUID.randomUUID().toString(),
            name = uri.fragment?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "trojan",
            protocol = Protocol.TROJAN,
            address = uri.host,
            port = uri.port,
            subscriptionId = subscriptionId,
            rawLink = link
        )
    }.getOrNull()

    private fun parseShadowsocks(link: String, subscriptionId: String?): ConfigProfile? = runCatching {
        // ss://BASE64(method:password)@host:port#name  OR fully base64'd variant
        val withoutScheme = link.removePrefix("ss://")
        val hashIndex = withoutScheme.indexOf('#')
        val name = if (hashIndex >= 0) {
            java.net.URLDecoder.decode(withoutScheme.substring(hashIndex + 1), "UTF-8")
        } else "shadowsocks"
        val body = if (hashIndex >= 0) withoutScheme.substring(0, hashIndex) else withoutScheme

        val atIndex = body.indexOf('@')
        val hostPort: String
        if (atIndex >= 0) {
            hostPort = body.substring(atIndex + 1)
        } else {
            // fully base64-encoded form: base64(method:password@host:port)
            val decoded = String(Base64.decode(body, Base64.DEFAULT))
            hostPort = decoded.substringAfter('@')
        }
        val host = hostPort.substringBeforeLast(':')
        val port = hostPort.substringAfterLast(':').toInt()

        ConfigProfile(
            id = UUID.randomUUID().toString(),
            name = name,
            protocol = Protocol.SHADOWSOCKS,
            address = host,
            port = port,
            subscriptionId = subscriptionId,
            rawLink = link
        )
    }.getOrNull()
}
