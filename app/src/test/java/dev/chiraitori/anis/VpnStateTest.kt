package dev.chiraitori.anis

import dev.chiraitori.anis.vpn.VpnState
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnStateTest {

    @After
    fun resetState() {
        VpnState.setRunning(false)
    }

    @Test
    fun startingCountsAsActiveAndCanBeCancelled() {
        VpnState.setStarting(true)

        assertTrue(VpnState.isStartingFlow.value)
        assertTrue(VpnState.isActiveOrStarting)

        VpnState.setRunning(false)

        assertFalse(VpnState.isStartingFlow.value)
        assertFalse(VpnState.isActiveOrStarting)
    }

    @Test
    fun runningClearsStartingState() {
        VpnState.setStarting(true)
        VpnState.setRunning(true)

        assertTrue(VpnState.isRunningFlow.value)
        assertFalse(VpnState.isStartingFlow.value)
    }
}
