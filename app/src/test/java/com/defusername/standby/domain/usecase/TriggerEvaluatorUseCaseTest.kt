package com.defusername.standby.domain.usecase

import com.defusername.standby.domain.model.AppSettings
import com.defusername.standby.domain.model.PowerState
import com.defusername.standby.domain.model.TriggerMode
import com.defusername.standby.domain.repository.PowerStateRepository
import com.defusername.standby.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TriggerEvaluatorUseCaseTest {

    private lateinit var scope: CoroutineScope
    private lateinit var powerStateFlow: MutableStateFlow<PowerState>
    private lateinit var settingsFlow: MutableStateFlow<AppSettings>
    private lateinit var useCase: TriggerEvaluatorUseCase

    private val evaluateMethod = TriggerEvaluatorUseCase::class.java.getDeclaredMethod(
        "evaluate", PowerState::class.java, AppSettings::class.java, PowerState::class.java
    ).apply { isAccessible = true }

    @Before
    fun setup() {
        scope = CoroutineScope(SupervisorJob())
        powerStateFlow = MutableStateFlow(PowerState())
        settingsFlow = MutableStateFlow(AppSettings())
        useCase = TriggerEvaluatorUseCase(
            FakePowerStateRepository(powerStateFlow),
            FakeSettingsRepository(settingsFlow),
            scope
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun evaluate(
        power: PowerState,
        settings: AppSettings,
        previous: PowerState
    ): Boolean = evaluateMethod.invoke(useCase, power, settings, previous) as Boolean

    @Test
    fun `screen off while charging in CHARGING_ONLY returns true`() {
        assertTrue(evaluate(
            PowerState(isScreenOn = false, isCharging = true),
            AppSettings(triggerMode = TriggerMode.CHARGING_ONLY),
            PowerState(isScreenOn = true, isCharging = true)
        ))
    }

    @Test
    fun `screen off while not charging in CHARGING_ONLY returns false`() {
        assertFalse(evaluate(
            PowerState(isScreenOn = false, isCharging = false),
            AppSettings(triggerMode = TriggerMode.CHARGING_ONLY),
            PowerState(isScreenOn = true, isCharging = false)
        ))
    }

    @Test
    fun `screen off in ALWAYS returns true regardless of charge`() {
        assertTrue(evaluate(
            PowerState(isScreenOn = false, isCharging = false),
            AppSettings(triggerMode = TriggerMode.ALWAYS),
            PowerState(isScreenOn = true, isCharging = false)
        ))
        assertTrue(evaluate(
            PowerState(isScreenOn = false, isCharging = true),
            AppSettings(triggerMode = TriggerMode.ALWAYS),
            PowerState(isScreenOn = true, isCharging = true)
        ))
    }

    @Test
    fun `charging starts while screen off in CHARGING_ONLY returns true`() {
        assertTrue(evaluate(
            PowerState(isScreenOn = false, isCharging = true),
            AppSettings(triggerMode = TriggerMode.CHARGING_ONLY),
            PowerState(isScreenOn = false, isCharging = false)
        ))
    }

    @Test
    fun `charging starts while screen off in ALWAYS returns true`() {
        assertTrue(evaluate(
            PowerState(isScreenOn = false, isCharging = true),
            AppSettings(triggerMode = TriggerMode.ALWAYS),
            PowerState(isScreenOn = false, isCharging = false)
        ))
    }

    @Test
    fun `charging starts while screen on returns false`() {
        assertFalse(evaluate(
            PowerState(isScreenOn = true, isCharging = true),
            AppSettings(triggerMode = TriggerMode.CHARGING_ONLY),
            PowerState(isScreenOn = true, isCharging = false)
        ))
    }

    @Test
    fun `no state change returns false`() {
        assertFalse(evaluate(
            PowerState(isScreenOn = false, isCharging = true),
            AppSettings(triggerMode = TriggerMode.CHARGING_ONLY),
            PowerState(isScreenOn = false, isCharging = true)
        ))
    }

    @Test
    fun `screen on transition returns false`() {
        assertFalse(evaluate(
            PowerState(isScreenOn = true, isCharging = false),
            AppSettings(triggerMode = TriggerMode.ALWAYS),
            PowerState(isScreenOn = false, isCharging = false)
        ))
    }
}

private class FakePowerStateRepository(
    private val flow: StateFlow<PowerState>
) : PowerStateRepository {
    override val state: StateFlow<PowerState> = flow
}

private class FakeSettingsRepository(
    private val flow: StateFlow<AppSettings>
) : SettingsRepository {
    override val settings: Flow<AppSettings> = flow
    override suspend fun update(transform: (AppSettings) -> AppSettings) {}
}
