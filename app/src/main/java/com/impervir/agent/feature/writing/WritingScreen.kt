package com.impervir.agent.feature.writing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.impervir.agent.data.Check
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.data.Platform
import com.impervir.agent.data.PostStatus
import com.impervir.agent.data.SampleData
import com.impervir.agent.navigation.Destination
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.components.AddPill
import com.impervir.agent.ui.components.AddedBlock
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.components.SectionTitle
import com.impervir.agent.ui.ember.Ember
import com.impervir.agent.ui.ember.EmberMood
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val STEP_MS = 1700
private const val TYPE_START = 3400
private const val CHARS_PER_MS = 0.08f

private val phrases = listOf("Listening back", "Finding your stories", "Writing like you", "Cutting the clichés", "7 posts ready")

private val activity = listOf(
    1900 to "Found 5 stories in your check-in",
    3000 to "Matched your style from 46 past posts",
    4100 to "Rewrote 'game-changer' to 'saved her 3 hours'",
    5300 to "Planned 4 Telegram posts at 6 PM",
    6500 to "Skipped hashtags on LinkedIn",
)

/** The agent writing this week's posts, live. Follows the app theme (light ground in the iOS design). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WritingScreen(repository: ImpervirRepository, navigator: Navigator) {
    val drafts by repository.drafts.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // The three drafts that need the person; Telegram ones auto-post.
    val live = remember(drafts) { drafts.filter { it.status == PostStatus.NeedsApproval && it.platform != Platform.Telegram }.take(3) }
    val offsets = remember(live) { live.runningFold(0) { a, p -> a + p.body.length } }
    val totalChars = offsets.last()
    val typeEnd = TYPE_START + (totalChars / CHARS_PER_MS).toInt()
    val endMs = maxOf(typeEnd, 4 * STEP_MS) + 400

    // Elapsed time drives everything; saved so rotation doesn't restart the run.
    var e by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (e < endMs) { delay(50); e += 50 }
    }

    val typedN = if (e < TYPE_START) 0 else minOf(totalChars, ((e - TYPE_START) * CHARS_PER_MS).toInt())
    val typingDone = typedN >= totalChars
    val phase = if (typingDone && e >= 4 * STEP_MS) 4 else minOf(e / STEP_MS, 3)
    val done = phase == 4
    LaunchedEffect(done) { if (done) haptics.performHapticFeedback(HapticFeedbackType.LongPress) }

    val mood = when {
        done -> EmberMood.Happy
        phase == 0 -> EmberMood.Listening
        phase == 1 -> EmberMood.Thinking
        typedN in 1 until totalChars -> EmberMood.Typing
        else -> EmberMood.Writing
    }
    val needCount = drafts.count { it.status == PostStatus.NeedsApproval }

    var showStories by rememberSaveable { mutableStateOf(false) }
    var showActivity by rememberSaveable { mutableStateOf(false) }
    var showPlatforms by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeTopAppBar(
                title = { BlurRiseText("Your agent is on it", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = { navigator.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to check-in") }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = MaterialTheme.colorScheme.surface),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val ctaAlpha by animateFloatAsState(if (done) 1f else 0.45f, tween(400), label = "cta")
                    Button(
                        onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); navigator.switchTab(Destination.Posts) },
                        enabled = done,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface, disabledContentColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier.fillMaxWidth().height(56.dp).graphicsLayer { alpha = ctaAlpha },
                    ) {
                        Text("Review ", style = MaterialTheme.typography.labelLarge)
                        Text("$needCount", style = NumberStyle.copy(fontSize = 15.sp))
                        Text(" drafts", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    TextButton(onClick = { navigator.back() }, modifier = Modifier.padding(top = 4.dp)) {
                        Text("Cancel", color = ImpervirTheme.extras.muted)
                    }
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize()) {
            DriftBackground()
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(inner).padding(horizontal = 20.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                // Ember at work + status line
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ember(mood, size = 72.dp)
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Step ${phase + 1} of 5", style = NumberStyle.copy(fontSize = 13.sp), color = ImpervirTheme.extras.muted, modifier = Modifier.weight(1f))
                            AnimatedVisibility(!done, enter = fadeIn(), exit = fadeOut()) {
                                TextButton(onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch {
                                        val r = snackbar.showSnackbar("I'll notify you when it's ready", actionLabel = "Go home")
                                        if (r == SnackbarResult.ActionPerformed) navigator.switchTab(Destination.Home)
                                    }
                                }) { Text("Run in background") }
                            }
                        }
                        StatusPhrase(phrases[phase])
                        Row {
                            Text("from a ", style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
                            Text("2:04", style = NumberStyle.copy(fontSize = 12.sp), color = ImpervirTheme.extras.muted)
                            Text(" check-in", style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
                        }
                    }
                }

                // Live drafts
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Live drafts", Modifier.semantics { heading() })
                    val active = offsets.indexOfLast { typedN >= it }.coerceIn(0, live.lastIndex.coerceAtLeast(0))
                    live.forEachIndexed { i, post ->
                        val n = (typedN - offsets[i]).coerceIn(0, post.body.length)
                        val isDone = typedN >= offsets[i + 1]
                        val status = when {
                            e < TYPE_START -> DraftStatus.Waiting
                            isDone -> DraftStatus.Ready
                            i == active -> DraftStatus.Writing
                            else -> DraftStatus.Queued
                        }
                        StaggerIn(i) {
                            LiveDraftCard(
                                platform = post.platform, full = post.body, typed = n,
                                caret = e >= TYPE_START && i == active && !typingDone,
                                status = status,
                                source = post.source?.let { "${it.label} ${it.timestamp}" },
                            )
                        }
                    }
                }

                // The five checks
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionTitle("Checks", Modifier.semantics { heading() })
                    Row(Modifier.fillMaxWidth()) {
                        Check.entries.forEachIndexed { i, c ->
                            CheckDot(
                                label = c.label,
                                on = i < 3 && e >= (i + 1) * STEP_MS,
                                next = c == Check.YouApprove,
                                pulse = c == Check.YouApprove && done,
                                dim = c == Check.ItLearns,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Optional sections, added by the person
                val heard = if (e < STEP_MS) 0 else minOf(5, (e - STEP_MS) / 400 + 1)
                AddedBlock(showStories, "Stories it heard", onRemove = { showStories = false }) {
                    Text("$heard of 5", style = NumberStyle.copy(fontSize = 12.sp), color = ImpervirTheme.extras.muted, modifier = Modifier.padding(bottom = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SampleData.stories.forEachIndexed { i, s ->
                            val on by animateFloatAsState(if (i < heard) 1f else 0f, Motion.soft(), label = "story")
                            Row(
                                Modifier.graphicsLayer { alpha = 0.25f + 0.75f * on; translationX = (1f - on) * 12.dp.toPx() },
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(s.timestamp, style = NumberStyle.copy(fontSize = 12.sp), color = ImpervirTheme.extras.accent, modifier = Modifier.width(40.dp).padding(top = 2.dp))
                                Text(s.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                AddedBlock(showActivity, "Activity", onRemove = { showActivity = false }) {
                    ActivityTicker(activity.filter { e >= it.first }.map { it.second })
                }
                AddedBlock(showPlatforms, "Platforms", onRemove = { showPlatforms = false }) {
                    val counts = drafts.groupingBy { it.platform }.eachCount()
                    val timing = mapOf(Platform.LinkedIn to (3400 to 2400), Platform.X to (3800 to 2000), Platform.Threads to (4200 to 2400), Platform.Telegram to (4600 to 2000))
                    Row(Modifier.fillMaxWidth()) {
                        listOf(Platform.LinkedIn, Platform.X, Platform.Threads, Platform.Telegram).forEach { p ->
                            val (start, dur) = timing.getValue(p)
                            val f = ((e - start).toFloat() / dur).coerceIn(0f, 1f)
                            val c = counts[p] ?: 0
                            PlatformRing(p, f, "$c post" + if (c == 1) "" else "s", Modifier.weight(1f))
                        }
                    }
                }
                if (!showStories || !showActivity || !showPlatforms) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!showStories) AddPill("Stories it heard", { showStories = true })
                        if (!showActivity) AddPill("Activity", { showActivity = true })
                        if (!showPlatforms) AddPill("Platforms", { showPlatforms = true })
                    }
                }
            }
        }
    }
}

/** Rises a card in with a small per-index delay. */
@Composable
private fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(index * 120L); p.animateTo(1f, Motion.soft()) }
    Box(Modifier.graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 16.dp.toPx() }) { content() }
}
