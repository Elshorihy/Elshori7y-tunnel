package com.elshorihy.tunnle

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var info: TextView
    private lateinit var connectButtonState: ConnectionRingView
    private lateinit var shareSwitch: Switch
    private var connected = false
    private var connecting = false
    private var connectedAt = 0L
    private val vpnRequest = 1001
    private val handler = Handler(Looper.getMainLooper())

    private val timer = object : Runnable {
        override fun run() {
            if (connected) {
                val seconds = ((System.currentTimeMillis() - connectedAt) / 1000).toInt()
                info.text = "Connected  •  ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.pageTitle)
        info = findViewById(R.id.info)
        connectButtonState = findViewById(R.id.connectionRing)
        shareSwitch = findViewById(R.id.shareSwitch)

        findViewById<TextView>(R.id.headerMenu).setOnClickListener {
            info.text = "Elshori7y Tunnle  •  Premium VPN UI"
        }

        connectButtonState.setOnClickListener {
            if (connected) disconnectVpn() else requestVpnPermission()
        }

        shareSwitch.setOnCheckedChangeListener { button, checked ->
            if (checked) {
                button.isChecked = false
                info.text = if (connected) "VPN Sharing module is ready for the next engine update" else "Connect VPN first"
            }
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
    }

    private fun showProfiles() {
        status.text = "Profiles"
        info.text = "Default profile\n\nProfile import and advanced tunnel configurations are coming next."
    }

    private fun showServers() {
        status.text = "Servers"
        info.text = "Auto server\nEgypt  •  Recommended\n\nServer selection engine will be connected to the tunnel core next."
    }

    private fun showLogs() {
        status.text = "Connection Logs"
        info.text = if (connected) "Activity Log\nVPN connected\nService is running" else "Activity Log\nNo active connection"
    }

    private fun showSettings() {
        status.text = "Settings"
        info.text = "App settings\n\nNotifications  •  Connection behavior  •  Appearance"
    }

    private fun requestVpnPermission() {
        connecting = true
        connectButtonState.setState(false, true)
        status.text = "Preparing secure connection"
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, vpnRequest)
        } else {
            startVpn()
        }
    }

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
        connectButtonState.setState(true)
        status.text = "Secure connection"
        info.text = "Connected  •  0:00"
        handler.removeCallbacks(timer)
        handler.post(timer)
    }

    private fun disconnectVpn() {
        stopService(Intent(this, TunnelVpnService::class.java))
        resetDisconnected("Disconnected")
    }

    private fun resetDisconnected(message: String) {
        connected = false
        connecting = false
        handler.removeCallbacks(timer)
        connectButtonState.setState(false, false)
        status.text = "Secure connection"
        info.text = message
    }

    override fun onDestroy() {
        handler.removeCallbacks(timer)
        super.onDestroy()
    }
}
