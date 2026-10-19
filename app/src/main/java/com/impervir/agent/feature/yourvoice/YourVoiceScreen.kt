package com.impervir.agent.feature.yourvoice

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.navigation.Destination
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import kotlinx.coroutines.launch

/** A tone slider: 0 = left word, 100 = right word. */
internal data class Trait(val key: String, val left: String, val right: String, val default: Float)

internal val traitDefs = listOf(
    Trait("casual", "Casual", "Formal", 25f),
    Trait("length", "Short", "Long", 30f),
    Trait("tone", "Playful", "Serious", 30f),
    Trait("structure", "Story-led", "Tip-led", 20f),
)

/** Where each learned rule came from (the agent never invents). */
internal val ruleSources = mapOf(
    "r1" to "From your edit on Oct 14",
    "r2" to "From your edit on Oct 9",
    "r3" to "From your edit on Oct 6",
    "r4" to "From your edit on Sep 30",
    "r5" to "From your edit on Sep 24",
    "r6" to "From your edit on Sep 18",
)

internal val storyTags = mapOf(
    "s1" to listOf("Customer win", "Notion planner"),
    "s2" to listOf("Customer win", "Notion planner"),
    "s3" to listOf("Lesson", "Focus Planner"),
    "s4" to listOf("Lesson learned", "Launch"),
    "s5" to listOf("What's next", "Weekly Review Kit"),
)

/** UI state for Your voice. Rule on/off and new rules live in the repository; local edits stay here. */
@Stable
internal class YourVoiceState {
    val traits = mutableStateMapOf<String, Float>().apply { traitDefs.forEach { put(it.key, it.default) } }
    val ruleEdits = mutableStateMapOf<String, String>()
    val deletedRules = mutableStateListOf<String>()
    var editingRule by mutableStateOf<String?>(null)
    var showTrend by mutableStateOf(false)
    var showRuleBox by mutableStateOf(false)
    var showBanBox by mutableStateOf(false)
    val banned = mutableStateListOf("game-changer", "unlock your potential", "Here’s the thing", "level up")
    var importing by mutableStateOf(false)
    var importProgress by mutableFloatStateOf(0f)
    var pasting by mutableStateOf(false)

    fun left(key: String) = (traits[key] ?: 50f) < 50f

    fun sample(): String {
        val opener = if (left("casual")) "So a customer emailed me." else "A customer wrote in to tell me."
        val body = if (left("length")) "She stopped missing deadlines." else "After eleven days with my planner, she stopped missing client deadlines."
        val closer = if (left("tone")) "Eleven days, not one miss. Nice." else "The result held. It was not luck."
        val tip = if (left("structure")) "The part she liked most was the page I almost cut." else "Tip: keep the one page you almost cut."
        return listOf(opener, body, closer, tip).joinToString(" ")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourVoiceScreen(repository: ImpervirRepository, navigator: Navigator) {
    val rules by repository.rules.collectAsStateWithLifecycle()
    val state = remember { YourVoiceState() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    fun snack(msg: String, action: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(msg, action, withDismissAction = action == null) == SnackbarResult.ActionPerformed) onAction()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { BlurRiseText("Your voice", style = LocalTextStyle.current) },
                navigationIcon = {
                    IconButton(onClick = { navigator.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
                .animateContentSize(Motion.soft()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Learned from 46 past posts and 5 weeks of edits",
                style = MaterialTheme.typography.bodyMedium,
                color = ImpervirTheme.extras.muted,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            val pad = Modifier.padding(horizontal = 16.dp)
            MatchCard(state, pad.riseIn(0))
            ToneCard(state, onSettled = { snack("I'll write this way from now on") }, modifier = pad.riseIn(1))
            RulesCard(
                rules = rules.filterNot { it.id in state.deletedRules },
                state = state,
                onToggle = { repository.toggleRule(it) },
                onDelete = { id ->
                    state.deletedRules.add(id)
                    snack("Rule deleted", "Undo") { state.deletedRules.remove(id) }
                },
                onSaveEdit = { id, text ->
                    state.ruleEdits[id] = text
                    state.editingRule = null
                    snack("Rule updated")
                },
                onAdd = { text ->
                    repository.addRule(text)
                    snack("Rule added")
                },
                modifier = pad.riseIn(2),
            )
            NeverSayCard(
                state = state,
                onRemove = { phrase ->
                    val idx = state.banned.indexOf(phrase)
                    state.banned.remove(phrase)
                    snack("Removed “$phrase”", "Undo") { state.banned.add(idx.coerceIn(0, state.banned.size), phrase) }
                },
                onAdd = { phrase ->
                    state.banned.add(phrase)
                    snack("I won't use that phrase")
                },
                modifier = pad.riseIn(3),
            )
            StoriesSection(onUse = { navigator.switchTab(Destination.Talk) }, modifier = Modifier.riseIn(4))
            TeachCard(
                state = state,
                onImport = {
                    if (!state.importing) {
                        state.importing = true
                        scope.launch {
                            val p = Animatable(0f)
                            p.animateTo(1f, tween(1900)) { state.importProgress = value }
                            state.importing = false
                            state.importProgress = 0f
                            snack("Imported 12 posts")
                        }
                    }
                },
                onLearn = { ok ->
                    if (ok) {
                        state.pasting = false
                        snack("Got it. I'll learn from this post")
                    } else snack("Paste a post first")
                },
                modifier = pad.riseIn(5),
            )
        }
    }
}

/** Staggered entrance, once. */
@Composable
internal fun Modifier.riseIn(index: Int): Modifier {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, Motion.reveal(delay = 120 + index * Motion.Stagger)) }
    return this.graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 22.dp.toPx()
    }
}
