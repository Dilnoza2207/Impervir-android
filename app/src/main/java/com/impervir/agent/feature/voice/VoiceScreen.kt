package com.impervir.agent.feature.voice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.feature.voice.house.HouseCaption
import com.impervir.agent.feature.voice.house.HouseDirector
import com.impervir.agent.feature.voice.house.HouseStage
import com.impervir.agent.feature.voice.house.RoomDots
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.components.SectionTitle
import com.impervir.agent.ui.theme.ImmersiveDark
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.launch

private val Glass = Color.White.copy(alpha = 0.07f)
private val GlassLine = Color.White.copy(alpha = 0.12f)

/**
 * Voice mode: Ember works through its house while you talk. Always dark.
 * Shows the prompt, Ember's house (tap a dot to send it to a room), the live transcript, stories caught,
 * and three controls: Pause/Resume, Add to chat, and a ••• menu (Type instead, Discard recording).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(repository: ImpervirRepository, navigator: Navigator) {
    ImmersiveDark {
        val director = remember { HouseDirector() }
        val session = remember { VoiceSession() }
        var paused by rememberSaveable { mutableStateOf(false) }
        var confirmDiscard by rememberSaveable { mutableStateOf(false) }
        var renaming by rememberSaveable { mutableStateOf<String?>(null) }
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val haptics = LocalHapticFeedback.current

        SideEffect { director.paused = paused; session.paused = paused }

        // One clock drives Ember's house and the simulated transcript.
        LaunchedEffect(director, session) {
            var last = -1L
            while (true) {
                withFrameNanos { now ->
                    if (last >= 0L) {
                        val dt = (now - last) / 1_000_000_000f
                        director.advance(dt)
                        session.advance(dt)
                    }
                    last = now
                }
            }
        }

        // A light tick each time a story lands.
        val caughtCount = session.caught.size
        val seen = remember { intArrayOf(caughtCount) }
        LaunchedEffect(caughtCount) {
            if (caughtCount > seen[0]) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            seen[0] = caughtCount
        }

        fun notify(text: String) {
            scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(text, duration = SnackbarDuration.Short) }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = navigator::back) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to chat")
                        }
                    },
                    actions = { RecordingChip(paused, session, Modifier.padding(end = 12.dp)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            bottomBar = {
                VoiceControls(
                    paused = paused,
                    onTogglePause = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        paused = !paused
                    },
                    onAddToChat = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        navigator.back()
                    },
                    onTypeInstead = navigator::back,
                    onDiscard = { confirmDiscard = true },
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                PromptHeadline(session.prompt.question)
                HouseStage(director, Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp))
                HouseCaption(director, Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp))
                RoomDots(
                    current = director.targetRoom,
                    onPick = { i ->
                        if (director.goRoom(i)) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (paused) notify("Ember heads there when you resume")
                        }
                    },
                    modifier = Modifier.padding(top = 2.dp),
                )
                LiveTranscript(session, Modifier.padding(start = 22.dp, end = 22.dp, top = 8.dp))
                StoriesCaught(
                    session = session,
                    onRename = { renaming = it },
                    onHear = { notify("Playing \"${session.nameOf(it)}\"") },
                    onRemove = { tag ->
                        session.remove(tag)
                        scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val r = snackbar.showSnackbar("Story removed", actionLabel = "Undo", duration = SnackbarDuration.Short)
                            if (r == SnackbarResult.ActionPerformed) session.restore(tag)
                        }
                    },
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 16.dp),
                )
            }
        }

        if (confirmDiscard) {
            AlertDialog(
                onDismissRequest = { confirmDiscard = false },
                icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                title = { Text("Discard this recording?") },
                text = { Text("Ember forgets what you said in this session, including the stories it caught.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            confirmDiscard = false
                            navigator.back()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Discard") }
                },
                dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep talking") } },
            )
        }

        renaming?.let { tag ->
            RenameDialog(
                initial = session.nameOf(tag),
                onDismiss = { renaming = null },
                onSave = { name ->
                    renaming = null
                    if (name.isNotBlank()) {
                        session.rename(tag, name.trim())
                        notify("Renamed to ${name.trim()}")
                    }
                },
            )
        }
    }
}

@Composable
private fun PromptHeadline(question: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 26.dp)
            .semantics { heading() },
    ) {
        key(question) { BlurRiseText(question, style = MaterialTheme.typography.headlineSmall) }
    }
}

/** "● Listening 0:42" (the dot blinks while recording). */
@Composable
private fun RecordingChip(paused: Boolean, session: VoiceSession, modifier: Modifier = Modifier) {
    val seconds by remember { derivedStateOf { session.elapsed / 10 } }
    val blink = rememberInfiniteTransition(label = "rec")
    val a by blink.animateFloat(1f, .25f, infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "recDot")
    val accent = ImpervirTheme.extras.accent
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = CircleShape,
        color = Glass,
        border = BorderStroke(1.dp, GlassLine),
        modifier = modifier.clearAndSetSemantics {
            contentDescription = (if (paused) "Paused, " else "Listening, ") + "${seconds / 60} minutes ${seconds % 60} seconds"
        },
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .graphicsLayer { alpha = if (paused) 1f else a }
                    .clip(CircleShape)
                    .background(if (paused) muted else accent),
            )
            Spacer(Modifier.width(8.dp))
            Text(if (paused) "Paused" else "Listening", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(8.dp))
            Text(
                "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
                style = NumberStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = muted,
            )
        }
    }
}

