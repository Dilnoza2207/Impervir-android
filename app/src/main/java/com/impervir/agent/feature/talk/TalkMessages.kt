package com.impervir.agent.feature.talk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.ui.ember.Ember
import com.impervir.agent.ui.ember.EmberMood
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.NumberStyle

enum class MsgAction { Copy, Reply, Edit, AskDifferently, ReadAloud, Play, Delete }

private val AgentShape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)
private val MineShape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp)

@Composable
private fun bubbleBorder() = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))

/** Text with the picked-up story phrase in the accent color. */
@Composable
fun highlighted(text: String, highlight: String?): AnnotatedString {
    val accent = ImpervirTheme.extras.accent
    return buildAnnotatedString {
        val h = highlight.orEmpty()
        val i = if (h.isEmpty()) -1 else text.indexOf(h, ignoreCase = true)
        if (i < 0) append(text) else {
            append(text.substring(0, i))
            withStyle(SpanStyle(color = accent)) { append(text.substring(i, i + h.length)) }
            append(text.substring(i + h.length))
        }
    }
}

/** Ember's 32dp slot next to agent bubbles (empty when the avatar sits on another bubble of the run). */
@Composable
private fun EmberSlot(mood: EmberMood?) {
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        if (mood != null) Ember(mood, size = 32.dp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AgentMessage(
    msg: TalkMsg,
    state: TalkState,
    showAvatar: Boolean,
    showTime: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onAction: (MsgAction) -> Unit,
    onWritePosts: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    // Words stream in once, the first time this message shows.
    val fresh = remember(msg.id) { msg.id !in state.revealed }
    val progress = remember(msg.id) { Animatable(if (fresh) 0f else 1f) }
    LaunchedEffect(msg.id, msg.text) {
        state.revealed.add(msg.id)
        if (progress.value < 1f) kotlinx.coroutines.delay(seedDelay(msg.id))
        if (progress.value < 1f) progress.animateTo(1f, tween((msg.text.length * 16).coerceIn(300, 1400), easing = LinearEasing))
    }
    val shown = (msg.text.length * progress.value).toInt()
    val text = buildAnnotatedString {
        append(msg.text.substring(0, shown))
        withStyle(SpanStyle(color = Color.Transparent)) { append(msg.text.substring(shown)) } // keeps the bubble's size steady
    }
    val pressScale by animateFloatAsState(if (selected) 0.98f else 1f, label = "bubble")

    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(0.94f)) {
        EmberSlot(if (showAvatar) moodFor(msg) else null)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                shape = AgentShape,
                color = Color.White.copy(alpha = if (selected) 0.12f else 0.07f),
                border = if (selected) BorderStroke(1.5.dp, Color.White.copy(alpha = 0.45f)) else bubbleBorder(),
                modifier = Modifier
                    .scale(pressScale)
                    .clip(AgentShape)
                    .combinedClickable(
                        onClickLabel = "Show message actions",
                        onLongClickLabel = "Show message actions",
                        onClick = onToggle,
                        onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); if (!selected) onToggle() },
                    )
                    .semantics { contentDescription = "Agent: ${msg.text}" },
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, lineHeight = 26.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 14.dp),
                )
            }
            AnimatedVisibility(
                visible = selected,
                enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140)),
            ) {
                AgentActionRow(onAction)
            }
            if (msg.kind == MsgKind.Final) {
                Button(
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onWritePosts() },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ImpervirTheme.extras.accent, contentColor = MaterialTheme.colorScheme.onPrimary),
                ) {
                    Text("Write my posts", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            if (showTime && msg.time.isNotEmpty()) TimeLabel(msg.time, read = false)
        }
    }
}

@Composable
private fun AgentActionRow(onAction: (MsgAction) -> Unit) {
    var more by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        ActionPill("Copy", Icons.Rounded.ContentCopy) { onAction(MsgAction.Copy) }
        ActionPill("Reply", Icons.AutoMirrored.Rounded.Reply) { onAction(MsgAction.Reply) }
        ActionPill("Edit", Icons.Rounded.Edit) { onAction(MsgAction.Edit) }
        Box {
            IconButton(onClick = { more = true }) {
                Icon(Icons.Rounded.MoreHoriz, contentDescription = "More message actions", tint = ImpervirTheme.extras.muted)
            }
            DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                DropdownMenuItem(text = { Text("Ask differently") }, leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                    onClick = { more = false; onAction(MsgAction.AskDifferently) })
                DropdownMenuItem(text = { Text("Read aloud") }, leadingIcon = { Icon(Icons.Rounded.VolumeUp, null) },
                    onClick = { more = false; onAction(MsgAction.ReadAloud) })
            }
        }
    }
}

