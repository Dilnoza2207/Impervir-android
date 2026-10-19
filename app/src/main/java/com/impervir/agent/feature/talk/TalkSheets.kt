package com.impervir.agent.feature.talk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.impervir.agent.data.SampleData
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.NumberStyle

@Composable
private fun SheetTitle(title: String, sub: String? = null) {
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = ImpervirTheme.extras.muted, modifier = Modifier.padding(top = 4.dp))
    }
}

private val rowColors @Composable get() = ListItemDefaults.colors(containerColor = Color.Transparent)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachSheet(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        SheetTitle("Add to this check-in", "Anything you add becomes a source for this week's posts.")
        val rows: List<Triple<ImageVector, String, String>> = listOf(
            Triple(Icons.Rounded.Image, "Photo of a customer review", "A screenshot or photo from your inbox"),
            Triple(Icons.Rounded.Link, "Paste a link", "A post, article or thread"),
            Triple(Icons.Rounded.ShoppingBag, "Product page", SampleData.products.joinToString(" or ") { it.first }),
            Triple(Icons.Rounded.History, "Reuse a past story", "From stories you saved before"),
        )
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            rows.forEach { (icon, t, s) ->
                ListItem(
                    headlineContent = { Text(t, style = MaterialTheme.typography.titleSmall) },
                    supportingContent = { Text(s, color = ImpervirTheme.extras.muted) },
                    leadingContent = { Icon(icon, contentDescription = null, tint = ImpervirTheme.extras.accent) },
                    colors = rowColors,
                    modifier = Modifier.clickableRow { onAdd(t) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInSettingsSheet(
    state: TalkState,
    onDismiss: () -> Unit,
    onFinish: () -> Unit,
    onRestart: () -> Unit,
) {
    var qOpen by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        SheetTitle("Check-in settings")
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp).animateContentSize()) {
            ListItem(
                headlineContent = { Text("Answer by voice by default", style = MaterialTheme.typography.titleSmall) },
                supportingContent = { Text("Your agent opens the recorder for each question.", color = ImpervirTheme.extras.muted) },
                trailingContent = { Switch(checked = state.voiceByDefault, onCheckedChange = null) },
                colors = rowColors,
                modifier = Modifier.clickableRow(role = androidx.compose.ui.semantics.Role.Switch) { state.voiceByDefault = !state.voiceByDefault },
            )
            val rot by animateFloatAsState(if (qOpen) 180f else 0f, label = "chev")
            ListItem(
                headlineContent = { Text("Edit this week's questions", style = MaterialTheme.typography.titleSmall) },
                supportingContent = { Text("Three questions, in your own words", color = ImpervirTheme.extras.muted) },
                trailingContent = { Icon(Icons.Rounded.ExpandMore, contentDescription = if (qOpen) "Collapse" else "Expand", modifier = Modifier.rotate(rot)) },
                colors = rowColors,
                modifier = Modifier.clickableRow { qOpen = !qOpen },
            )
            AnimatedVisibility(qOpen) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.questions.forEachIndexed { i, q ->
                        OutlinedTextField(
                            value = q,
                            onValueChange = { state.questions[i] = it },
                            label = { Text("Question ${i + 1}", style = NumberStyle) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            ListItem(
                headlineContent = { Text("That's my week", style = MaterialTheme.typography.titleSmall) },
                supportingContent = { Text("Stop here and write posts from what you said", color = ImpervirTheme.extras.muted) },
                trailingContent = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null) },
                colors = rowColors,
                modifier = Modifier.clickableRow(onClick = onFinish),
            )
            ListItem(
                headlineContent = { Text("Restart check-in", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text("Clears this conversation and asks again", color = ImpervirTheme.extras.muted) },
                colors = rowColors,
                modifier = Modifier.clickableRow(onClick = onRestart),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorySheet(storyIndex: Int, tag: String, onDismiss: () -> Unit, onEdit: () -> Unit, onRemove: () -> Unit) {
    val story = SampleData.stories[storyIndex.coerceIn(0, SampleData.stories.lastIndex)]
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    tag,
                    style = MaterialTheme.typography.labelMedium,
                    color = ImpervirTheme.extras.accent,
                    modifier = Modifier.background(ImpervirTheme.extras.accent.copy(alpha = 0.14f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Row {
                    Text("from your voice note ", style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
                    Text(story.timestamp, style = NumberStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize), color = ImpervirTheme.extras.muted)
                }
            }
            Text(story.text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 14.dp).semantics { heading() })
            Row(Modifier.padding(top = 4.dp)) {
                Text("Used in ", style = MaterialTheme.typography.bodyMedium, color = ImpervirTheme.extras.muted)
                Text("2", style = NumberStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize), color = ImpervirTheme.extras.muted)
                Text(" drafts", style = MaterialTheme.typography.bodyMedium, color = ImpervirTheme.extras.muted)
            }
            Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onEdit, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ImpervirTheme.extras.accent),
                ) { Text("Edit story") }
                OutlinedButton(onClick = onRemove, modifier = Modifier.weight(1f)) { Text("Remove") }
            }
        }
    }
}

/** Full-width ripple row with a 48dp+ target. */
private fun Modifier.clickableRow(role: androidx.compose.ui.semantics.Role = androidx.compose.ui.semantics.Role.Button, onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(role = role, onClick = onClick))
