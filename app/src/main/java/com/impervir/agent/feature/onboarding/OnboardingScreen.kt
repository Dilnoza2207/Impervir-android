package com.impervir.agent.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.data.Platform
import com.impervir.agent.data.SampleData
import com.impervir.agent.data.Store
import com.impervir.agent.navigation.Destination
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.components.AddPill
import com.impervir.agent.ui.components.AddedBlock
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.components.ImpervirCard
import com.impervir.agent.ui.components.PlatformBadge
import com.impervir.agent.ui.components.StoreBadge
import com.impervir.agent.ui.ember.Ember
import com.impervir.agent.ui.ember.EmberMood
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val StepCount = 5

private enum class Link { Idle, Loading, Done }
private enum class VoiceStage { Idle, Reading, Learned }

private val learnedTraits = listOf(
    "Short first line, no question hooks",
    "Uses numbers instead of adjectives",
    "Ends with a real question",
)
private val checkInTimes = listOf("8 AM", "9 AM", "12 PM", "6 PM")
private val dayShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val dayFull = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@Composable
fun OnboardingScreen(repository: ImpervirRepository, navigator: Navigator) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var step by rememberSaveable { mutableIntStateOf(0) }
    // Connection states, saved as enum names so they survive rotation.
    var accounts by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    var stores by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    var moreAccounts by rememberSaveable { mutableStateOf(false) }
    var voice by rememberSaveable { mutableStateOf(VoiceStage.Idle.name) }
    var traitsShown by rememberSaveable { mutableIntStateOf(0) }
    var pasteOpen by rememberSaveable { mutableStateOf(false) }
    var pasteText by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableIntStateOf(0) }
    var time by rememberSaveable { mutableStateOf("9 AM") }
    var showQuestions by rememberSaveable { mutableStateOf(false) }

    fun linkOf(map: Map<String, String>, id: String) = map[id]?.let { Link.valueOf(it) } ?: Link.Idle
    fun connect(isStore: Boolean, id: String) {
        val map = if (isStore) stores else accounts
        if (linkOf(map, id) != Link.Idle) return
        if (isStore) stores = stores + (id to Link.Loading.name) else accounts = accounts + (id to Link.Loading.name)
        scope.launch {
            delay(800)
            if (isStore) stores = stores + (id to Link.Done.name) else accounts = accounts + (id to Link.Done.name)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    fun learnVoice() {
        if (voice != VoiceStage.Idle.name) return
        voice = VoiceStage.Reading.name
        pasteOpen = false
        scope.launch {
            delay(1600)
            voice = VoiceStage.Learned.name
            for (i in 1..learnedTraits.size) {
                traitsShown = i
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(450)
            }
        }
    }

    val accountsDone = accounts.values.count { it == Link.Done.name }
    val storesDone = stores.values.count { it == Link.Done.name }
    val canContinue = when (step) {
        1 -> accountsDone >= 1
        2 -> storesDone >= 1
        3 -> voice == VoiceStage.Learned.name && traitsShown >= learnedTraits.size
        else -> true
    }

    // System back walks back through the steps before leaving onboarding.
    BackHandler(enabled = step > 0) { step-- }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        StepProgress(step, Modifier.padding(horizontal = 24.dp, vertical = 16.dp))

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(380, easing = Motion.Emphasized)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(300)))
                    .togetherWith(slideOutHorizontally(tween(300)) { if (forward) -it / 6 else it / 6 } + fadeOut(tween(200)))
            },
            modifier = Modifier.weight(1f),
            label = "onboardingStep",
        ) { s ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
                    .animateContentSize(Motion.soft()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (s) {
                    0 -> WelcomeStep()
                    1 -> AccountsStep(
                        link = { linkOf(accounts, it) },
                        more = moreAccounts,
                        onMore = { moreAccounts = it },
                        connected = accountsDone,
                        onConnect = { connect(false, it) },
                    )
                    2 -> StoreStep(link = { linkOf(stores, it) }, connected = storesDone, onConnect = { connect(true, it) })
                    3 -> VoiceStep(
                        stage = VoiceStage.valueOf(voice),
                        traitsShown = traitsShown,
                        pasteOpen = pasteOpen,
                        onPasteOpen = { pasteOpen = it },
                        pasteText = pasteText,
                        onPasteText = { pasteText = it },
                        onLearn = { learnVoice() },
                    )
                    else -> CheckInStep(
                        day = day, onDay = { day = it },
                        time = time, onTime = { time = it },
                        showQuestions = showQuestions, onShowQuestions = { showQuestions = it },
                    )
                }
            }
        }

        // Footer: Back and Continue
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(visible = step > 0) {
                TextButton(onClick = { step-- }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
            }
            Spacer(Modifier.weight(1f))
            val last = step == StepCount - 1
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (last) navigator.switchTab(Destination.Home) else step++
                },
                enabled = canContinue,
                modifier = Modifier.height(52.dp),
            ) {
                AnimatedContent(
                    when {
                        last -> "Finish setup"
                        step == 0 -> "Get started"
                        else -> "Continue"
                    },
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                    label = "cta",
                ) { Text(it, modifier = Modifier.padding(horizontal = 12.dp)) }
            }
        }
    }
}