@Composable
private fun ActionPill(label: String, icon: ImageVector, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors = ButtonDefaults.textButtonColors(containerColor = Color.White.copy(alpha = 0.07f), contentColor = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.height(36.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun TimeLabel(time: String, read: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(time, style = NumberStyle.copy(fontSize = 12.sp), color = ImpervirTheme.extras.muted)
        if (read) {
            Icon(Icons.Rounded.DoneAll, contentDescription = null, tint = ImpervirTheme.extras.accent, modifier = Modifier.size(14.dp))
            Text("Read", style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
        }
    }
}

/** The person's typed or spoken answer, on the right. Tap or long-press opens its menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyMessage(msg: TalkMsg, showRead: Boolean, onAction: (MsgAction) -> Unit) {
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }
    val voice = msg.kind == MsgKind.Voice
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Box {
            Surface(
                shape = MineShape,
                color = Color.White.copy(alpha = if (menu) 0.18f else 0.13f),
                border = bubbleBorder(),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(MineShape)
                    .combinedClickable(
                        onClickLabel = if (voice) "Voice note options" else "Message options",
                        onClick = { menu = true },
                        onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); menu = true },
                    ),
            ) {
                if (voice) VoiceNoteContent(msg) else Text(
                    highlighted(msg.text, msg.highlight),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                val items = if (voice) listOf(
                    Triple("Play", Icons.Rounded.PlayArrow, MsgAction.Play),
                    Triple("Copy transcript", Icons.Rounded.ContentCopy, MsgAction.Copy),
                    Triple("Edit transcript", Icons.Rounded.Edit, MsgAction.Edit),
                    Triple("Delete", Icons.Rounded.Delete, MsgAction.Delete),
                ) else listOf(
                    Triple("Copy", Icons.Rounded.ContentCopy, MsgAction.Copy),
                    Triple("Edit", Icons.Rounded.Edit, MsgAction.Edit),
                    Triple("Reply", Icons.AutoMirrored.Rounded.Reply, MsgAction.Reply),
                    Triple("Delete", Icons.Rounded.Delete, MsgAction.Delete),
                )
                items.forEach { (label, icon, action) ->
                    val danger = action == MsgAction.Delete
                    DropdownMenuItem(
                        text = { Text(label, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { menu = false; onAction(action) },
                    )
                }
            }
        }
        TimeLabel(msg.time, read = showRead)
    }
}

@Composable
private fun VoiceNoteContent(msg: TalkMsg) {
    val accent = ImpervirTheme.extras.accent
    Column(Modifier.width(280.dp).padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(32.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            }
            Row(Modifier.weight(1f).height(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                msg.wave.takeLast(26).forEach { l ->
                    Box(
                        Modifier.width(3.dp).height((5 + maxOf(l, 0.2f) * 17).dp)
                            .background(Color(0xFFEDEAF0).copy(alpha = 0.7f), RoundedCornerShape(3.dp)),
                    )
                }
            }
            Text(msg.duration, style = NumberStyle.copy(fontSize = 12.sp), color = ImpervirTheme.extras.muted)
        }
        Text(highlighted(msg.text, msg.highlight), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
    }
}

/** "Story saved · Customer win" chip, centered. Opens the story sheet. */
@Composable
fun SavedStoryChip(msg: TalkMsg, onOpen: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            onClick = onOpen,
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.07f),
            border = bubbleBorder(),
            modifier = Modifier.semantics { contentDescription = "Story saved, ${msg.tag}. Open story" },
        ) {
            Row(Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(20.dp).background(ImpervirTheme.extras.accent, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                }
                Text("Story saved", style = MaterialTheme.typography.bodySmall)
                Text("· ${msg.tag}", style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
            }
        }
    }
}

/** Ember thinking + three bouncing dots while the agent "types". */
@Composable
fun TypingIndicator() {
    val t = rememberInfiniteTransition(label = "typing")
    val phase by t.animateFloat(0f, 3f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "dots")
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { contentDescription = "Agent is typing"; liveRegion = LiveRegionMode.Polite },
    ) {
        EmberSlot(EmberMood.Thinking)
        Row(
            Modifier.clip(AgentShape).background(Color.White.copy(alpha = 0.07f)).border(bubbleBorder(), AgentShape)
                .padding(horizontal = 18.dp, vertical = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(3) { i ->
                val d = (phase - i).let { if (it < 0) it + 3 else it }
                val lift = if (d < 1f) kotlin.math.sin(d * Math.PI).toFloat() else 0f
                Box(
                    Modifier.size(7.dp)
                        .graphicsLayer { translationY = -lift * 4.dp.toPx(); alpha = 0.4f + 0.6f * lift }
                        .background(MaterialTheme.colorScheme.onSurface, CircleShape),
                )
            }
        }
    }
}
