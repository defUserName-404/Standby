package com.defusername.standby.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defusername.standby.domain.model.PermissionState
import com.defusername.standby.domain.repository.PermissionRepository
import com.defusername.standby.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep {
    WELCOME,
    POST_NOTIFICATIONS,
    BATTERY_OPTIMIZATION,
    NOTIFICATION_ACCESS,
    OEM_GUIDANCE,
    SUMMARY
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissionRepository: PermissionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /** Null while settings are still loading. */
    val onboardingCompleted: StateFlow<Boolean?> = settingsRepository.settings
        .map { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val permissionStates: StateFlow<List<PermissionState>> = permissionRepository.states

    private val steps: List<OnboardingStep> = buildList {
        add(OnboardingStep.WELCOME)
        add(OnboardingStep.POST_NOTIFICATIONS)
        add(OnboardingStep.BATTERY_OPTIMIZATION)
        add(OnboardingStep.NOTIFICATION_ACCESS)
        if (OemGuidance.isAggressiveOem) add(OnboardingStep.OEM_GUIDANCE)
        add(OnboardingStep.SUMMARY)
    }

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStep: StateFlow<OnboardingStep> = _currentStepIndex
        .map { steps[it] }
        .stateIn(viewModelScope, SharingStarted.Eagerly, OnboardingStep.WELCOME)

    fun advance() {
        if (_currentStepIndex.value < steps.lastIndex) {
            _currentStepIndex.value += 1
        }
    }

    /** Re-check live grant state — called on resume after returning from system settings. */
    fun refreshPermissions() = permissionRepository.refresh()

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(onboardingCompleted = true) }
        }
    }
}
