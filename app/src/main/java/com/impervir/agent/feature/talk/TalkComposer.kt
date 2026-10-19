package com.impervir.agent.feature.talk

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.ui.components.AddPill
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

private val Glass = Color.White.copy(alpha = 0.07f)
private val GlassBorder = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
private const val BARS = 48

/** What the inline recorder hands back when the person sends. */
class Recording(val words: Int, val levels: List<Float>, val seconds: Int)

/** "Replying to Agent" / "Editing your message" strip above the composer. */
@Composable
fun ComposeBarStrip(bar: ComposeBar, onClose: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = Glass, border = GlassBorder) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(bar.label, style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
                Text(bar.excerpt, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Cancel ${bar.label.lowercase()}") }
        }
    }
}

/** Optional answer ideas for the open question. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnswerIdeas(ideas: List<String>, onPick: (String) -> Unit, onSkip: () -> Unit, onHide: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Tap one to answer", style = MaterialTheme.typography.labelMedium, color = ImpervirTheme.extras.muted, modifier = Modifier.weight(1f))
            IconButton(onClick = onHide) { Icon(Icons.Rounded.Close, contentDescription = "Hide answer ideas", modifier = Modifier.size(18.dp)) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ideas.forEach { idea ->
                Surface(onClick = { onPick(idea) }, shape = RoundedCornerShape(50), color = Glass, border = GlassBorder) {
                    Text(idea, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                }
            }
            OutlinedButton(onClick = onSkip, shape = RoundedCornerShape(50), border = GlassBorder) {
                Text("Skip this one", style = MaterialTheme.typography.labelMedium, color = ImpervirTheme.extras.muted)
            }
        }
    }
}

@Composable
fun AnswerIdeasPill(onAdd: () -> Unit) = AddPill("Answer ideas", onClick = onAdd)

/** Attach "+", the text field, and the mic that morphs into send once there's text. */
@Composable
fun TextComposer(
    draft: String,
    onDraft: (String) -> Unit,
    onAttach: () -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit,
    enabled: Boolean,
) {
    val hasText = draft.isNotBlank()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledIconButton(
            onClick = onAttach,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Glass, contentColor = MaterialTheme.colorScheme.onSurface),
        ) { Icon(Icons.Rounded.Add, contentDescription = "Add to this check-in") }
        Surface(shape = RoundedCornerShape(26.dp), color = Glass, border = GlassBorder, modifier = Modifier.weight(1f)) {
            Row(Modifier.heightIn(min = 52.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
                    if (draft.isEmpty()) Text("Type, or tap the mic to talk", style = MaterialTheme.typography.bodyLarge, color = ImpervirTheme.extras.muted)
                    BasicTextField(
                        value = draft,
                        onValueChange = onDraft,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(ImpervirTheme.extras.accent),
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { if (hasText && enabled) onSend() }),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Message your agent" },
                    )
                }
                AnimatedContent(
                    targetState = hasText,
                    transitionSpec = { (scaleIn(Motion.pop(), 0.6f) + fadeIn(tween(120))) togetherWith (scaleOut(tween(120), 0.6f) + fadeOut(tween(120))) },
                    label = "micSend",
                ) { text ->
                    if (text) {
                        FilledIconButton(
                            onClick = onSend, enabled = enabled, modifier = Modifier.size(44.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.surface),
                        ) { Icon(Icons.Rounded.ArrowUpward, contentDescription = "Send") }
                    } else {
                        FilledIconButton(
                            onClick = onMic, modifier = Modifier.size(44.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = ImpervirTheme.extras.accent, contentColor = MaterialTheme.colorScheme.onPrimary),
                        ) { Icon(Icons.Rounded.Mic, contentDescription = "Record a voice answer") }
                    }
                }
            }
        }
    }
}

/**
 * Inline recorder: live transcript, scrolling level waveform, timer. Slide left (or tap the bin) to cancel,
 * tap send to stop and send, or jump to full-screen voice mode.
 */
