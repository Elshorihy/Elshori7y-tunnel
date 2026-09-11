package com.elshorihy.tunnle

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var connectButton: Button
    private var connected = false
    private val vpnRequest = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        connectButton = findViewById(R.id.connectButton)

        connectButton.setOnClickListener {
            if (connected) disconnectVpn() else requestVpnPermission()
        }

        findViewById<Button>(R.id.shareButton).setOnClickListener {
            status.text = if (connected) "VPN Sharing: ready" else "Connect VPN first"
        }

        findViewById<Button>(R.id.profilesButton).setOnClickListener {
            findViewById<TextView>(R.id.info).text = "Profiles\n• Default\n\nConfig import is the next module."
        }

        findViewById<Button>(R.id.logsButton).setOnClickListener {
            findViewById<TextView>(R.id.info).text = "Logs\n• No connection events yet"
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) startActivityForResult(intent, vpnRequest) else startVpn()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == vpnRequest && resultCode == RESULT_OK) startVpn()
    }

    private fun startVpn() {
        startService(Intent(this, TunnelVpnService::class.java))
        connected = true
        status.text = "VPN service running"
        connectButton.text = "DISCONNECT"
    }

    private fun disconnectVpn() {
        stopService(Intent(this, TunnelVpnService::class.java))
        connected = false
        status.text = "Disconnected"
        connectButton.text = "CONNECT"
    }
}
