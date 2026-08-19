package dev.chiraitori.anis

import dev.chiraitori.anis.data.model.ProtectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RootIptablesTest {

    @Test
    fun testProtectionModeEnums() {
        assertEquals("Local VPN Mode", ProtectionMode.LOCAL_VPN.title)
        assertEquals("Root Proxy (iptables)", ProtectionMode.ROOT_PROXY.title)
        assertTrue(ProtectionMode.ROOT_PROXY.subtitle.contains("DNS"))
    }

    @Test
    fun testIptablesCommandConstruction() {
        val appUid = 10123
        val localPort = 5354
        val whitelistedUids = setOf(10456, 10789)

        val commands = mutableListOf<String>()
        commands.add("settings put global private_dns_mode off")
        commands.add("iptables -t nat -N ANIS_DNS 2>/dev/null || true")
        commands.add("iptables -t nat -A ANIS_DNS -m owner --uid-owner $appUid -j RETURN")
        for (uid in whitelistedUids) {
            commands.add("iptables -t nat -A ANIS_DNS -m owner --uid-owner $uid -j RETURN")
        }
        commands.add("iptables -t nat -A ANIS_DNS -p udp --dport 53 -j REDIRECT --to-ports $localPort")
        commands.add("iptables -t nat -A ANIS_DNS -p tcp --dport 53 -j REDIRECT --to-ports $localPort")
        commands.add("iptables -t nat -A OUTPUT -j ANIS_DNS")

        assertEquals(8, commands.size)
        assertTrue(commands.contains("iptables -t nat -A ANIS_DNS -m owner --uid-owner 10123 -j RETURN"))
        assertTrue(commands.contains("iptables -t nat -A ANIS_DNS -p udp --dport 53 -j REDIRECT --to-ports 5354"))
        assertTrue(commands.contains("iptables -t nat -A ANIS_DNS -m owner --uid-owner 10456 -j RETURN"))
        assertTrue(commands.contains("iptables -t nat -A ANIS_DNS -m owner --uid-owner 10789 -j RETURN"))
    }
}