@Composable
fun InlineRecorder(
    script: ScriptedQuestion,
    onCancel: () -> Unit,
    onSend: (Recording) -> Unit,
    onVoiceMode: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val accent = ImpervirTheme.extras.accent
    val words = remember(script) { script.spoken.split(" ") }
    val hlRange = remember(script) {
        val start = script.spoken.indexOf(script.highlight)
        if (start < 0) IntRange.EMPTY else {
            val first = script.spoken.substring(0, start).split(" ").size - 1
            first until first + script.highlight.split(" ").size
        }
    }
    var tick by remember { mutableIntStateOf(0) }
    val levels = remember { mutableStateListOf<Float>().apply { repeat(BARS) { add(0.05f) } } }
    val hot = remember { mutableStateListOf<Boolean>().apply { repeat(BARS) { add(false) } } }
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            tick++
            val n = minOf(tick / 3, words.size)
            val talking = tick / 3 <= words.size
            levels.removeAt(0); hot.removeAt(0)
            levels.add(if (talking) 0.25f + Random.nextFloat() * 0.75f else 0.04f + Random.nextFloat() * 0.06f)
            hot.add(talking && (n - 1) in hlRange)
        }
    }
    val n = minOf(tick / 3, words.size)
    val secs = tick / 10
    val time = "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}"

    // Slide-to-cancel
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    val cancelPx = with(LocalDensity.current) { 110.dp.toPx() }
    var armed by remember { mutableFloatStateOf(0f) }

    val pulse = rememberInfiniteTransition(label = "rec")
    val dot by pulse.animateFloat(1f, 0.3f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dot")
    val caret by pulse.animateFloat(1f, 0f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "caret")

    Surface(shape = RoundedCornerShape(30.dp), color = Glass, border = GlassBorder) {
        Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Live transcript
            Box(Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.BottomStart) {
                val transcript = buildAnnotatedString {
                    words.take(n).forEachIndexed { i, w ->
                        if (i in hlRange) withStyle(SpanStyle(color = accent)) { append(w) } else append(w)
                        append(" ")
                    }
                    withStyle(SpanStyle(color = accent.copy(alpha = caret))) { append("▍") }
                }
                Text(
                    if (n == 0) buildAnnotatedString { withStyle(SpanStyle(color = ImpervirTheme.extras.muted)) { append("Listening… ") }; append(transcript) } else transcript,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            // Level waveform
            val barColor = Color(0xFFEDEAF0).copy(alpha = 0.22f)
            Canvas(
                Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 4.dp)
                    .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 6.dp)
                    .semantics { contentDescription = "Voice level" },
            ) {
                val gap = 3.dp.toPx()
                val w = (size.width - gap * (BARS - 1)) / BARS
                levels.forEachIndexed { i, l ->
                    val h = (3 + l * 30).dp.toPx().coerceAtMost(size.height)
                    drawRoundRect(
                        color = if (hot[i]) accent else barColor,
                        topLeft = Offset(i * (w + gap), size.height - h),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(w / 2, w / 2),
                    )
                }
            }
            // Controls
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onCancel() },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Glass),
                ) { Icon(Icons.Rounded.Delete, contentDescription = "Discard recording") }
                Row(
                    Modifier.weight(1f)
                        .offset { IntOffset(drag.value.roundToInt(), 0) }
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { d ->
                                scope.launch { drag.snapTo((drag.value + d).coerceIn(-cancelPx * 1.3f, 0f)) }
                                val now = if (drag.value < -cancelPx) 1f else 0f
                                if (now != armed) { armed = now; if (now == 1f) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                            },
                            onDragStopped = {
                                if (drag.value < -cancelPx) onCancel() else drag.animateTo(0f, Motion.pop())
                            },
                        )
                        .semantics { contentDescription = "Recording, $time. Slide left to cancel" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(10.dp).graphicsLayer { alpha = dot }.background(Color(0xFFFF4D4D), CircleShape))
                    Text(time, style = NumberStyle.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = ImpervirTheme.extras.muted, modifier = Modifier.size(14.dp))
                    Text(
                        if (armed == 1f) "Release to cancel" else "Slide to cancel",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (armed == 1f) MaterialTheme.colorScheme.error else ImpervirTheme.extras.muted,
                        maxLines = 1,
                    )
                }
                IconButton(onClick = onVoiceMode, colors = IconButtonDefaults.iconButtonColors(containerColor = Glass)) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = "Open full-screen voice mode")
                }
                FilledIconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSend(Recording(maxOf(n, 10), levels.toList(), secs))
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.surface),
                ) { Icon(Icons.Rounded.ArrowUpward, contentDescription = "Stop and send voice answer") }
            }
        }
    }
}

/** Composer area: reply/edit bar, answer ideas, and either the text composer or the inline recorder. */
@Composable
fun ComposerArea(
    state: TalkState,
    draft: String,
    onDraft: (String) -> Unit,
    bar: ComposeBar?,
    onCloseBar: () -> Unit,
    recording: Boolean,
    ideasOn: Boolean,
    onIdeas: (Boolean) -> Unit,
    onAttach: () -> Unit,
    onSend: () -> Unit,
    onPickIdea: (String) -> Unit,
    onSkip: () -> Unit,
    onStartRec: () -> Unit,
    onCancelRec: () -> Unit,
    onSendRec: (Recording) -> Unit,
    onVoiceMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedVisibility(bar != null && !recording, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            bar?.let { ComposeBarStrip(it, onCloseBar) }
        }
        val questionOpen = state.questionOpen && !recording && bar == null
        AnimatedVisibility(questionOpen && ideasOn, enter = expandVertically(tween(280)) + fadeIn(tween(280)), exit = shrinkVertically(tween(220)) + fadeOut(tween(160))) {
            AnswerIdeas(state.currentScript.ideas, onPickIdea, onSkip) { onIdeas(false) }
        }
        AnimatedVisibility(questionOpen && !ideasOn, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.padding(start = 2.dp)) { AnswerIdeasPill { onIdeas(true) } }
        }
        AnimatedContent(
            targetState = recording,
            transitionSpec = { (fadeIn(tween(220)) + scaleIn(Motion.soft(), 0.96f)) togetherWith fadeOut(tween(150)) },
            label = "composer",
        ) { rec ->
            if (rec) InlineRecorder(state.currentScript, onCancelRec, onSendRec, onVoiceMode)
            else TextComposer(draft, onDraft, onAttach, onSend, onStartRec, enabled = !state.typing)
        }
    }
}
