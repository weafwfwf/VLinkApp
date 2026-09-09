package com.vlink.app.core.parser

import android.util.Base64
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder

/**
 * Full field sets needed to build a real V2Ray/Xray outbound JSON — the
 * lightweight ConfigProfile only keeps name/address/port for list display,
 * this is the richer parse used right before starting a tunnel or doing a
 * real (through-the-core) ping test.
 */
sealed class ParsedOutbound {
    abstract val address: String
    abstract val port: Int

    data class Vmess(
        override val address: String, override val port: Int,
        val id: String, val alterId: Int, val security: String,
        val network: String, val host: String?, val path: String?,
        val tls: Boolean, val sni: String?,
    ) : ParsedOutbound()

    data class Vless(
        override val address: String, override val port: Int,
        val id: String, val flow: String?, val encryption: String,
        val network: String, val host: String?, val path: String?,
        val security: String, val sni: String?,
    ) : ParsedOutbound()

    data class Trojan(
        override val address: String, override val port: Int,
        val password: String, val sni: String?, val network: String,
    ) : ParsedOutbound()

    data class Shadowsocks(
        override val address: String, override val port: Int,
        val method: String, val password: String,
    ) : ParsedOutbound()
}

object FullConfigParser {

    fun parse(rawLink: String): ParsedOutbound? = runCatching {
        when {
            rawLink.startsWith("vmess://") -> parseVmess(rawLink)
            rawLink.startsWith("vless://") -> parseVless(rawLink)
            rawLink.startsWith("trojan://") -> parseTrojan(rawLink)
            rawLink.startsWith("ss://") -> parseShadowsocks(rawLink)
            else -> null
        }
    }.getOrNull()

    private fun parseVmess(link: String): ParsedOutbound.Vmess {
        val json = JSONObject(String(Base64.decode(link.removePrefix("vmess://"), Base64.DEFAULT)))
        return ParsedOutbound.Vmess(
            address = json.getString("add"),
            port = json.getString("port").toInt(),
            id = json.getString("id"),
            alterId = json.optString("aid", "0").toIntOrNull() ?: 0,
            security = json.optString("scy", "auto"),
            network = json.optString("net", "tcp"),
            host = json.optString("host").ifBlank { null },
            path = json.optString("path").ifBlank { null },
            tls = json.optString("tls") == "tls",
            sni = json.optString("sni").ifBlank { null },
        )
    }

    private fun parseVless(link: String): ParsedOutbound.Vless {
        val uri = URI(link)
        val params = uri.query.orEmpty().split("&").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to URLDecoder.decode(parts[1], "UTF-8") else null
        }.toMap()
        return ParsedOutbound.Vless(
            address = uri.host, port = uri.port,
            id = uri.userInfo,
            flow = params["flow"],
            encryption = params["encryption"] ?: "none",
            network = params["type"] ?: "tcp",
            host = params["host"],
            path = params["path"],
            security = params["security"] ?: "none",
            sni = params["sni"],
        )
    }

    private fun parseTrojan(link: String): ParsedOutbound.Trojan {
        val uri = URI(link)
        val params = uri.query.orEmpty().split("&").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to URLDecoder.decode(parts[1], "UTF-8") else null
        }.toMap()
        return ParsedOutbound.Trojan(
            address = uri.host, port = uri.port,
            password = uri.userInfo,
            sni = params["sni"],
            network = params["type"] ?: "tcp",
        )
    }

    private fun parseShadowsocks(link: String): ParsedOutbound.Shadowsocks {
        val withoutScheme = link.removePrefix("ss://")
        val body = withoutScheme.substringBefore('#')
        val atIndex = body.indexOf('@')
        val (methodPass, hostPort) = if (atIndex >= 0) {
            body.substring(0, atIndex) to body.substring(atIndex + 1)
        } else {
            val decoded = String(Base64.decode(body, Base64.DEFAULT))
            decoded.substringBefore('@') to decoded.substringAfter('@')
        }
        val decodedMethodPass = runCatching {
            String(Base64.decode(methodPass, Base64.DEFAULT))
        }.getOrDefault(methodPass) // some clients don't base64 this part
        val method = decodedMethodPass.substringBefore(':')
        val password = decodedMethodPass.substringAfter(':')

        return ParsedOutbound.Shadowsocks(
            address = hostPort.substringBeforeLast(':'),
            port = hostPort.substringAfterLast(':').toInt(),
            method = method,
            password = password,
        )
    }
}
