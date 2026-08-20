package dev.chiraitori.anis.service

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.chiraitori.anis.MainActivity
import dev.chiraitori.anis.vpn.VpnController
import dev.chiraitori.anis.vpn.VpnState

class AdBlockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val isRunning = VpnState.isRunningFlow.value
        val controller = VpnController.getInstance(this)

        if (isRunning) {
            controller.stopProtection()
        } else {
            if (controller.isVpnPrepared()) {
                controller.startProtection()
            } else {
                // Launch main activity to prompt for VPN permission
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        this, 0, intent,
                        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    startActivityAndCollapse(pendingIntent)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(intent)
                }
                return
            }
        }

        updateTileState()
    }

    private fun updateTileState() {
        qsTile?.let { tile ->
            val isRunning = VpnState.isRunningFlow.value
            tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "Anis"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (isRunning) "Protected" else "Paused"
            }
            tile.updateTile()
        }
    }
}
