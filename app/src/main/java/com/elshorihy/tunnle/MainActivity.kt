package com.elshorihy.tunnle

import android.app.Activity
import android.content.Intent
import android.net.TrafficStats
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var info: TextView
    private lateinit var downloadText: TextView
    private lateinit var uploadText: TextView
    private lateinit var pingText: TextView
    private lateinit var tunnelText: TextView
    private lateinit var serverText: TextView
    private lateinit var sharingStatus: TextView
    private lateinit var connectionButton: ConnectionRingView
    private lateinit var shareSwitch: Switch

    private var connected = false
    private var connecting = false
    private var connectedAt = 0L
    private var lastRx = TrafficStats.getTotalRxBytes()
    private var lastTx = TrafficStats.getTotalTxBytes()
    private var lastStatsAt = System.currentTimeMillis()
    private val vpnRequest = 1001
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            if (connected) {
                val now = System.currentTimeMillis()
                val seconds = ((now - connectedAt) / 1000).toInt()
                info.text = String.format(Locale.US, "Connected  •  %d:%02d", seconds / 60, seconds % 60)
                updateTrafficStats(now)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.pageTitle)
        info = findViewById(R.id.info)
        downloadText = findViewById(R.id.downloadText)
        uploadText = findViewById(R.id.uploadText)
        pingText = findViewById(R.id.pingText)
        tunnelText = findViewById(R.id.tunnelText)
        serverText = findViewById(R.id.serverText)
        sharingStatus = findViewById(R.id.sharingStatus)
        connectionButton = findViewById(R.id.connectionRing)
        shareSwitch = findViewById(R.id.shareSwitch)

        connectionButton.setOnClickListener {
            if (connected) disconnectVpn() else requestVpnPermission()
        }

        shareSwitch.setOnCheckedChangeListener { button, checked ->
            if (checked) {
                button.isChecked = false
                sharingStatus.text = if (connected) {
                    "Sharing engine will be enabled with the tunnel core"
                } else {
                    "Connect VPN first"
                }
            } else {
                sharingStatus.text = "Share this VPN connection"
            }
        }

        findViewById<TextView>(R.id.headerMenu).setOnClickListener {
            info.text = "Elshori7y Tunnle  •  Secure networking"
        }

        findViewById<Button>(R.id.homeButton).setOnClickListener { showHome() }
        findViewById<Button>(R.id.profilesButton).setOnClickListener { showProfiles() }
        findViewById<Button>(R.id.serversButton).setOnClickListener { showServers() }
        findViewById<Button>(R.id.logsButton).setOnClickListener { showLogs() }
        findViewById<Button>(R.id.settingsButton).setOnClickListener { showSettings() }
    }

    private fun showHome() {
        status.text = "Secure connection"
        info.text = if (connected) "Connected" else "Ready to connect"
        serverText.text = "Auto server  •  VPN"
        tunnelText.text = if (connected) "SECURE TUNNEL ACTIVE" else "SECURE TUNNEL"
    }

    private fun showProfiles() {
        status.text = "Profiles"
        info.text = "DEFAULT\nBasic VPN profile\n\nAdvanced profile import will be connected to the tunnel core next."
    }

    private fun showServers() {
        status.text = "Servers"
        serverText.text = "Egypt • Recommended\nAuto selection"
        info.text = "Server selection\n\nAuto server is currently selected."
    }

    private fun showLogs() {
        status.text = "Connection Logs"
        info.text = if (connected) {
            "Activity Log\nVPN connected\nService is running\n\nSession timer is active"
        } else {
            "Activity Log\nNo active connection"
        }
    }

    private fun showSettings() {
        status.text = "Settings"
        info.text = "Connection behavior\nNotifications\nVPN Sharing\nAppearance\n\nAdvanced tunnel settings will be added with the tunnel core."
    }

    private fun requestVpnPermission() {
        connecting = true
        connectionButton.setState(false, true)
        status.text = "Preparing secure connection"
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, vpnRequest)
        } else {
            startVpn()
        }
    }

    @Deprecated("Use Activity Result APIs in a future refactor")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == vpnRequest) {
            if (resultCode == RESULT_OK) startVpn() else resetDisconnected("VPN permission was cancelled")
        }
    }

    private fun startVpn() {
        startService(Intent(this, TunnelVpnService::class.java))
        connected = true
        connecting = false
        connectedAt = System.currentTimeMillis()
        lastRx = TrafficStats.getTotalRxBytes()
        lastTx = TrafficStats.getTotalTxBytes()
        lastStatsAt = connectedAt
        connectionButton.setState(true)
        status.text = "Secure connection"
        tunnelText.text = "SECURE TUNNEL ACTIVE"
        info.text = "Connected  •  0:00"
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun disconnectVpn() {
        stopService(Intent(this, TunnelVpnService::class.java))
        resetDisconnected("Disconnected")
    }

    private fun updateTrafficStats(now: Long) {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val elapsed = (now - lastStatsAt).coerceAtLeast(1L)
        val rxRate = ((rx - lastRx).coerceAtLeast(0L) * 1000L) / elapsed
        val txRate = ((tx - lastTx).coerceAtLeast(0L) * 1000L) / elapsed
        downloadText.text = "↓ ${formatRate(rxRate)}\nDownload"
        uploadText.text = "↑ ${formatRate(txRate)}\nUpload"
        pingText.text = "⌁ —\nPing"
        lastRx = rx
        lastTx = tx
        lastStatsAt = now
    }

    private fun formatRate(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", bytesPerSecond / 1024.0 / 1024.0)
            bytesPerSecond >= 1024 -> String.format(Locale.US, "%.0f KB/s", bytesPerSecond / 1024.0)
            else -> "$bytesPerSecond B/s"
        }
    }

    private fun resetDisconnected(message: String) {
        connected = false
        connecting = false
        handler.removeCallbacks(ticker)
        connectionButton.setState(false, false)
        status.text = "Secure connection"
        tunnelText.text = "SECURE TUNNEL"
        downloadText.text = "↓ 0 B/s\nDownload"
        uploadText.text = "↑ 0 B/s\nUpload"
        pingText.text = "⌁ —\nPing"
        info.text = message
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }
}
