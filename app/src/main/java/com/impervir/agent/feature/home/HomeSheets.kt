package com.impervir.agent.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.navigation.Destination
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.NumberStyle

internal data class HomeNotification(val title: String, val sub: String, val time: String, val icon: ImageVector, val target: Destination)

internal val homeNotifications = listOf(
    HomeNotification("3 drafts are ready", "Week 43 · 3 need your okay", "9:02 AM", Icons.Rounded.Edit, Destination.Posts),
    HomeNotification("Priya commented on your LinkedIn post", "Customer story · Week 42", "8:41 AM", Icons.Rounded.ChatBubbleOutline, Destination.Inbox),
    HomeNotification("Your Week 42 report is in: $566", "Revenue, orders and which posts sold", "Sun", Icons.Rounded.Insights, Destination.Report),
    HomeNotification("Telegram post went out at 6 PM", "Sunday reset tip", "Sun 6 PM", Icons.AutoMirrored.Rounded.Send, Destination.PostDetail("p42-7")),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationsSheet(onDismiss: () -> Unit, onOpen: (Destination) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text("Notifications", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 8.dp).semantics { heading() })
            homeNotifications.forEach { n ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onOpen(n.target) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) { Icon(n.icon, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(n.title, style = MaterialTheme.typography.titleSmall)
                        Text(n.sub, style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
                    }
                    Text(n.time, style = NumberStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = ImpervirTheme.extras.muted)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomizeSheet(enabled: Set<HomeWidget>, onToggle: (HomeWidget, Boolean) -> Unit, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text("Customize Home", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(
                "Home keeps the essentials. Turn on anything else you want to see.",
                style = MaterialTheme.typography.bodyMedium,
                color = ImpervirTheme.extras.muted,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            HomeWidget.entries.forEach { w ->
                val on = w in enabled
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clip(MaterialTheme.shapes.small)
                        .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle(w, it) })
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(w.title, style = MaterialTheme.typography.titleSmall)
                        Text(w.sub, style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
                    }
                    Switch(checked = on, onCheckedChange = null)
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Done") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JotSheet(onDismiss: () -> Unit, onSave: () -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Jot an idea", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(
                "A customer line, a thing you noticed. Your agent keeps it for your next check-in.",
                style = MaterialTheme.typography.bodyMedium,
                color = ImpervirTheme.extras.muted,
            )
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("Your idea") },
                placeholder = { Text("Write it the way you would say it") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onSave, enabled = draft.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }
}
