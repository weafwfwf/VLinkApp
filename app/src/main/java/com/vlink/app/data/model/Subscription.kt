package com.vlink.app.data.model

/**
 * A subscription source — a URL that returns a (usually base64-encoded)
 * list of config links. The app can hold many of these at once, each
 * refreshed independently, which is the "چند ساب همزمان" requirement.
 */
data class Subscription(
    val id: String,          // stable UUID
    val name: String,        // user-facing label, e.g. "Provider A"
    val url: String,
    val enabled: Boolean = true,
    val lastUpdatedAt: Long? = null,
    val configCount: Int = 0
)
