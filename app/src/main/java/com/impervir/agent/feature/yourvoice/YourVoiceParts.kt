package com.impervir.agent.feature.yourvoice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.SampleData
import com.impervir.agent.data.VoiceRule
import com.impervir.agent.ui.components.AddPill
import com.impervir.agent.ui.components.AddedBlock
import com.impervir.agent.ui.components.CountUpText
import com.impervir.agent.ui.components.ImpervirCard
import com.impervir.agent.ui.components.SectionTitle
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle

// ------------------------------------------------------------------ Match ring

@Composable
internal fun MatchCard(state: YourVoiceState, modifier: Modifier = Modifier) {
    val match = SampleData.voiceMatch
    val trend = SampleData.editRateTrend
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val sweep by animateFloatAsState(if (started) match / 100f else 0f, tween(1200, 250, Motion.Emphasized), label = "ring")
    val accent = ImpervirTheme.extras.accent
    val track = MaterialTheme.colorScheme.outlineVariant
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ImpervirCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(168.dp).semantics(mergeDescendants = true) { contentDescription = "$match percent match. Sounds like you" },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
                        val stroke = 14.dp.toPx()
                        val inset = stroke / 2
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                        drawArc(accent, -90f, 360f * sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CountUpText(match, format = { "$it%" }, style = NumberStyle.copy(fontSize = 44.sp, lineHeight = 46.sp), durationMs = 1200)
                        Text("match", style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
                    }
                }
                Text("Sounds like you", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 14.dp).semantics { heading() })
                Text(
                    "You edited ${trend.last().second}% of words last week, down from ${trend.first().second}%.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ImpervirTheme.extras.muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (!state.showTrend) {
                    AddPill("Add trend chart", onClick = { state.showTrend = true }, modifier = Modifier.padding(top = 14.dp))
                }
            }
        }
        AddedBlock(visible = state.showTrend, title = "Edits per week", onRemove = { state.showTrend = false }) {
            Text(
                "Edits fell every week, from ${trend.first().second}% to ${trend.last().second}%.",
                style = MaterialTheme.typography.bodyMedium,
                color = ImpervirTheme.extras.muted,
            )
            val max = trend.maxOf { it.second }.toFloat()
            Row(
                Modifier.fillMaxWidth().height(140.dp).padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                trend.forEachIndexed { i, (week, pct) ->
                    val h by animateFloatAsState(if (state.showTrend) pct / max else 0f, tween(700, 120 + i * 70, Motion.Emphasized), label = "bar$i")
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$pct%", style = NumberStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium))
                        Box(
                            Modifier
                                .padding(vertical = 4.dp)
                                .fillMaxWidth()
                                .height((80 * h).dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (i == trend.lastIndex) accent else MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Text("W$week", style = NumberStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = ImpervirTheme.extras.muted)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Tone sliders

@Composable
internal fun ToneCard(state: YourVoiceState, onSettled: () -> Unit, modifier: Modifier = Modifier) {
    ImpervirCard(modifier.fillMaxWidth()) {
        SectionTitle("How you sound", Modifier.semantics { heading() })
        Text(
            "Drag a slider and the sample below rewrites itself.",
            style = MaterialTheme.typography.bodySmall,
            color = ImpervirTheme.extras.muted,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        traitDefs.forEach { t ->
            val v = state.traits[t.key] ?: t.default
            val leftOn = v < 50f
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    t.left,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (leftOn) MaterialTheme.colorScheme.onSurface else ImpervirTheme.extras.muted,
                    modifier = Modifier.weight(1f),
                )
                Text(t.right, style = MaterialTheme.typography.labelLarge, color = if (!leftOn) MaterialTheme.colorScheme.onSurface else ImpervirTheme.extras.muted)
            }
            Slider(
                value = v,
                onValueChange = { state.traits[t.key] = it },
                onValueChangeFinished = onSettled,
                valueRange = 0f..100f,
                modifier = Modifier.semantics { contentDescription = "${t.left} to ${t.right}" },
            )
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("A sample, in your voice", style = MaterialTheme.typography.labelMedium, color = ImpervirTheme.extras.muted)
                AnimatedContent(
                    targetState = state.sample(),
                    transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) },
                    label = "sample",
                ) { text ->
                    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Rules

@Composable
internal fun RulesCard(
    rules: List<VoiceRule>,
    state: YourVoiceState,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSaveEdit: (String, String) -> Unit,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val on = rules.count { it.enabled }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ImpervirCard(Modifier.fillMaxWidth()) {
            SectionTitle("Rules I learned", Modifier.semantics { heading() }) {
                Text("$on on, ${rules.size - on} off", style = NumberStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), color = ImpervirTheme.extras.muted)
            }
            Spacer(Modifier.height(4.dp))
            rules.forEachIndexed { i, rule ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                RuleRow(
                    rule = rule,
                    text = state.ruleEdits[rule.id] ?: rule.text,
                    source = ruleSources[rule.id] ?: "Added by you today",
                    editing = state.editingRule == rule.id,
                    onToggle = { onToggle(rule.id) },
                    onEdit = { state.editingRule = rule.id },
                    onCancelEdit = { state.editingRule = null },
                    onSave = { onSaveEdit(rule.id, it) },
                    onDelete = { onDelete(rule.id) },
                )
            }
            if (!state.showRuleBox) {
                AddPill("Add a rule", onClick = { state.showRuleBox = true }, modifier = Modifier.padding(top = 12.dp))
            }
        }
        AddedBlock(visible = state.showRuleBox, title = "New rule", onRemove = { state.showRuleBox = false }) {
            AddField(placeholder = "Like 'No exclamation marks'", label = "Add a rule") { onAdd(it) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RuleRow(
    rule: VoiceRule,
    text: String,
    source: String,
    editing: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    if (editing) {
        var draft by rememberSaveable(rule.id) { mutableStateOf(text) }
        Column(Modifier.padding(vertical = 10.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("Edit rule") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (draft.isNotBlank()) onSave(draft.trim()) }),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.align(Alignment.End).padding(top = 4.dp)) {
                TextButton(onClick = onCancelEdit) { Text("Cancel") }
                TextButton(onClick = { onSave(draft.trim()) }, enabled = draft.isNotBlank()) { Text("Save") }
            }
        }
        return
    }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clip(MaterialTheme.shapes.small)
                .combinedClickable(
                    onClickLabel = if (rule.enabled) "Turn rule off" else "Turn rule on",
                    onLongClickLabel = "More options",
                    onClick = onToggle,
                    onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); menu = true },
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (rule.enabled) MaterialTheme.colorScheme.onSurface else ImpervirTheme.extras.muted,
                )
                Text(source, style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
            }
            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.semantics { contentDescription = "Rule on or off" },
            )
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.MoreHoriz, contentDescription = "More options for this rule", tint = ImpervirTheme.extras.muted)
            }
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Edit rule") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    onClick = { menu = false; onEdit() },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

/** A single-line field with an Add button, used by the "+ Add" blocks. */
@Composable
private fun AddField(placeholder: String, label: String, onAdd: (String) -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    val submit = {
        if (draft.isNotBlank()) {
            onAdd(draft.trim())
            draft = ""
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { Text(placeholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.weight(1f).semantics { contentDescription = label },
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = submit, enabled = draft.isNotBlank()) { Text("Add") }
    }
}

// ------------------------------------------------------------------ Never say

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NeverSayCard(state: YourVoiceState, onRemove: (String) -> Unit, onAdd: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ImpervirCard(Modifier.fillMaxWidth()) {
            SectionTitle("Never say", Modifier.semantics { heading() })
            Text(
                "I keep these words out of every draft.",
                style = MaterialTheme.typography.bodySmall,
                color = ImpervirTheme.extras.muted,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.banned.forEach { phrase ->
                    InputChip(
                        selected = false,
                        onClick = { onRemove(phrase) },
                        label = { Text(phrase) },
                        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "Remove $phrase", modifier = Modifier.size(InputChipDefaults.IconSize)) },
                    )
                }
            }
            if (!state.showBanBox) {
                AddPill("Add a phrase", onClick = { state.showBanBox = true }, modifier = Modifier.padding(top = 8.dp))
            }
        }
        AddedBlock(visible = state.showBanBox, title = "New phrase to avoid", onRemove = { state.showBanBox = false }) {
            AddField(placeholder = "A phrase to avoid", label = "Add a phrase to avoid") { onAdd(it) }
        }
    }
}

// ------------------------------------------------------------------ Stories (pager)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StoriesSection(onUse: () -> Unit, modifier: Modifier = Modifier) {
    val stories = SampleData.stories
    val pager = rememberPagerState { stories.size }
    Column(modifier) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            SectionTitle("Your stories", Modifier.semantics { heading() }) {
                Text("${stories.size} saved", style = NumberStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), color = ImpervirTheme.extras.muted)
            }
            Text(
                "Each one comes from a voice note. Swipe to see more.",
                style = MaterialTheme.typography.bodySmall,
                color = ImpervirTheme.extras.muted,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )
        }
        HorizontalPager(
            state = pager,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
        ) { page ->
            val s = stories[page]
            ImpervirCard(Modifier.fillMaxWidth().heightIn(min = 220.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = ImpervirTheme.extras.accent, modifier = Modifier.size(18.dp))
                    Text(
                        "From your voice note at ${s.timestamp} · Mon Oct 20",
                        style = MaterialTheme.typography.labelMedium,
                        color = ImpervirTheme.extras.muted,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                Text(s.text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp))
                FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    storyTags[s.id].orEmpty().forEach { tag ->
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(tag, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
                FilledTonalButton(onClick = onUse, modifier = Modifier.padding(top = 14.dp)) {
                    Text("Use in a new post")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp).semantics { contentDescription = "Story ${pager.currentPage + 1} of ${stories.size}" },
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(stories.size) { i ->
                val w by animateFloatAsState(if (i == pager.currentPage) 18f else 6f, Motion.pop(), label = "dot$i")
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(w.dp)
                        .clip(CircleShape)
                        .background(if (i == pager.currentPage) ImpervirTheme.extras.accent else MaterialTheme.colorScheme.outline),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Teach me more

@Composable
internal fun TeachCard(state: YourVoiceState, onImport: () -> Unit, onLearn: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ImpervirCard(Modifier.fillMaxWidth()) {
            SectionTitle("Teach me more", Modifier.semantics { heading() })
            Text(
                "The more I read, the closer I sound.",
                style = MaterialTheme.typography.bodySmall,
                color = ImpervirTheme.extras.muted,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
            OutlinedButton(onClick = onImport, enabled = !state.importing, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Import posts from LinkedIn")
            }
            AnimatedVisibility(visible = state.importing) {
                Column(Modifier.padding(top = 12.dp)) {
                    Text("Reading your posts from LinkedIn", style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
                    LinearProgressIndicator(progress = { state.importProgress }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            }
            if (!state.pasting) {
                AddPill("Paste a post", onClick = { state.pasting = true }, modifier = Modifier.padding(top = 12.dp))
            }
        }
        AddedBlock(visible = state.pasting, title = "Paste a post you wrote", onRemove = { state.pasting = false }) {
            var draft by rememberSaveable { mutableStateOf("") }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                minLines = 4,
                placeholder = { Text("Paste it here") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val ok = draft.isNotBlank()
                    if (ok) draft = ""
                    onLearn(ok)
                },
                modifier = Modifier.padding(top = 10.dp),
            ) { Text("Learn from this post") }
        }
    }
}
