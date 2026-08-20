package dev.chiraitori.anis

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.chiraitori.anis.data.SettingsRepository
import dev.chiraitori.anis.data.model.ProtectionMode
import dev.chiraitori.anis.vpn.VpnController
import dev.chiraitori.anis.vpn.VpnState
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VpnToggleInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val controller = VpnController.getInstance(context)
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    @After
    fun stopProtection() {
        controller.stopProtection()
        waitUntil { !hasVpnNetwork() }
    }

    @Test
    fun localVpnCanBeTurnedOnAndOff() {
        assumeTrue("VPN consent must be granted before this device test", VpnService.prepare(context) == null)
        SettingsRepository.getInstance(context).setProtectionMode(ProtectionMode.LOCAL_VPN)

        controller.stopProtection()
        assertTrue("Existing VPN network did not stop", waitUntil { !hasVpnNetwork() })

        controller.startProtection()
        assertTrue("VPN did not enter the running state", waitUntil { VpnState.isRunningFlow.value })
        assertTrue("Android did not expose the VPN network", waitUntil { hasVpnNetwork() })

        controller.stopProtection()
        assertTrue("VPN state stayed active after stop", waitUntil { !VpnState.isRunningFlow.value })
        assertTrue("VPN network stayed active after stop", waitUntil { !hasVpnNetwork() })
        assertFalse(VpnState.isActiveOrStarting)
    }

    private fun hasVpnNetwork(): Boolean = connectivityManager.allNetworks.any { network ->
        connectivityManager.getNetworkCapabilities(network)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }

    private fun waitUntil(timeoutMs: Long = 10_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return condition()
    }
}
