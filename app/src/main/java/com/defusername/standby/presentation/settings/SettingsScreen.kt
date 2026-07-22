package com.defusername.standby.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.defusername.standby.domain.model.PermissionStatus
import com.defusername.standby.domain.model.PermissionType
import com.defusername.standby.domain.model.TriggerMode

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissions by viewModel.permissionStates.collectAsStateWithLifecycle()
    var showExcludePicker by remember { mutableStateOf(false) }

    Scaffold { padding ->
        val appSettings = settings
        if (appSettings == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("StandBy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            TriggerModeSection(appSettings.triggerMode, viewModel::setTriggerMode)

            ZenModeSection(appSettings.zenModeEnabled, viewModel::setZenMode)

            WidgetsSection(
                viewModel = viewModel,
                enabledWidgetIds = appSettings.enabledWidgetIds
            )

            ExcludeListSection(
                excludedCount = appSettings.excludedPackages.size,
                onOpenPicker = { showExcludePicker = true }
            )

            HorizontalDivider()

            PermissionStatusSection(permissions)

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showExcludePicker) {
        ExcludeListDialog(
            viewModel = viewModel,
            currentlyExcluded = settings?.excludedPackages ?: emptySet(),
            onDismiss = { showExcludePicker = false }
        )
    }
}

@Composable
private fun TriggerModeSection(current: TriggerMode, onSelect: (TriggerMode) -> Unit) {
    Section(title = "Trigger", subtitle = "When should the StandBy screen appear?") {
        TriggerMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(mode) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = mode == current, onClick = { onSelect(mode) })
                Spacer(Modifier.height(0.dp))
                Text(
                    when (mode) {
                        TriggerMode.CHARGING_ONLY -> "Only while charging"
                        TriggerMode.ALWAYS -> "Always (charging or battery)"
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ZenModeSection(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Section(title = "Zen mode", subtitle = "Hide all notifications on the StandBy screen") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (enabled) "On" else "Off")
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun WidgetsSection(viewModel: SettingsViewModel, enabledWidgetIds: List<String>) {
    val allSpecs = viewModel.widgetSpecs
    val effectiveOrder = enabledWidgetIds.ifEmpty { allSpecs.map { it.id } }

    Section(
        title = "Widgets",
        subtitle = "Enable widgets and set their display order (used as the tiebreaker within a size class)"
    ) {
        allSpecs.forEach { spec ->
            val enabled = spec.id in effectiveOrder
            val orderIndex = effectiveOrder.indexOf(spec.id)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = enabled,
                    onCheckedChange = { viewModel.setWidgetEnabled(spec.id, it) }
                )
                Text(
                    spec.displayName,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
                if (enabled) {
                    TextButton(
                        onClick = { viewModel.moveWidget(spec.id, towardFront = true) },
                        enabled = orderIndex > 0
                    ) { Text("Up") }
                    TextButton(
                        onClick = { viewModel.moveWidget(spec.id, towardFront = false) },
                        enabled = orderIndex < effectiveOrder.lastIndex
                    ) { Text("Down") }
                }
            }
        }
    }
}

@Composable
private fun ExcludeListSection(excludedCount: Int, onOpenPicker: () -> Unit) {
    Section(title = "Excluded apps", subtitle = "Notifications from these apps never appear on StandBy") {
        Text("$excludedCount app(s) excluded")
        Spacer(Modifier.height(8.dp))
        Button(onClick = onOpenPicker) { Text("Manage excluded apps") }
    }
}

@Composable
private fun PermissionStatusSection(permissions: List<com.defusername.standby.domain.model.PermissionState>) {
    Section(title = "Permissions", subtitle = "Grants StandBy relies on") {
        PermissionRow("Status notification", permissions, PermissionType.POST_NOTIFICATIONS)
        PermissionRow("Battery optimization exemption", permissions, PermissionType.BATTERY_OPTIMIZATION_EXEMPTION)
        PermissionRow("Notification access", permissions, PermissionType.NOTIFICATION_LISTENER)
    }
}

@Composable
private fun PermissionRow(
    label: String,
    permissions: List<com.defusername.standby.domain.model.PermissionState>,
    type: PermissionType
) {
    val status = permissions.firstOrNull { it.type == type }?.status
    val granted = status == PermissionStatus.GRANTED
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text(
            if (granted) "Granted" else "Degraded",
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun Section(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ExcludeListDialog(
    viewModel: SettingsViewModel,
    currentlyExcluded: Set<String>,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val allApps = remember { viewModel.loadInstalledApps() }
    val filtered = remember(query) {
        if (query.isBlank()) allApps else allApps.filter { it.label.contains(query, ignoreCase = true) }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Excluded apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        val checked = app.packageName in currentlyExcluded
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setPackageExcluded(app.packageName, !checked) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { viewModel.setPackageExcluded(app.packageName, it) }
                            )
                            Text(app.label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }
}
