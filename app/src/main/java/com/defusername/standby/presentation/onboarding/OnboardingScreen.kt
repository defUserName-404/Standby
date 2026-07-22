package com.defusername.standby.presentation.onboarding

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defusername.standby.domain.model.PermissionState
import com.defusername.standby.domain.model.PermissionStatus
import com.defusername.standby.domain.model.PermissionType

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val step by viewModel.currentStep.collectAsStateWithLifecycle()
    val permissions by viewModel.permissionStates.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val postNotificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(onNext = viewModel::advance)
                OnboardingStep.POST_NOTIFICATIONS -> PostNotificationsStep(
                    status = permissions.statusOf(PermissionType.POST_NOTIFICATIONS),
                    onRequest = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onNext = viewModel::advance
                )
                OnboardingStep.BATTERY_OPTIMIZATION -> BatteryStep(
                    status = permissions.statusOf(PermissionType.BATTERY_OPTIMIZATION_EXEMPTION),
                    onRequest = { requestBatteryExemption(context) },
                    onNext = viewModel::advance
                )
                OnboardingStep.NOTIFICATION_ACCESS -> NotificationAccessStep(
                    status = permissions.statusOf(PermissionType.NOTIFICATION_LISTENER),
                    onOpenSettings = { openSettingsSafely(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    onNext = viewModel::advance
                )
                OnboardingStep.OEM_GUIDANCE -> OemGuidanceStep(
                    onOpenAppSettings = { openAppDetailsSettings(context) },
                    onNext = viewModel::advance
                )
                OnboardingStep.SUMMARY -> SummaryStep(
                    permissions = permissions,
                    onFinish = viewModel::completeOnboarding
                )
            }
        }
    }
}

private fun List<PermissionState>.statusOf(type: PermissionType): PermissionStatus? =
    firstOrNull { it.type == type }?.status

@Composable
private fun StepShell(
    title: String,
    body: String,
    content: @Composable () -> Unit
) {
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(24.dp))
    content()
    Spacer(Modifier.height(32.dp))
}

@Composable
private fun StatusLine(status: PermissionStatus?, grantedText: String, pendingText: String) {
    val (text, color) = when (status) {
        PermissionStatus.GRANTED -> grantedText to MaterialTheme.colorScheme.primary
        else -> pendingText to MaterialTheme.colorScheme.error
    }
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    StepShell(
        title = "Welcome to StandBy",
        body = "StandBy shows a glanceable clock, battery and notification display over your lock screen.\n\n" +
            "To work reliably it needs a few permissions. This takes about a minute — we'll explain each one."
    ) {}
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text("Get started")
    }
}

@Composable
private fun PostNotificationsStep(
    status: PermissionStatus?,
    onRequest: () -> Unit,
    onNext: () -> Unit
) {
    StepShell(
        title = "Notifications",
        body = "Android requires a visible status notification while StandBy monitors your screen and charger " +
            "in the background. That's all we use notifications for — you won't get alerts from us."
    ) {
        StatusLine(status, "Granted", "Not granted yet")
        if (status != PermissionStatus.GRANTED) {
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text("Allow notifications")
            }
            Spacer(Modifier.height(8.dp))
        }
        if (status == PermissionStatus.GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Next") }
        } else {
            TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("Continue anyway (status notification will be hidden)")
            }
        }
    }
}

@Composable
private fun BatteryStep(
    status: PermissionStatus?,
    onRequest: () -> Unit,
    onNext: () -> Unit
) {
    StepShell(
        title = "Battery optimization",
        body = "StandBy must stay alive in the background to notice when your screen turns off or the " +
            "charger connects. Excluding it from battery optimization is the single most important step " +
            "for reliability."
    ) {
        StatusLine(status, "Excluded from battery optimization", "Still being optimized")
        if (status != PermissionStatus.GRANTED) {
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text("Exclude from battery optimization")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("Continue anyway (trigger may stop working)")
            }
        } else {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Next") }
        }
    }
}

@Composable
private fun NotificationAccessStep(
    status: PermissionStatus?,
    onOpenSettings: () -> Unit,
    onNext: () -> Unit
) {
    StepShell(
        title = "Notification access (optional)",
        body = "StandBy can show your most recent notification on the StandBy screen. " +
            "Content never leaves your device. Skip this if you only want the clock — " +
            "you can enable it later in settings."
    ) {
        StatusLine(status, "Access granted", "Not granted — notification widget will stay disabled")
        if (status != PermissionStatus.GRANTED) {
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Open notification access settings")
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(if (status == PermissionStatus.GRANTED) "Next" else "Skip")
        }
    }
}

@Composable
private fun OemGuidanceStep(onOpenAppSettings: () -> Unit, onNext: () -> Unit) {
    StepShell(
        title = "One more thing on ${OemGuidance.manufacturerDisplayName}",
        body = "${OemGuidance.manufacturerDisplayName} aggressively stops background apps. " +
            "Please check these settings manually so StandBy keeps working:"
    ) {
        OemGuidance.instructions().forEachIndexed { index, instruction ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Text("${index + 1}. ", fontWeight = FontWeight.SemiBold)
                Text(instruction)
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Open StandBy app settings")
        }
    }
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Done") }
}

@Composable
private fun SummaryStep(permissions: List<PermissionState>, onFinish: () -> Unit) {
    StepShell(
        title = "You're all set",
        body = "Lock your screen or plug in the charger to see StandBy in action."
    ) {
        SummaryRow(
            label = "Status notification",
            status = permissions.statusOf(PermissionType.POST_NOTIFICATIONS),
            degradedNote = "Monitoring runs, but you won't see its status notification."
        )
        SummaryRow(
            label = "Battery optimization exemption",
            status = permissions.statusOf(PermissionType.BATTERY_OPTIMIZATION_EXEMPTION),
            degradedNote = "The system may stop the background monitor; StandBy might not appear after long idle periods."
        )
        SummaryRow(
            label = "Notification access",
            status = permissions.statusOf(PermissionType.NOTIFICATION_LISTENER),
            degradedNote = "The notification widget stays disabled; clock and battery are unaffected."
        )
    }
    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish") }
}

@Composable
private fun SummaryRow(label: String, status: PermissionStatus?, degradedNote: String) {
    val granted = status == PermissionStatus.GRANTED
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(
                if (granted) "Granted" else "Degraded",
                color = if (granted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (!granted) {
            Text(
                degradedNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun requestBatteryExemption(context: Context) {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    try {
        context.startActivity(direct)
    } catch (_: ActivityNotFoundException) {
        openSettingsSafely(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}

private fun openAppDetailsSettings(context: Context) {
    openSettingsSafely(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    )
}

private fun openSettingsSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Nowhere further to fall back to — user navigates system settings manually.
    }
}