@Composable
private fun StepProgress(step: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "Step ${step + 1} of $StepCount" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(StepCount) { i ->
            val fill by animateFloatAsState(if (i <= step) 1f else 0f, tween(400, easing = Motion.Emphasized), label = "seg$i")
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fill)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(ImpervirTheme.extras.accent),
                )
            }
        }
    }
}

@Composable
private fun StepHeader(eyebrow: String, title: String, sub: String) {
    Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = ImpervirTheme.extras.accent, modifier = Modifier.padding(top = 8.dp))
    BlurRiseText(title, style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
    Text(sub, style = MaterialTheme.typography.bodyLarge, color = ImpervirTheme.extras.muted)
}

// ------------------------------------------------------------------ Step 1: Welcome

@Composable
private fun WelcomeStep() {
    Spacer(Modifier.height(24.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Ember(EmberMood.Waving, size = 160.dp)
    }
    Spacer(Modifier.height(12.dp))
    Text("Welcome to Impervir", style = MaterialTheme.typography.labelLarge, color = ImpervirTheme.extras.accent)
    BlurRiseText("Talk for two minutes. Sell all week.", style = MaterialTheme.typography.displayMedium, modifier = Modifier.semantics { heading() })
    Text(
        "I write your posts from your own stories, post them, and show you which ones made money.",
        style = MaterialTheme.typography.bodyLarge,
        color = ImpervirTheme.extras.muted,
    )
}

// ------------------------------------------------------------------ Step 2: Accounts

@Composable
private fun AccountsStep(link: (String) -> Link, more: Boolean, onMore: (Boolean) -> Unit, connected: Int, onConnect: (String) -> Unit) {
    StepHeader("Step 2 of 5", "Connect your accounts.", "Connect at least one. You can add the rest any time.")
    ImpervirCard(Modifier.fillMaxWidth()) {
        LinkRow({ PlatformBadge(Platform.LinkedIn, size = 40.dp) }, "LinkedIn", "Maya Chen", link("li")) { onConnect("li") }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LinkRow({ PlatformBadge(Platform.X, size = 40.dp) }, "X", "@mayachen", link("x")) { onConnect("x") }
    }
    AddedBlock(visible = more, title = "More platforms", onRemove = { onMore(false) }) {
        LinkRow({ PlatformBadge(Platform.Threads, size = 40.dp) }, "Threads", "@mayachen", link("th")) { onConnect("th") }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LinkRow({ PlatformBadge(Platform.Telegram, size = 40.dp) }, "Telegram", "Your channel", link("tg")) { onConnect("tg") }
    }
    if (!more) AddPill("Add Threads or Telegram", onClick = { onMore(true) })
    Note(Icons.Rounded.Shield, "I never like, follow or reply for you.")
    Text(
        if (connected == 0) "None connected yet" else "$connected of 4 connected",
        style = MaterialTheme.typography.bodySmall,
        color = ImpervirTheme.extras.muted,
    )
}

// ------------------------------------------------------------------ Step 3: Store

@Composable
private fun StoreStep(link: (String) -> Link, connected: Int, onConnect: (String) -> Unit) {
    StepHeader("Step 3 of 5", "Connect your store.", "Pick the store you sell from. Tap one to connect it.")
    ImpervirCard(Modifier.fillMaxWidth()) {
        LinkRow({ StoreBadge(Store.Stripe, size = 40.dp) }, "Stripe", "Card payments", link("stripe")) { onConnect("stripe") }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LinkRow({ StoreBadge(Store.Gumroad, size = 40.dp) }, "Gumroad", "Digital downloads", link("gumroad")) { onConnect("gumroad") }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LinkRow({ StoreBadge(Store.LemonSqueezy, size = 40.dp) }, "Lemon Squeezy", "Digital downloads and tax", link("lemon")) { onConnect("lemon") }
    }
    Note(Icons.Rounded.ShoppingBag, "So I can tell you which posts made money.")
    Text(
        if (connected == 0) "No store connected yet" else "$connected connected",
        style = MaterialTheme.typography.bodySmall,
        color = ImpervirTheme.extras.muted,
    )
}

// ------------------------------------------------------------------ Step 4: Voice

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceStep(
    stage: VoiceStage,
    traitsShown: Int,
    pasteOpen: Boolean,
    onPasteOpen: (Boolean) -> Unit,
    pasteText: String,
    onPasteText: (String) -> Unit,
    onLearn: () -> Unit,
) {
    StepHeader("Step 4 of 5", "Teach me your voice.", "Import a few posts you already wrote. I learn how you sound from them.")
    AnimatedContent(stage, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "voiceStage") { st ->
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (st) {
                VoiceStage.Idle -> {
                    Button(onClick = onLearn, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        PlatformBadge(Platform.LinkedIn, size = 28.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Import from LinkedIn")
                    }
                    AddedBlock(visible = pasteOpen, title = "Paste your posts", onRemove = { onPasteOpen(false) }) {
                        OutlinedTextField(
                            value = pasteText,
                            onValueChange = onPasteText,
                            placeholder = { Text("Paste 3 of your past posts") },
                            minLines = 5,
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Paste 3 of your past posts" },
                        )
                        OutlinedButton(onClick = onLearn, enabled = pasteText.isNotBlank(), modifier = Modifier.padding(top = 10.dp)) { Text("Analyze") }
                    }
                    if (!pasteOpen) AddPill("Paste posts instead", onClick = { onPasteOpen(true) })
                }
                VoiceStage.Reading -> {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Ember(EmberMood.Thinking, size = 120.dp)
                        Text("Reading your posts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth(0.6f).padding(top = 12.dp))
                    }
                }
                VoiceStage.Learned -> {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Ember(if (traitsShown >= learnedTraits.size) EmberMood.Proud else EmberMood.Happy, size = 120.dp)
                        Text("Here is what I learned.", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        learnedTraits.forEachIndexed { i, t ->
                            AnimatedVisibility(visible = i < traitsShown, enter = scaleIn(Motion.pop(), initialScale = 0.7f) + fadeIn(tween(200))) {
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(t, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
                                    }
                                }
                            }
                        }
                    }
                    Text("You can edit these any time in Your voice.", style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Step 5: Check-in

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckInStep(
    day: Int,
    onDay: (Int) -> Unit,
    time: String,
    onTime: (String) -> Unit,
    showQuestions: Boolean,
    onShowQuestions: (Boolean) -> Unit,
) {
    StepHeader("Step 5 of 5", "Pick your check-in.", "One short session a week. I ask, you answer, I write.")
    Text("Day", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        dayShort.forEachIndexed { i, d -> FilterChip(selected = day == i, onClick = { onDay(i) }, label = { Text(d) }) }
    }
    Text("Time", style = MaterialTheme.typography.labelLarge)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        checkInTimes.forEachIndexed { i, t ->
            SegmentedButton(
                selected = time == t,
                onClick = { onTime(t) },
                shape = SegmentedButtonDefaults.itemShape(i, checkInTimes.size),
            ) { Text(t) }
        }
    }
    ImpervirCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ember(EmberMood.Curious, size = 44.dp)
            AnimatedContent("Every ${dayFull[day]} at $time I'll ask you 3 questions.", label = "preview", modifier = Modifier.padding(start = 12.dp)) {
                Text(it, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
    AddedBlock(visible = showQuestions, title = "The 3 questions", onRemove = { onShowQuestions(false) }) {
        SampleData.checkInQuestions.forEachIndexed { i, q ->
            Row(Modifier.padding(vertical = 6.dp)) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", style = NumberStyle.copy(fontSize = 12.sp))
                }
                Text(q, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
    if (!showQuestions) AddPill("See the 3 questions", onClick = { onShowQuestions(true) })
}

// ------------------------------------------------------------------ shared rows

@Composable
private fun LinkRow(leading: @Composable () -> Unit, name: String, hint: String, link: Link, onConnect: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        leading()
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(name, style = MaterialTheme.typography.titleSmall)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
        }
        AnimatedContent(link, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "link") { l ->
            when (l) {
                Link.Idle -> FilledTonalButton(onClick = onConnect, modifier = Modifier.semantics { contentDescription = "Connect $name" }) { Text("Connect") }
                Link.Loading -> CircularProgressIndicator(Modifier.size(24.dp).semantics { contentDescription = "Connecting" }, strokeWidth = 2.5.dp)
                Link.Done -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = ImpervirTheme.extras.positive, modifier = Modifier.size(18.dp))
                    Text("Connected", style = MaterialTheme.typography.labelMedium, color = ImpervirTheme.extras.positive, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun Note(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ImpervirTheme.extras.muted, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = ImpervirTheme.extras.muted, modifier = Modifier.padding(start = 8.dp))
    }
}
