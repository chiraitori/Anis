package dev.chiraitori.anis

import android.app.Application
import dev.chiraitori.anis.data.BlockListRepository
import dev.chiraitori.anis.data.FirewallRepository
import dev.chiraitori.anis.data.ProfileManager
import dev.chiraitori.anis.data.QueryLogRepository
import dev.chiraitori.anis.data.SettingsRepository
import dev.chiraitori.anis.service.TrustedNetworkManager
import dev.chiraitori.anis.vpn.VpnController

class AnisApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var blockListRepository: BlockListRepository
        private set
    lateinit var profileManager: ProfileManager
        private set
    lateinit var firewallRepository: FirewallRepository
        private set
    lateinit var queryLogRepository: QueryLogRepository
        private set
    lateinit var vpnController: VpnController
        private set
    lateinit var trustedNetworkManager: TrustedNetworkManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        settingsRepository = SettingsRepository.getInstance(this)
        blockListRepository = BlockListRepository.getInstance(this)
        profileManager = ProfileManager.getInstance(this, blockListRepository, settingsRepository)
        firewallRepository = FirewallRepository.getInstance(this)
        queryLogRepository = QueryLogRepository.instance
        vpnController = VpnController.getInstance(this)

        trustedNetworkManager = TrustedNetworkManager(this, settingsRepository)
        trustedNetworkManager.start()
    }

    companion object {
        lateinit var instance: AnisApplication
            private set
    }
}