/** Words arrive one by one and rise in; story phrases are in the accent colour. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveTranscript(session: VoiceSession, modifier: Modifier = Modifier) {
    val n by remember { derivedStateOf { session.wordCount } }
    val tag by remember { derivedStateOf { session.justCaught } }
    val prompt = session.prompt
    val accent = ImpervirTheme.extras.accent
    // Keeps the last caught tag so the pill keeps its text while it animates out.
    val lastTag = remember { arrayOf("") }
    tag?.let { lastTag[0] = it }
    Box(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .clearAndSetSemantics { contentDescription = "Live transcript: " + prompt.words.take(n).joinToString(" ") },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fade = 26.dp.toPx() / size.height
                    drawRect(Brush.verticalGradient(0f to Color.Transparent, fade to Color.Black), blendMode = BlendMode.DstIn)
                },
        ) {
            FlowRow(
                Modifier
                    .align(Alignment.BottomStart)
                    .wrapContentHeight(Alignment.Bottom, unbounded = true),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (i in 0 until n) {
                    key(prompt, i) { RisingWord(prompt.words[i], if (prompt.phraseOf(i) >= 0) accent else null) }
                }
            }
        }
        AnimatedVisibility(
            visible = tag != null,
            enter = fadeIn(tween(200)) + slideInVertically(Motion.pop()) { it } + scaleIn(Motion.pop(), initialScale = .9f),
            exit = fadeOut(tween(250)) + slideOutVertically(tween(300)) { -it / 2 },
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Text(
                "+1 story · ${session.nameOf(lastTag[0])}",
                style = NumberStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accent)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun RisingWord(word: String, highlight: Color?) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(600, easing = Motion.Emphasized)) }
    Text(
        word,
        style = MaterialTheme.typography.bodyLarge,
        color = highlight ?: MaterialTheme.colorScheme.onSurface,
        fontWeight = if (highlight != null) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 6.dp.toPx() },
    )
}

/** "Stories caught 3 / 5" + chips; tap a chip for Rename / Hear it / Remove. */
@Composable
private fun StoriesCaught(
    session: VoiceSession,
    onRename: (String) -> Unit,
    onHear: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shown = session.shown
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Stories caught", Modifier.semantics { heading() }) {
            AnimatedContent(
                targetState = shown.size,
                transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
                label = "count",
            ) { c -> Text("$c / $TotalStories", style = NumberStyle.copy(fontSize = 15.sp)) }
        }
        if (shown.isEmpty()) {
            Text(
                "Say a story out loud and it lands here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                shown.forEach { tag ->
                    key(tag) { StoryChip(session.nameOf(tag), { onRename(tag) }, { onHear(tag) }, { onRemove(tag) }) }
                }
            }
        }
    }
}

@Composable
private fun StoryChip(name: String, onRename: () -> Unit, onHear: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val pop = remember { Animatable(.4f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, Motion.pop()) }
    val accent = ImpervirTheme.extras.accent
    Box(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = ((pop.value - .4f) / .6f).coerceIn(0f, 1f) }) {
        AssistChip(
            onClick = { menu = true },
            label = { Text(name) },
            leadingIcon = { Box(Modifier.size(12.dp).clip(CircleShape).background(accent)) },
            shape = CircleShape,
            colors = AssistChipDefaults.assistChipColors(containerColor = Glass, labelColor = MaterialTheme.colorScheme.onSurface),
            border = BorderStroke(1.dp, GlassLine),
            modifier = Modifier.semantics { contentDescription = "Story: $name. Options" },
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                onClick = { menu = false; onRename() },
            )
            DropdownMenuItem(
                text = { Text("Hear it") },
                leadingIcon = { Icon(Icons.Rounded.GraphicEq, null) },
                onClick = { menu = false; onHear() },
            )
            DropdownMenuItem(
                text = { Text("Remove") },
                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
                onClick = { menu = false; onRemove() },
            )
        }
    }
}

/** Pause/Resume, Add to chat, and the ••• menu, on a glass pill above the navigation bar. */
@Composable
private fun VoiceControls(
    paused: Boolean,
    onTogglePause: () -> Unit,
    onAddToChat: () -> Unit,
    onTypeInstead: () -> Unit,
    onDiscard: () -> Unit,
) {
    var more by remember { mutableStateOf(false) }
    Surface(
        shape = CircleShape,
        color = Glass,
        border = BorderStroke(1.dp, GlassLine),
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 12.dp)
            .fillMaxWidth(),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = onTogglePause,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Glass),
            ) {
                AnimatedContent(paused, label = "pause") { p ->
                    Icon(
                        if (p) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = if (p) "Resume" else "Pause",
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onAddToChat,
                modifier = Modifier.height(56.dp),
                contentPadding = PaddingValues(horizontal = 26.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Add to chat", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
            }
            Spacer(Modifier.weight(1f))
            Box {
                FilledTonalIconButton(
                    onClick = { more = true },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Glass),
                ) { Icon(Icons.Rounded.MoreHoriz, contentDescription = "More options") }
                DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                    DropdownMenuItem(
                        text = { Text("Type instead") },
                        leadingIcon = { Icon(Icons.Rounded.Keyboard, null) },
                        onClick = { more = false; onTypeInstead() },
                    )
                    DropdownMenuItem(
                        text = { Text("Discard recording") },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                        ),
                        onClick = { more = false; onDiscard() },
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename story") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Shown on your chips and in your drafts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true, label = { Text("Story name") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value) }, enabled = value.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
