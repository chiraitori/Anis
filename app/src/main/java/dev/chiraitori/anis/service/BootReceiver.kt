package dev.chiraitori.anis.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.chiraitori.anis.data.SettingsRepository
import dev.chiraitori.anis.vpn.VpnController

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val settingsRepo = SettingsRepository.getInstance(context)
            val frequency = settingsRepo.autoUpdateFrequencyFlow.value
            val wifiOnly = settingsRepo.autoUpdateWifiOnlyFlow.value
            BlockListUpdateScheduler.schedule(context, frequency, wifiOnly)

            if (settingsRepo.startOnBoot && settingsRepo.isOnboardingCompleted) {
                val vpnController = VpnController.getInstance(context)
                if (vpnController.isVpnPrepared()) {
                    vpnController.startProtection()
                }
            }
        }
    }
}
