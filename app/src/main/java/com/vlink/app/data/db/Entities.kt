package com.vlink.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean,
    val lastUpdatedAt: Long?
)

@Entity(tableName = "configs")
data class ConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    val protocol: String,       // stored as String, mapped to Protocol enum in repo
    val address: String,
    val port: Int,
    val subscriptionId: String?,
    val rawLink: String,
    val lastPingMs: Long?,
    val lastTestedAt: Long?
)
