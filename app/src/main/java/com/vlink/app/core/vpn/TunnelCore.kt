package com.vlink.app.core.vpn

/**
 * Abstraction over "whatever native V2Ray/Xray core library you vendor in".
 * Everything else in the app (VpnService, ping tester) talks to THIS
 * interface, not to the native library directly — so swapping in the real
 * core later means implementing one class, not touching the rest of the app.
 *
 * ============================ PLUG-IN POINT =================================
 * Once you add a real core AAR (e.g. AndroidLibXrayLite's libv2ray.aar):
 *
 *   1. Create `XrayNativeCore : TunnelCore` (new file next to this one).
 *   2. Typical AndroidLibXrayLite-style API looks like:
 *
 *        val point = Libv2ray.newV2RayPoint(callbackHandler, /* useVpnService = */ true)
 *        point.configureFileContent = configJson
 *        point.probeUrl = "https://www.gstatic.com/generate_204"
 *        point.runLoop(/* preferIpv6 = */ false)   // start
 *        point.stopLoop()                           // stop
 *        point.queryStats("proxy", "downlink")       // stats
 *
 *      `callbackHandler` implements the core's callback interface, whose
 *      most important method is `protect(fd: Long): Boolean` — this MUST
 *      call this Android VpnService's `protect(fd)` so the core's own
 *      outbound sockets don't get routed back into the VPN (infinite loop).
 *
 *   3. Wire the real TUN file descriptor from V2RayVpnService into the
 *      core's tun2socks layer (AndroidLibXrayLite bundles a tun2socks
 *      binary/lib for this — check its README for the exact call).
 *
 *   4. Replace `NoOpTunnelCore` below with `XrayNativeCore` wherever
 *      `TunnelCore` is instantiated (see `TunnelCore.create()`).
 *
 * Exact method names differ between forks (v2rayNG's own fork, Xray4Android,
 * AndroidLibXrayLite, etc.) — treat the snippet above as "shape", not a
 * guaranteed exact signature; check the README of whichever AAR you pick.
 * ============================================================================
 */
interface TunnelCore {

    /** Starts the core with a full tunnel config (from ConfigBuilder.build(profile)),
     *  routing packets from [tunFd] (the VpnService TUN interface fd). */
    fun startTunnel(configJson: String, tunFd: Int): Boolean

    /** Stops whatever startTunnel started. */
    fun stopTunnel()

    /**
     * Starts a throwaway local instance for a single config, exposing a
     * local SOCKS proxy on 127.0.0.1:[port] (no TUN interface involved) —
     * used only for the "real" per-config ping test.
     * Returns true if it started successfully.
     */
    fun startEphemeral(configJson: String, port: Int): Boolean

    fun stopEphemeral()

    /** True once a real native core is wired in (see PLUG-IN POINT above). */
    val isReal: Boolean

    companion object {
        fun create(): TunnelCore = NoOpTunnelCore()
        // Once you've implemented XrayNativeCore:
        // fun create(): TunnelCore = XrayNativeCore()
    }
}

/**
 * Placeholder so the app builds and runs before the native core is vendored
 * in. `startTunnel` always reports failure (so the UI can show "core not
 * installed" instead of silently pretending to be connected), and
 * `startEphemeral` returns false so [PingTester] falls back to the plain
 * TCP handshake test.
 */
private class NoOpTunnelCore : TunnelCore {
    override val isReal = false
    override fun startTunnel(configJson: String, tunFd: Int): Boolean = false
    override fun stopTunnel() {}
    override fun startEphemeral(configJson: String, port: Int): Boolean = false
    override fun stopEphemeral() {}
}
