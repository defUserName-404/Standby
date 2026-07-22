package com.defusername.standby.data.notification

import com.defusername.standby.domain.model.PermissionStatus
import com.defusername.standby.domain.model.PermissionType
import com.defusername.standby.domain.repository.PermissionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects silent revocation of notification-listener access (requirement 4.5).
 * Framework-free on purpose: the revocation state-machine is unit-testable, and the
 * user-facing re-enable prompt is rendered by [com.defusername.standby.service.StandByMonitorService],
 * which observes [listenerRevoked].
 *
 * Cadence per design.md §4: checked on service start and on each screen-on.
 */
@Singleton
class ListenerHealthMonitor @Inject constructor(
    private val permissionRepository: PermissionRepository
) {

    private val _listenerRevoked = MutableStateFlow(false)
    val listenerRevoked: StateFlow<Boolean> = _listenerRevoked.asStateFlow()

    /** Null until the first check completes — distinguishes "never granted" from "revoked". */
    private var wasGranted: Boolean? = null

    /**
     * Re-reads the live listener state and updates [listenerRevoked]. Prompts only on a
     * true silent revocation: previously granted, now not granted.
     */
    fun check() {
        val grantedNow = permissionRepository.states.value
            .firstOrNull { it.type == PermissionType.NOTIFICATION_LISTENER }
            ?.status == PermissionStatus.GRANTED

        when {
            wasGranted == true && !grantedNow -> _listenerRevoked.value = true
            grantedNow -> _listenerRevoked.value = false
            // wasGranted null or false && !grantedNow → never granted, no revocation prompt.
        }
        wasGranted = grantedNow
    }
}
