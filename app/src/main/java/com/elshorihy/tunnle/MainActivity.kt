package com.elshorihy.tunnle

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.TrafficStats
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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

    private val servers = mutableListOf(
        ServerProfile("auto", "Auto Select", "🌐", "", 0, "AUTO", "Choose an imported WireGuard profile"),
        ServerProfile("us", "United States", "🇺🇸", "", 51820, "WireGuard", "Import a real US WireGuard .conf"),
        ServerProfile("de", "Germany", "🇩🇪", "", 51820, "WireGuard", "Import a real Germany WireGuard .conf"),
        ServerProfile("nl", "Netherlands", "🇳🇱", "", 51820, "WireGuard", "Import a real Netherlands WireGuard .conf")
    )
    private var selectedServer = servers.first()

    private var connected = false
    private var connecting = false
    private var connectedAt = 0L
    private var lastRx = TrafficStats.getTotalRxBytes()
    private var lastTx = TrafficStats.getTotalTxBytes()
    private var lastStatsAt = System.currentTimeMillis()
    private val vpnRequest = 1001
    private val configRequest = 1002
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wireGuard: WireGuardManager

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
        wireGuard = WireGuardManager(this)

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

        connectionButton.setOnClickListener { if (connected) disconnectVpn() else requestVpnPermission() }
        shareSwitch.setOnCheckedChangeListener { button, checked ->
            if (checked) {
                button.isChecked = false
                sharingStatus.text = "VPN Sharing will be added after the tunnel core is verified"
            } else sharingStatus.text = "Share this VPN connection"
        }
        findViewById<TextView>(R.id.headerMenu).setOnClickListener { info.text = "Elshori7y Tunnle  •  WireGuard core" }
        findViewById<Button>(R.id.homeButton).setOnClickListener { showHome() }
        findViewById<Button>(R.id.profilesButton).setOnClickListener { showProfiles() }
        findViewById<Button>(R.id.serversButton).setOnClickListener { showServers() }
        findViewById<Button>(R.id.logsButton).setOnClickListener { showLogs() }
        findViewById<Button>(R.id.settingsButton).setOnClickListener { showSettings() }
    }

    private fun showHome() {
        status.text = "Secure connection"
        info.text = if (connected) "Connected" else "Ready to connect"
        serverText.text = "${selectedServer.country}  ${selectedServer.name}  •  ${selectedServer.protocol}"
        tunnelText.text = if (connected) "WIREGUARD TUNNEL ACTIVE" else "WIREGUARD TUNNEL"
    }

    private fun showProfiles() {
        status.text = "Profiles"
        AlertDialog.Builder(this)
            .setTitle("Elshori7y Profiles")
            .setItems(servers.map { profileLabel(it) }.toTypedArray()) { _, which ->
                selectedServer = servers[which]
                showHome()
            }
            .setPositiveButton("Import .conf") { _, _ -> importWireGuardConfig() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showServers() {
        status.text = "Servers"
        AlertDialog.Builder(this)
            .setTitle("Server List")
            .setItems(servers.map { profileLabel(it) }.toTypedArray()) { _, which ->
                selectedServer = servers[which]
                serverText.text = "${selectedServer.country}  ${selectedServer.name}"
                info.text = if (selectedServer.wireGuardConfig != null) "Config loaded • ready to connect" else "No WireGuard config loaded\nImport a .conf for this location"
            }
            .setPositiveButton("Import .conf") { _, _ -> importWireGuardConfig() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun profileLabel(profile: ServerProfile): String {
        val state = if (profile.wireGuardConfig != null) "READY" else "CONFIG NEEDED"
        return "${profile.country} ${profile.name}  •  ${profile.protocol}  •  $state"
    }

    private fun importWireGuardConfig() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }
        startActivityForResult(intent, configRequest)
    }

    private fun showAddConfigDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 8, 48, 0)
        }
        val name = EditText(this).apply { hint = "Profile name" }
        val host = EditText(this).apply { hint = "Host / IP" }
        val port = EditText(this).apply { hint = "Port"; inputType = 2; setText("51820") }
        val protocol = EditText(this).apply { hint = "Protocol"; setText("WireGuard") }
        box.addView(name); box.addView(host); box.addView(port); box.addView(protocol)
        AlertDialog.Builder(this)
            .setTitle("Add Server")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val profile = ServerProfile(
                    "custom-${System.currentTimeMillis()}",
                    name.text.toString().ifBlank { "Custom Server" },
                    "🌐",
                    host.text.toString().ifBlank { "Not configured" },
                    port.text.toString().toIntOrNull() ?: 51820,
                    protocol.text.toString().ifBlank { "WireGuard" },
                    "User-created profile"
                )
                servers.add(profile)
                selectedServer = profile
                showHome()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogs() {
        status.text = "Connection Logs"
        info.text = if (connected) "Activity Log\nWireGuard connected\nProfile: ${selectedServer.name}\nTunnel core is running" else "Activity Log\nNo active WireGuard connection\nLast profile: ${selectedServer.name}"
    }

    private fun showSettings() {
        status.text = "Settings"
        info.text = "WireGuard core: enabled\nConfig import: .conf\nVPN permission: Android system\n\nVPN Sharing requires a connected tunnel and will be enabled in a later core stage."
    }

    private fun requestVpnPermission() {
        if (selectedServer.wireGuardConfig.isNullOrBlank()) {
            AlertDialog.Builder(this)
                .setTitle("Config required")
                .setMessage("This server needs a real WireGuard .conf file. Import one first; the app will then connect to its actual endpoint.")
                .setPositiveButton("Import .conf") { _, _ -> importWireGuardConfig() }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        connecting = true
        connectionButton.setState(false, true)
        status.text = "Preparing WireGuard"
        val intent = VpnService.prepare(this)
        if (intent != null) startActivityForResult(intent, vpnRequest) else startWireGuard()
    }

    @Deprecated("Use Activity Result APIs in a future refactor")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == vpnRequest) {
            if (resultCode == RESULT_OK) startWireGuard() else resetDisconnected("VPN permission was cancelled")
        } else if (requestCode == configRequest && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching {
                contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: error("Unable to read config")
            }.onSuccess { raw ->
                selectedServer = selectedServer.copy(wireGuardConfig = raw)
                selectedServer = selectedServer.copy(
                    host = extractEndpointHost(raw) ?: selectedServer.host,
                    port = extractEndpointPort(raw) ?: selectedServer.port
                )
                val index = servers.indexOfFirst { it.id == selectedServer.id }
                if (index >= 0) servers[index] = selectedServer
                info.text = "Config imported successfully\n${selectedServer.name}\n${selectedServer.host}:${selectedServer.port}"
                showHome()
            }.onFailure {
                info.text = "Config import failed: ${it.message ?: "invalid file"}"
            }
        }
    }

    private fun extractEndpointHost(raw: String): String? = raw.lineSequence()
        .firstOrNull { it.trim().startsWith("Endpoint", true) }
        ?.substringAfter('=', "")?.trim()?.substringBeforeLast(':')?.trim()?.takeIf { it.isNotBlank() }

    private fun extractEndpointPort(raw: String): Int? = raw.lineSequence()
        .firstOrNull { it.trim().startsWith("Endpoint", true) }
        ?.substringAfterLast(':')?.trim()?.toIntOrNull()

    private fun startWireGuard() {
        val result = wireGuard.connect(selectedServer)
        result.onSuccess {
            connected = true
            connecting = false
            connectedAt = System.currentTimeMillis()
            lastRx = TrafficStats.getTotalRxBytes()
            lastTx = TrafficStats.getTotalTxBytes()
            lastStatsAt = connectedAt
            connectionButton.setState(true)
            status.text = "Secure connection"
            tunnelText.text = "WIREGUARD TUNNEL ACTIVE"
            info.text = "Connected  •  0:00\n${selectedServer.country} ${selectedServer.name}"
            handler.removeCallbacks(ticker)
            handler.post(ticker)
        }.onFailure {
            resetDisconnected("WireGuard connection failed\n${it.message ?: "Unknown error"}")
        }
    }

    private fun disconnectVpn() {
        val result = wireGuard.disconnect()
        result.onSuccess { resetDisconnected("Disconnected") }
            .onFailure { resetDisconnected("Disconnect failed: ${it.message ?: "Unknown error"}") }
    }

    private fun updateTrafficStats(now: Long) {
        val rx = TrafficStats.getTotalRxBytes(); val tx = TrafficStats.getTotalTxBytes()
        val elapsed = (now - lastStatsAt).coerceAtLeast(1L)
        val rxRate = ((rx - lastRx).coerceAtLeast(0L) * 1000L) / elapsed
        val txRate = ((tx - lastTx).coerceAtLeast(0L) * 1000L) / elapsed
        downloadText.text = "↓ ${formatRate(rxRate)}\nDownload"
        uploadText.text = "↑ ${formatRate(txRate)}\nUpload"
        pingText.text = "⌁ —\nPing"
        lastRx = rx; lastTx = tx; lastStatsAt = now
    }

    private fun formatRate(bytesPerSecond: Long): String = when {
        bytesPerSecond >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", bytesPerSecond / 1024.0 / 1024.0)
        bytesPerSecond >= 1024 -> String.format(Locale.US, "%.0f KB/s", bytesPerSecond / 1024.0)
        else -> "$bytesPerSecond B/s"
    }

    private fun resetDisconnected(message: String) {
        connected = false; connecting = false; handler.removeCallbacks(ticker)
        connectionButton.setState(false, false); status.text = "Secure connection"; tunnelText.text = "WIREGUARD TUNNEL"
        downloadText.text = "↓ 0 B/s\nDownload"; uploadText.text = "↑ 0 B/s\nUpload"; pingText.text = "⌁ —\nPing"; info.text = message
    }

    override fun onDestroy() { handler.removeCallbacks(ticker); super.onDestroy() }
}
