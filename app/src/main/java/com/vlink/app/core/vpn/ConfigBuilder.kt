package com.vlink.app.core.vpn

import com.vlink.app.core.parser.FullConfigParser
import com.vlink.app.core.parser.ParsedOutbound
import com.vlink.app.data.model.ConfigProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds the JSON config that the Xray/V2Ray core actually consumes.
 * This is the standard Xray config schema (inbounds/outbounds/routing) —
 * the same shape v2rayNG generates before handing it to libv2ray.
 *
 * Covers: vmess, vless, trojan, shadowsocks, over tcp/ws, with optional TLS.
 * NOT covered yet: grpc, kcp, quic, reality, mux fine-tuning — add as needed.
 */
object ConfigBuilder {

    /**
     * @param socksInboundPort if set, adds a local SOCKS inbound on 127.0.0.1
     *   at that port (used for the one-off "real ping" test). If null, adds
     *   the "dokodemo-door"-less inbound expected when running as the actual
     *   VPN tunnel driven by tun2socks (the fd side is handled by the core
     *   adapter, not by this JSON).
     */
    fun build(profile: ConfigProfile, socksInboundPort: Int? = null): String {
        val parsed = FullConfigParser.parse(profile.rawLink)
            ?: error("Could not parse rawLink for ${profile.id}")

        val root = JSONObject()
        root.put("log", JSONObject().put("loglevel", "warning"))

        val inbounds = JSONArray()
        if (socksInboundPort != null) {
            inbounds.put(
                JSONObject()
                    .put("tag", "socks-in")
                    .put("listen", "127.0.0.1")
                    .put("port", socksInboundPort)
                    .put("protocol", "socks")
                    .put("settings", JSONObject().put("udp", true))
            )
        }
        root.put("inbounds", inbounds)

        val outbound = JSONObject().put("tag", "proxy")
        when (parsed) {
            is ParsedOutbound.Vmess -> buildVmessOutbound(outbound, parsed)
            is ParsedOutbound.Vless -> buildVlessOutbound(outbound, parsed)
            is ParsedOutbound.Trojan -> buildTrojanOutbound(outbound, parsed)
            is ParsedOutbound.Shadowsocks -> buildShadowsocksOutbound(outbound, parsed)
        }
        root.put("outbounds", JSONArray().put(outbound).put(
            JSONObject().put("tag", "direct").put("protocol", "freedom")
        ))

        return root.toString()
    }

    private fun buildVmessOutbound(outbound: JSONObject, c: ParsedOutbound.Vmess) {
        outbound.put("protocol", "vmess")
        outbound.put("settings", JSONObject().put("vnext", JSONArray().put(
            JSONObject()
                .put("address", c.address).put("port", c.port)
                .put("users", JSONArray().put(
                    JSONObject().put("id", c.id).put("alterId", c.alterId).put("security", c.security)
                ))
        )))
        outbound.put("streamSettings", streamSettings(c.network, c.host, c.path, c.tls, c.sni))
    }

    private fun buildVlessOutbound(outbound: JSONObject, c: ParsedOutbound.Vless) {
        outbound.put("protocol", "vless")
        val user = JSONObject().put("id", c.id).put("encryption", c.encryption)
        c.flow?.let { user.put("flow", it) }
        outbound.put("settings", JSONObject().put("vnext", JSONArray().put(
            JSONObject().put("address", c.address).put("port", c.port)
                .put("users", JSONArray().put(user))
        )))
        outbound.put("streamSettings", streamSettings(c.network, c.host, c.path, c.security == "tls", c.sni))
    }

    private fun buildTrojanOutbound(outbound: JSONObject, c: ParsedOutbound.Trojan) {
        outbound.put("protocol", "trojan")
        outbound.put("settings", JSONObject().put("servers", JSONArray().put(
            JSONObject().put("address", c.address).put("port", c.port).put("password", c.password)
        )))
        // Trojan is TLS by default
        outbound.put("streamSettings", streamSettings(c.network, null, null, true, c.sni))
    }

    private fun buildShadowsocksOutbound(outbound: JSONObject, c: ParsedOutbound.Shadowsocks) {
        outbound.put("protocol", "shadowsocks")
        outbound.put("settings", JSONObject().put("servers", JSONArray().put(
            JSONObject().put("address", c.address).put("port", c.port)
                .put("method", c.method).put("password", c.password)
        )))
    }

    private fun streamSettings(network: String, host: String?, path: String?, tls: Boolean, sni: String?): JSONObject {
        val stream = JSONObject().put("network", network)
        if (network == "ws") {
            val wsSettings = JSONObject()
            path?.let { wsSettings.put("path", it) }
            host?.let { wsSettings.put("headers", JSONObject().put("Host", it)) }
            stream.put("wsSettings", wsSettings)
        }
        if (tls) {
            stream.put("security", "tls")
            val tlsSettings = JSONObject()
            sni?.let { tlsSettings.put("serverName", it) }
            stream.put("tlsSettings", tlsSettings)
        }
        return stream
    }
}
