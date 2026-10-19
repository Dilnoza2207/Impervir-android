package com.impervir.agent.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.Platform
import com.impervir.agent.data.SampleData
import com.impervir.agent.ui.components.AddedBlock
import com.impervir.agent.ui.components.Avatar
import com.impervir.agent.ui.components.ImpervirCard
import com.impervir.agent.ui.components.grouped
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.NumberStyle

@Composable
internal fun EditorBottomBar(whenLabel: String, isBest: Boolean, onTime: () -> Unit, onApprove: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClickLabel = "Change posting time", onClick = onTime)
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                Column(Modifier.padding(start = 8.dp)) {
                    Text(whenLabel, style = NumberStyle.copy(fontSize = 14.sp))
                    if (isBest) Text("best time", style = MaterialTheme.typography.labelSmall, color = ImpervirTheme.extras.accent)
                }
            }
            Button(onClick = onApprove, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                Text("Approve & schedule", maxLines = 1)
            }
        }
    }
}

@Composable
internal fun ClicheWarning(phrase: String, rule: String, onFix: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("Sounds like AI: '$phrase'", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(rule, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        TextButton(onClick = onFix) { Text("Fix it") }
    }
}

@Composable
internal fun PostTextField(text: String, limit: Int, onChange: (String) -> Unit) {
    val n = text.length
    val over = n > limit
    val near = n >= limit * 0.9
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = 8,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.medium,
        label = { Text("Post text") },
        isError = over,
        supportingText = {
            Text(
                "${n.grouped()} / ${limit.grouped()}",
                style = NumberStyle.copy(fontSize = 12.sp),
                color = when {
                    over -> MaterialTheme.colorScheme.error
                    near -> ImpervirTheme.extras.accent
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$n of $limit characters" },
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VoiceSourceChips(playing: Int, onToggle: (Int) -> Unit) {
    Column {
        Text("From your voice note", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorData.sources.forEachIndexed { i, (t, label) ->
                val on = playing == i
                FilterChip(
                    selected = on,
                    onClick = { onToggle(i) },
                    leadingIcon = { Icon(if (on) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t, style = NumberStyle.copy(fontSize = 13.sp))
                            Spacer(Modifier.width(6.dp))
                            Waveform(on)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.semantics { contentDescription = (if (on) "Pause " else "Play ") + "$t: $label" },
                )
            }
        }
    }
}

@Composable
private fun Waveform(active: Boolean) {
    val bars = listOf(5, 10, 7, 12, 8)
    val t = rememberInfiniteTransition(label = "wave")
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        bars.forEachIndexed { j, h ->
            val k by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(420, delayMillis = j * 80), RepeatMode.Reverse), label = "b$j")
            val scale = if (active) k else 1f
            Box(
                Modifier
                    .width(2.dp)
                    .height((h * scale).dp)
                    .clip(CircleShape)
                    .background(if (active) ImpervirTheme.extras.accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
            )
        }
    }
}

@Composable
internal fun PreviewCard(text: String, whenLabel: String, platform: Platform) {
    ImpervirCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(SampleData.userInitials, size = 40.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Row {
                    Text(SampleData.userName, style = MaterialTheme.typography.titleSmall)
                    Text(" · Notion templates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(whenLabel, style = NumberStyle.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Rounded.Public, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp).size(12.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.SpaceBetween) {
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            listOf(
                Icons.Rounded.ThumbUp to "Like",
                Icons.Rounded.ChatBubbleOutline to "Comment",
                Icons.Rounded.Repeat to "Repost",
                Icons.AutoMirrored.Rounded.Send to "Send",
            ).forEach { (icon, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = muted, modifier = Modifier.size(16.dp))
                    Text(" $label", style = MaterialTheme.typography.labelMedium, color = muted)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AskAgentBlock(
    visible: Boolean,
    text: String,
    selected: String?,
    onPick: (String) -> Unit,
    onAccept: (String, String) -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit,
) {
    AddedBlock(visible = visible, title = "Ask the agent", onRemove = onRemove) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorData.tools.forEach { tool ->
                FilterChip(selected = selected == tool.id, onClick = { onPick(tool.id) }, label = { Text(tool.label) })
            }
        }
        val tool = EditorData.tools.firstOrNull { it.id == selected }
        AnimatedVisibility(visible = tool != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            val t = tool ?: return@AnimatedVisibility
            val suggestion = t.suggestionFor(text)
            Column(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(14.dp),
            ) {
                if (suggestion == null) {
                    Text("Nothing to change in this draft.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onReject, modifier = Modifier.align(Alignment.End)) { Text("Close") }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        Text("Suggested edit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    val d = wordDiff(suggestion.first.trim(), suggestion.second.trim())
                    val accent = ImpervirTheme.extras.accent
                    val muted = MaterialTheme.colorScheme.onSurfaceVariant
                    Text(
                        buildAnnotatedString {
                            if (d.prefix.isNotEmpty()) append(d.prefix + " ")
                            if (d.removed.isNotEmpty()) withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = muted)) { append(d.removed) }
                            if (d.removed.isNotEmpty() && d.added.isNotEmpty()) append(" ")
                            if (d.added.isNotEmpty()) withStyle(SpanStyle(background = accent.copy(alpha = 0.22f))) { append(d.added) }
                            if (d.suffix.isNotEmpty()) append(" " + d.suffix)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(Modifier.align(Alignment.End).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onReject) { Text("Reject") }
                        Button(onClick = { onAccept(suggestion.first, suggestion.second) }) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Accept", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExtraField(
    visible: Boolean,
    title: String,
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    note: String?,
    onRemove: () -> Unit,
    singleLine: Boolean = true,
) {
    AddedBlock(visible = visible, title = title, onRemove = onRemove) {
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            placeholder = { Text(placeholder) },
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        )
        if (note != null) {
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** M3 time picker in a dialog, with the platform's best slots as one-tap chips. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TimePickDialog(platform: Platform, day: String, time: String, onDismiss: () -> Unit, onPick: (day: String, time: String) -> Unit) {
    val rules = EditorData.rules.getValue(platform)
    val m = time.toMinutes()
    val state = rememberTimePickerState(initialHour = m / 60, initialMinute = m % 60, is24Hour = false)
    var keyboard by rememberSaveable { mutableStateOf(false) }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                Text("Pick a time", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(rules.bestNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rules.options.forEach { opt ->
                        val best = opt == rules.best
                        FilterChip(
                            selected = opt == "$day $time",
                            onClick = { onPick(opt.substringBefore(" "), opt.substringAfter(" ")) },
                            label = { Text(opt, style = NumberStyle.copy(fontSize = 13.sp)) },
                            leadingIcon = if (best) {
                                { Icon(Icons.Rounded.Star, contentDescription = "Best time", tint = ImpervirTheme.extras.accent, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Or a custom time on $day", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (keyboard) TimeInput(state) else TimePicker(state)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { keyboard = !keyboard }) {
                        Icon(
                            if (keyboard) Icons.Rounded.Schedule else Icons.Rounded.Keyboard,
                            contentDescription = if (keyboard) "Switch to clock" else "Switch to typing",
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    OutlinedButton(onClick = { onPick(day, formatTime(state.hour, state.minute)) }, modifier = Modifier.padding(start = 8.dp)) { Text("Save") }
                }
            }
        }
    }
}
