package com.defusername.standby.domain.repository

import com.defusername.standby.domain.model.PowerState
import kotlinx.coroutines.flow.StateFlow

interface PowerStateRepository {
    val state: StateFlow<PowerState>
}
