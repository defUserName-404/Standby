package com.defusername.standby.data.notification

import com.defusername.standby.domain.model.PermissionState
import com.defusername.standby.domain.model.PermissionStatus
import com.defusername.standby.domain.model.PermissionType
import com.defusername.standby.domain.repository.PermissionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenerHealthMonitorTest {

    private val permissionStates = MutableStateFlow(emptyList<PermissionState>())
    private val fakeRepo = object : PermissionRepository {
        override val states: StateFlow<List<PermissionState>> = permissionStates
        override fun refresh() {}
    }
    private val monitor = ListenerHealthMonitor(fakeRepo)

    @Test
    fun `never granted does not prompt`() {
        setListener(false)
        monitor.check()
        assertFalse(monitor.listenerRevoked.value)
    }

    @Test
    fun `granted does not prompt`() {
        setListener(true)
        monitor.check()
        assertFalse(monitor.listenerRevoked.value)
    }

    @Test
    fun `silent revocation after being granted prompts`() {
        setListener(true)
        monitor.check()
        assertFalse(monitor.listenerRevoked.value)

        setListener(false)
        monitor.check()
        assertTrue(monitor.listenerRevoked.value)
    }

    @Test
    fun `re-granting clears the prompt`() {
        setListener(true); monitor.check()
        setListener(false); monitor.check()
        assertTrue(monitor.listenerRevoked.value)

        setListener(true); monitor.check()
        assertFalse(monitor.listenerRevoked.value)
    }

    @Test
    fun `not granted then granted then revoked still prompts`() {
        setListener(false); monitor.check()
        setListener(true); monitor.check()
        setListener(false); monitor.check()
        assertTrue(monitor.listenerRevoked.value)
    }

    private fun setListener(granted: Boolean) {
        permissionStates.value = listOf(
            PermissionState(
                PermissionType.NOTIFICATION_LISTENER,
                if (granted) PermissionStatus.GRANTED else PermissionStatus.SPECIAL_ACCESS_REQUIRED
            )
        )
    }
}
