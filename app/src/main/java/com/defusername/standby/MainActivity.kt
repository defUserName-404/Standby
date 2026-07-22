package com.defusername.standby

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defusername.standby.presentation.onboarding.OnboardingScreen
import com.defusername.standby.presentation.onboarding.OnboardingViewModel
import com.defusername.standby.presentation.settings.SettingsScreen
import com.defusername.standby.presentation.settings.SettingsViewModel
import com.defusername.standby.service.StandByMonitorService
import com.defusername.standby.ui.theme.StandbyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startMonitorService()

        setContent {
            StandbyTheme {
                val onboardingCompleted by onboardingViewModel.onboardingCompleted
                    .collectAsStateWithLifecycle()
                when (onboardingCompleted) {
                    null -> { /* settings still loading */ }
                    false -> OnboardingScreen(viewModel = onboardingViewModel)
                    true -> SettingsScreen(viewModel = settingsViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check grants after returning from any system settings screen (requirement 11.3).
        onboardingViewModel.refreshPermissions()
        settingsViewModel.refreshPermissions()
    }

    private fun startMonitorService() {
        val intent = Intent(this, StandByMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
