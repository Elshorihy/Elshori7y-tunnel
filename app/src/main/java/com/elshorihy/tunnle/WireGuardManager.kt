package com.elshorihy.tunnle

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream

/** Real WireGuard control layer. A profile only becomes connectable when it contains a valid WireGuard .conf. */
class WireGuardManager(context: Context) {
    private val backend = GoBackend(context.applicationContext)
    private var activeTunnel: AppTunnel? = null

    @Synchronized
    fun connect(profile: ServerProfile): Result<Unit> = runCatching {
        val raw = profile.wireGuardConfig
            ?: error("This profile has no WireGuard configuration")
        require(raw.isNotBlank()) { "WireGuard configuration is empty" }

        val config = Config.parse(ByteArrayInputStream(raw.toByteArray(Charsets.UTF_8)))
        val tunnel = AppTunnel(profile.id)
        backend.setState(tunnel, Tunnel.State.UP, config)
        activeTunnel = tunnel
    }

    @Synchronized
    fun disconnect(): Result<Unit> = runCatching {
        activeTunnel?.let { backend.setState(it, Tunnel.State.DOWN, null) }
        activeTunnel = null
    }

    fun isConnected(): Boolean = activeTunnel?.let {
        backend.getState(it) == Tunnel.State.UP
    } == true

    private class AppTunnel(private val tunnelName: String) : Tunnel {
        @Volatile private var state = Tunnel.State.DOWN

        override fun getName(): String = tunnelName

        override fun onStateChange(newState: Tunnel.State) {
            state = newState
        }
    }
}
