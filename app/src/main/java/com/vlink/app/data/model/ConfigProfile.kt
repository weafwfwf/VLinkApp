package com.vlink.app.data.model

/**
 * A single proxy outbound configuration (one server) — the equivalent of
 * one entry you'd see in v2rayNG's server list.
 *
 * This is intentionally protocol-agnostic at the top level; protocol-specific
 * fields live in [raw] as the original share-link / JSON so the real V2Ray
 * core config (JSON) can be rebuilt exactly when the tunnel starts.
 */
data class ConfigProfile(
    val id: String,                // stable UUID, generated on import
    val name: String,               // display / remark name
    val protocol: Protocol,
    val address: String,
    val port: Int,
    val subscriptionId: String?,    // null if added manually (not from a sub)
    val rawLink: String,            // original vmess://, vless://, etc. link
    val lastPingMs: Long? = null,   // null = not tested yet, -1 = failed/timeout
    val lastTestedAt: Long? = null
)

enum class Protocol {
    VMESS, VLESS, TROJAN, SHADOWSOCKS, UNKNOWN
}
