package com.impervir.agent.feature.talk

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.navigation.Destination
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.ember.Ember
import com.impervir.agent.ui.ember.EmberMood
import com.impervir.agent.ui.theme.ImmersiveDark
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Weekly check-in as a chat. Always dark. */
@Composable
fun TalkScreen(repository: ImpervirRepository, navigator: Navigator, contentPadding: PaddingValues) {
    ImmersiveDark { TalkContent(navigator, contentPadding) }
}

private sealed interface TalkSheet {
    data object Attach : TalkSheet
    data object Settings : TalkSheet
    data class Story(val msgId: Int) : TalkSheet
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TalkContent(navigator: Navigator, contentPadding: PaddingValues) {
    val state = rememberTalkState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var draft by rememberSaveable { mutableStateOf("") }
    var recording by rememberSaveable { mutableStateOf(false) }
    var ideasOn by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableIntStateOf(-1) }
    var bar by remember { mutableStateOf<ComposeBar?>(null) }
    var sheet by remember { mutableStateOf<TalkSheet?>(null) }
    var overflow by remember { mutableStateOf(false) }

    fun toast(text: String, undo: (() -> Unit)? = null) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(text, actionLabel = if (undo != null) "Undo" else null, duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) undo?.invoke()
        }
    }

    fun runAnswer(msg: TalkMsg?) {
        if (state.typing) return
        val replies = state.answer(msg)
        draft = ""; bar = null; selectedId = -1
        scope.launch {
            delay(1300)
            state.deliver(replies)
            if (state.voiceByDefault && state.qi < 3) recording = true
        }
    }

    fun send() {
        val d = draft.trim()
        if (d.isEmpty() || state.typing) return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val editing = bar?.editId
        if (editing != null) {
            state.replace(editing, d)
            draft = ""; bar = null
            toast("Message updated")
            return
        }
        runAnswer(state.newMsg(MsgKind.User, d))
    }

    fun onAction(m: TalkMsg, a: MsgAction) {
        selectedId = -1
        when (a) {
            MsgAction.Copy -> { clipboard.setText(AnnotatedString(m.text)); toast(if (m.kind == MsgKind.Voice) "Transcript copied" else "Copied") }
            MsgAction.Reply -> { bar = ComposeBar(if (m.isAgent) "Replying to Agent" else "Replying to your note", m.text, null) }
            MsgAction.Edit -> {
                draft = m.text
                bar = ComposeBar(
                    when (m.kind) { MsgKind.Voice -> "Editing transcript"; MsgKind.User -> "Editing your message"; else -> "Editing the question" },
                    m.text, m.id,
                )
            }
            MsgAction.AskDifferently -> if (!state.typing) {
                val alt = state.askDifferently()
                scope.launch { delay(1000); state.deliverOne(alt) }
            }
            MsgAction.ReadAloud -> toast("Reading aloud")
            MsgAction.Play -> toast("Playing voice note")
            MsgAction.Delete -> {
                val i = state.messages.indexOfFirst { it.id == m.id }
                if (i >= 0) {
                    state.messages.removeAt(i)
                    toast("Deleted") { state.messages.add(i.coerceAtMost(state.messages.size), m) }
                }
            }
        }
    }

    fun restart() {
        state.restart()
        sheet = null; recording = false; bar = null; draft = ""
        toast("Check-in restarted")
    }

    // System back closes the innermost thing first.
    BackHandler(enabled = recording || bar != null || selectedId != -1) {
        when {
            recording -> recording = false
            bar != null -> { if (bar?.editId != null) draft = ""; bar = null }
            else -> selectedId = -1
        }
    }

    // Keep the newest message in view.
    val itemCount = state.messages.size + if (state.typing) 1 else 0
    LaunchedEffect(itemCount) { if (itemCount > 0) listState.animateScrollToItem(itemCount - 1) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    Ember(if (state.typing) EmberMood.Talking else EmberMood.Idle, size = 40.dp, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
                },
                title = { CheckInTitle(state) },
                actions = {
                    IconButton(onClick = { navigator.open(Destination.Voice) }) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = "Full-screen voice mode")
                    }
                    Box {
                        IconButton(onClick = { overflow = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More check-in options") }
                        DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                            DropdownMenuItem(
                                text = { Text("That's my week") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null) },
                                onClick = { overflow = false; navigator.open(Destination.Writing) },
                            )
                            DropdownMenuItem(
                                text = { Text("Check-in settings") },
                                leadingIcon = { Icon(Icons.Rounded.Tune, null) },
                                onClick = { overflow = false; sheet = TalkSheet.Settings },
                            )
                            DropdownMenuItem(
                                text = { Text("Restart check-in") },
                                leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                                onClick = { overflow = false; restart() },
                            )
                        }
                    }
                },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = inner.calculateTopPadding())
                .padding(bottom = contentPadding.calculateBottomPadding())
                .consumeWindowInsets(contentPadding)
                .imePadding(),
        ) {
            Box(Modifier.weight(1f)) {
                val lastMine = state.messages.indexOfLast { it.isMine }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.messages.size, key = { state.messages[it].id }) { i ->
                        val m = state.messages[i]
                        val next = state.messages.getOrNull(i + 1)
                        val endOfRun = m.isAgent && next?.isAgent != true && !(next == null && state.typing)
                        Entrance(m.id, state, Modifier.animateItem()) {
                            when (m.kind) {
                                MsgKind.Agent, MsgKind.Final -> AgentMessage(
                                    msg = m, state = state,
                                    showAvatar = endOfRun, showTime = endOfRun,
                                    selected = selectedId == m.id,
                                    onToggle = { selectedId = if (selectedId == m.id) -1 else m.id },
                                    onAction = { onAction(m, it) },
                                    onWritePosts = { navigator.open(Destination.Writing) },
                                )
                                MsgKind.User, MsgKind.Voice -> MyMessage(m, showRead = i == lastMine) { onAction(m, it) }
                                MsgKind.Saved -> SavedStoryChip(m) { sheet = TalkSheet.Story(m.id) }
                            }
                        }
                    }
                    if (state.typing) item(key = "typing") { Box(Modifier.animateItem()) { TypingIndicator() } }
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
            }
            ComposerArea(
                state = state,
                draft = draft,
                onDraft = { draft = it },
                bar = bar,
                onCloseBar = { if (bar?.editId != null) draft = ""; bar = null },
                recording = recording,
                ideasOn = ideasOn,
                onIdeas = { ideasOn = it },
                onAttach = { sheet = TalkSheet.Attach },
                onSend = ::send,
                onPickIdea = { runAnswer(state.newMsg(MsgKind.User, it)) },
                onSkip = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); runAnswer(null) },
                onStartRec = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedId = -1
                    recording = true
                },
                onCancelRec = { recording = false; toast("Recording discarded") },
                onSendRec = { rec ->
                    val script = state.currentScript
                    val text = script.spoken.split(" ").take(rec.words).joinToString(" ")
                    val dur = if (rec.seconds < 3) "0:08" else "${rec.seconds / 60}:${(rec.seconds % 60).toString().padStart(2, '0')}"
                    recording = false
                    runAnswer(
                        state.newMsg(
                            MsgKind.Voice, text,
                            highlight = script.highlight.takeIf { text.contains(it) },
                            duration = dur, wave = rec.levels.takeLast(26),
                        ),
                    )
                },
                onVoiceMode = { recording = false; navigator.open(Destination.Voice) },
            )
        }
    }

    when (val s = sheet) {
        TalkSheet.Attach -> AttachSheet(onDismiss = { sheet = null }) { sheet = null; toast("Added to this check-in") }
        TalkSheet.Settings -> CheckInSettingsSheet(
            state = state,
            onDismiss = { sheet = null },
            onFinish = { sheet = null; navigator.open(Destination.Writing) },
            onRestart = { restart() },
        )
        is TalkSheet.Story -> {
            val msg = state.messages.firstOrNull { it.id == s.msgId }
            if (msg == null) sheet = null else StorySheet(
                storyIndex = msg.storyIndex, tag = msg.tag,
                onDismiss = { sheet = null },
                onEdit = {
                    sheet = null
                    draft = msg.text
                    toast("Type the change, then send")
                },
                onRemove = {
                    sheet = null
                    val i = state.messages.indexOf(msg)
                    state.messages.remove(msg)
                    toast("Story removed") { state.messages.add(i.coerceIn(0, state.messages.size), msg) }
                },
            )
        }
        null -> Unit
    }
}

/** "Monday check-in", the question counter and three progress segments. */
@Composable
private fun CheckInTitle(state: TalkState) {
    Column {
        BlurRiseText("Monday check-in", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (state.qi < 3) "Question ${state.qi + 1} of 3" else "Question 3 of 3 · done",
                style = NumberStyle.copy(fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                color = ImpervirTheme.extras.muted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    val c by animateColorAsState(
                        if (i < state.qi) ImpervirTheme.extras.accent else Color.White.copy(alpha = 0.16f), label = "seg",
                    )
                    Box(Modifier.width(28.dp).height(4.dp).background(c, RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}

/** Rises and fades a row in the first time it shows. */
@Composable
private fun Entrance(id: Int, state: TalkState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val fresh = remember(id) { id !in state.revealed }
    val p = remember(id) { Animatable(if (fresh) 0f else 1f) }
    LaunchedEffect(id) {
        if (p.value < 1f) {
            delay(seedDelay(id))
            p.animateTo(1f, Motion.soft())
        }
    }
    Box(modifier.graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 14.dp.toPx() }) { content() }
}
