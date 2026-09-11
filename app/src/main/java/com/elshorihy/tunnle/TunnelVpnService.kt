package com.elshorihy.tunnle

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class TunnelVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (vpnInterface == null) {
            vpnInterface = Builder()
                .setSession("Elshori7y Tunnle")
                .addAddress("10.8.0.2", 32)
                .addRoute("10.8.0.0", 24)
                .setBlocking(true)
                .establish()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }
}
