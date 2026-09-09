package com.vlink.app.core.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.vlink.app.data.db.AppDatabase
import com.vlink.app.data.model.ConfigProfile
import com.vlink.app.data.model.Protocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Real VpnService wiring: builds the core config for the selected profile,
 * establishes the TUN interface, and hands off to [TunnelCore]. With the
 * default NoOpTunnelCore this will establish the TUN and then immediately
 * report failure + tear down — see TunnelCore.kt's PLUG-IN POINT for how to
 * make it actually carry traffic.
 */
class V2RayVpnService : VpnService() {

    private var tunFd: ParcelFileDescriptor? = null
    private val core: TunnelCore = TunnelCore.create()
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val EXTRA_CONFIG_ID = "config_id"
        const val ACTION_STOP = "com.vlink.app.action.STOP"
        private const val NOTIF_CHANNEL_ID = "vlink_tunnel"
        private const val NOTIF_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTunnel()
            stopSelf()
            return START_NOT_STICKY
        }

        val configId = intent?.getStringExtra(EXTRA_CONFIG_ID)
        startForeground(NOTIF_ID, buildNotification())

        scope.launch {
            val profile = configId?.let { loadProfile(it) }
            if (profile == null) {
                stopSelf()
                return@launch
            }
            startTunnel(profile)
        }
        return START_STICKY
    }

    private suspend fun loadProfile(id: String): ConfigProfile? {
        val entity = AppDatabase.get(applicationContext).configDao().getById(id) ?: return null
        return ConfigProfile(
            id = entity.id,
            name = entity.name,
            protocol = runCatching { Protocol.valueOf(entity.protocol) }.getOrDefault(Protocol.UNKNOWN),
            address = entity.address,
            port = entity.port,
            subscriptionId = entity.subscriptionId,
            rawLink = entity.rawLink,
            lastPingMs = entity.lastPingMs,
            lastTestedAt = entity.lastTestedAt,
        )
    }

    private fun startTunnel(profile: ConfigProfile) {
        val configJson = runCatching { ConfigBuilder.build(profile) }.getOrNull()
        if (configJson == null) {
            stopSelf()
            return
        }

        val builder = Builder()
            .setSession("VLink")
            .addAddress("10.10.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .setMtu(1500)

        // Exclude our own app from the VPN so the core's own network calls
        // (e.g. re-fetching a subscription while connected) don't loop.
        runCatching { builder.addDisallowedApplication(packageName) }

        tunFd = builder.establish()
        val fd = tunFd?.fd ?: run { stopSelf(); return }

        val started = core.startTunnel(configJson, fd)
        if (!started) {
            // NoOpTunnelCore always lands here until a real core is wired in
            // (see TunnelCore.kt). Tear down cleanly rather than pretending
            // to be connected.
            stopTunnel()
            stopSelf()
        }
    }

    private fun stopTunnel() {
        core.stopTunnel()
        tunFd?.close()
        tunFd = null
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIF_CHANNEL_ID, "VLink Tunnel", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("VLink")
            .setContentText("در حال اتصال...")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }
}
