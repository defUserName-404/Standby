package com.defusername.standby.domain.repository

import com.defusername.standby.domain.model.PermissionState
import kotlinx.coroutines.flow.StateFlow

interface PermissionRepository {

    /** Latest known state of every permission/special-access grant the app needs. */
    val states: StateFlow<List<PermissionState>>

    /** Re-check live system state and re-emit. Call on resume after returning from system settings. */
    fun refresh()
}
