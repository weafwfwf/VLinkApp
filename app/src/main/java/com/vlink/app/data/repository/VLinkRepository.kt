package com.vlink.app.data.repository

import com.vlink.app.core.parser.ConfigParser
import com.vlink.app.core.ping.PingTester
import com.vlink.app.data.db.AppDatabase
import com.vlink.app.data.db.ConfigEntity
import com.vlink.app.data.db.SubscriptionEntity
import com.vlink.app.data.model.ConfigProfile
import com.vlink.app.data.model.Protocol
import com.vlink.app.data.model.Subscription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID

class VLinkRepository(private val db: AppDatabase) {

    private val http = OkHttpClient()

    // ---- Subscriptions ----

    fun observeSubscriptions(): Flow<List<Subscription>> =
        db.subscriptionDao().observeAll().map { list ->
            list.map { Subscription(it.id, it.name, it.url, it.enabled, it.lastUpdatedAt) }
        }

    suspend fun addSubscription(name: String, url: String) {
        db.subscriptionDao().upsert(
            SubscriptionEntity(UUID.randomUUID().toString(), name, url, enabled = true, lastUpdatedAt = null)
        )
    }

    /** Fetches a subscription URL's payload and replaces its config list. */
    suspend fun refreshSubscription(subscription: Subscription) {
        val request = Request.Builder().url(subscription.url).build()
        val body = http.newCall(request).execute().use { it.body?.string().orEmpty() }
        val configs = ConfigParser.parseSubscriptionPayload(body, subscription.id)

        db.configDao().deleteForSubscription(subscription.id)
        db.configDao().upsertAll(configs.map { it.toEntity() })
        db.subscriptionDao().upsert(
            SubscriptionEntity(
                subscription.id, subscription.name, subscription.url,
                subscription.enabled, System.currentTimeMillis()
            )
        )
    }

    suspend fun removeSubscription(subscription: Subscription) {
        db.configDao().deleteForSubscription(subscription.id)
        db.subscriptionDao().delete(
            SubscriptionEntity(subscription.id, subscription.name, subscription.url, subscription.enabled, subscription.lastUpdatedAt)
        )
    }

    // ---- Configs ----

    fun observeConfigs(): Flow<List<ConfigProfile>> =
        db.configDao().observeAll().map { list -> list.map { it.toModel() } }

    suspend fun addManualConfig(link: String) {
        val parsed = ConfigParser.parse(link) ?: return
        db.configDao().upsert(parsed.toEntity())
    }

    suspend fun pingConfig(config: ConfigProfile) {
        val ms = PingTester.testOne(config)
        db.configDao().updatePingResult(config.id, if (ms < 0) null else ms, System.currentTimeMillis())
    }

    suspend fun pingAll(configs: List<ConfigProfile>) {
        val results = PingTester.testMany(configs)
        val now = System.currentTimeMillis()
        results.forEach { (id, ms) ->
            db.configDao().updatePingResult(id, if (ms < 0) null else ms, now)
        }
    }

    // ---- mapping helpers ----

    private fun ConfigProfile.toEntity() = ConfigEntity(
        id, name, protocol.name, address, port, subscriptionId, rawLink, lastPingMs, lastTestedAt
    )

    private fun ConfigEntity.toModel() = ConfigProfile(
        id, name,
        runCatching { Protocol.valueOf(protocol) }.getOrDefault(Protocol.UNKNOWN),
        address, port, subscriptionId, rawLink, lastPingMs, lastTestedAt
    )
}
