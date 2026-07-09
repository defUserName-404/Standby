package com.defusername.standby.domain.usecase

import com.defusername.standby.domain.model.AppSettings
import com.defusername.standby.domain.model.PowerState
import com.defusername.standby.domain.model.TriggerMode
import com.defusername.standby.domain.repository.PowerStateRepository
import com.defusername.standby.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggerEvaluatorUseCase @Inject constructor(
    powerStateRepo: PowerStateRepository,
    settingsRepo: SettingsRepository,
    scope: CoroutineScope
) {
    private val _launchEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val launchEvents: SharedFlow<Unit> = _launchEvents

    private val powerState = powerStateRepo.state.stateIn(
        scope, SharingStarted.Eagerly, powerStateRepo.state.value
    )
    private val settings = settingsRepo.settings.stateIn(
        scope, SharingStarted.Eagerly, AppSettings()
    )

    init {
        scope.launch {
            var previousPower = powerState.value
            combine(powerState, settings) { power, s ->
                val shouldLaunch = evaluate(power, s, previousPower)
                previousPower = power
                shouldLaunch
            }.collect { shouldLaunch ->
                if (shouldLaunch) {
                    _launchEvents.emit(Unit)
                }
            }
        }
    }

    private fun evaluate(
        power: PowerState,
        settings: AppSettings,
        previous: PowerState
    ): Boolean {
        val screenJustTurnedOff = previous.isScreenOn && !power.isScreenOn
        val chargingJustStarted = !previous.isCharging && power.isCharging

        return when {
            screenJustTurnedOff && settings.triggerMode == TriggerMode.ALWAYS -> true
            screenJustTurnedOff && settings.triggerMode == TriggerMode.CHARGING_ONLY && power.isCharging -> true
            chargingJustStarted && !power.isScreenOn && settings.triggerMode == TriggerMode.CHARGING_ONLY -> true
            chargingJustStarted && !power.isScreenOn && settings.triggerMode == TriggerMode.ALWAYS -> true
            else -> false
        }
    }
}
