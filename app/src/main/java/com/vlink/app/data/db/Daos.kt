package com.vlink.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sub: SubscriptionEntity)

    @Delete
    suspend fun delete(sub: SubscriptionEntity)
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM configs")
    fun observeAll(): Flow<List<ConfigEntity>>

    @Query("SELECT * FROM configs WHERE subscriptionId = :subId")
    fun observeForSubscription(subId: String): Flow<List<ConfigEntity>>

    @Query("SELECT * FROM configs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(configs: List<ConfigEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: ConfigEntity)

    @Query("DELETE FROM configs WHERE subscriptionId = :subId")
    suspend fun deleteForSubscription(subId: String)

    @Delete
    suspend fun delete(config: ConfigEntity)

    @Query("UPDATE configs SET lastPingMs = :pingMs, lastTestedAt = :testedAt WHERE id = :id")
    suspend fun updatePingResult(id: String, pingMs: Long?, testedAt: Long)
}
