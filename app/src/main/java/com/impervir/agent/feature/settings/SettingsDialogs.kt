package com.impervir.agent.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.NumberStyle

/**
 * Material 3 time picker in a dialog (12-hour clock with AM/PM), with the standard
 * dial / keyboard-entry toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerDialog(title: String, initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val picker = rememberTimePickerState(initialHour = initialMinutes / 60, initialMinute = initialMinutes % 60, is24Hour = false)
    var typing by rememberSaveable { mutableStateOf(false) }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = ImpervirTheme.extras.muted,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp).semantics { heading() },
                )
                if (typing) TimeInput(state = picker) else TimePicker(state = picker)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { typing = !typing }) {
                        Icon(
                            if (typing) Icons.Rounded.Schedule else Icons.Rounded.Keyboard,
                            contentDescription = if (typing) "Switch to clock" else "Switch to typing the time",
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onConfirm(picker.hour * 60 + picker.minute) }) { Text("OK") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CheckInSheet(state: SettingsState, onPickTime: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text("Weekly check-in", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(
                "The agent asks you two minutes of questions on this day.",
                style = MaterialTheme.typography.bodyMedium,
                color = ImpervirTheme.extras.muted,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            Text("Day", style = MaterialTheme.typography.labelLarge)
            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayShort.forEachIndexed { i, d ->
                    FilterChip(
                        selected = state.checkInDay == i,
                        onClick = { state.checkInDay = i },
                        label = { Text(d) },
                    )
                }
            }
            Text("Time", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
            FilledTonalButton(onClick = onPickTime, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(formatTime(state.checkInMinutes), style = NumberStyle.copy(fontSize = 16.sp))
            }
            Text(
                "Every ${DayFull[state.checkInDay]} at ${formatTime(state.checkInMinutes)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = ImpervirTheme.extras.muted,
                modifier = Modifier.padding(top = 16.dp),
            )
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("Done") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationsSheet(state: SettingsState, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text("Notifications", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(
                "Pick what your agent tells you about.",
                style = MaterialTheme.typography.bodyMedium,
                color = ImpervirTheme.extras.muted,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            NotifPref.entries.forEach { pref ->
                val on = pref in state.notifications
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .toggleable(value = on, role = Role.Switch) { now ->
                            if (now) state.notifications.add(pref) else state.notifications.remove(pref)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(pref.label, style = MaterialTheme.typography.titleSmall)
                        Text(pref.sub, style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
                    }
                    Switch(checked = on, onCheckedChange = null)
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Done") }
        }
    }
}
