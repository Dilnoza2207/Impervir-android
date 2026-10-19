package com.impervir.agent.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.data.Post
import com.impervir.agent.data.PostStatus
import com.impervir.agent.data.SampleData
import com.impervir.agent.navigation.Destination
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.components.Avatar
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Which bottom sheet is open on Home (only one at a time). */
internal enum class HomeSheet { None, Notifications, Customize, Jot }

/** Optional Home widgets. All off by default; the person turns them on in "Customize Home". */
internal enum class HomeWidget(val title: String, val sub: String) {
    Report("Week 42 report", "What sold last week, as a short recap"),
    Followers("Followers", "New followers on each platform this week"),
    Shortcuts("Shortcuts", "Jot an idea, Your voice, Connect a store"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repository: ImpervirRepository, navigator: Navigator, contentPadding: PaddingValues) {
    val drafts by repository.drafts.collectAsStateWithLifecycle()
    val comments by repository.comments.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // UI state that should survive rotation and tab switches.
    var widgetFlags by rememberSaveable { mutableStateOf("") } // names of enabled HomeWidgets, comma separated
    val enabled = remember(widgetFlags) { widgetFlags.split(',').filter { it.isNotBlank() }.map { HomeWidget.valueOf(it) }.toSet() }
    fun setWidget(w: HomeWidget, on: Boolean) {
        val next = if (on) enabled + w else enabled - w
        widgetFlags = HomeWidget.entries.filter { it in next }.joinToString(",") { it.name }
    }
    var nudgeDismissed by rememberSaveable { mutableStateOf(false) }
    var sheetName by rememberSaveable { mutableStateOf(HomeSheet.None.name) }
    val sheet = HomeSheet.valueOf(sheetName)
    var refreshing by remember { mutableStateOf(false) }

    fun showSnack(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(message, actionLabel, withDismissAction = actionLabel == null, duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) onAction()
        }
    }

    val needApproval = drafts.count { it.status == PostStatus.NeedsApproval }
    val upNext = remember(drafts) {
        drafts.filter { it.status != PostStatus.Skipped && it.status != PostStatus.Posted }
            .sortedBy { minutesOfWeek(it) }
            .take(3)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(SampleData.today, style = MaterialTheme.typography.labelLarge, color = ImpervirTheme.extras.muted)
                },
                actions = {
                    IconButton(onClick = { navigator.open(Destination.Calendar) }) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = "Calendar")
                    }
                    IconButton(onClick = { sheetName = HomeSheet.Notifications.name }) {
                        BadgedBox(badge = { Badge { Text("${homeNotifications.size}") } }) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "Notifications, ${homeNotifications.size} new")
                        }
                    }
                    IconButton(onClick = { navigator.open(Destination.Settings) }) {
                        Avatar(SampleData.userInitials, size = 34.dp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = contentPadding.calculateBottomPadding())) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                scope.launch {
                    delay(900)
                    refreshing = false
                    showSnack("You're up to date")
                }
            },
            modifier = Modifier.fillMaxSize().padding(top = inner.calculateTopPadding()),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BlurRiseText(
                    text = greeting(),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp).semantics { heading() },
                )

                AgentCard(
                    needApproval = needApproval,
                    totalPosts = drafts.size,
                    onReview = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        navigator.switchTab(Destination.Posts)
                    },
                    modifier = Modifier.riseIn(0),
                )

                ThisWeekCard(Modifier.riseIn(1))

                LastWeekCard(
                    onClick = { navigator.switchTab(Destination.Analytics) },
                    modifier = Modifier.riseIn(2),
                )

                AnimatedVisibility(
                    visible = !nudgeDismissed && comments.isNotEmpty(),
                    enter = expandVertically(tween(280)) + fadeIn(tween(280)),
                    exit = shrinkVertically(tween(260)) + fadeOut(tween(180)),
                ) {
                    CommentsNudge(
                        comments = comments,
                        onReply = { navigator.switchTab(Destination.Inbox) },
                        onDismiss = {
                            nudgeDismissed = true
                            showSnack("Nudge hidden", "Undo") { nudgeDismissed = false }
                        },
                        modifier = Modifier.riseIn(3),
                    )
                }

                UpNextSection(
                    posts = upNext,
                    onOpen = { navigator.open(Destination.PostEditor(it.id)) },
                    onOpenCalendar = { navigator.open(Destination.Calendar) },
                    onLater = { post ->
                        val old = post.time
                        repository.reschedule(post.id, post.day, shiftTime(post.time, 60))
                        showSnack("Moved 1 hour later", "Undo") { repository.reschedule(post.id, post.day, old) }
                    },
                    onSkip = { post ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val previous = post.status
                        repository.skip(post.id)
                        showSnack("Skipped. Nothing was posted.", "Undo") { repository.undo(post.id, previous) }
                    },
                    modifier = Modifier.riseIn(4),
                )

                // Optional widgets the person added in "Customize Home".
                Column(Modifier.animateContentSize(Motion.soft()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val remove: (HomeWidget) -> Unit = { w ->
                        setWidget(w, false)
                        showSnack("Removed. Add it back in Customize", "Undo") { setWidget(w, true) }
                    }
                    ReportWidget(visible = HomeWidget.Report in enabled, onRemove = { remove(HomeWidget.Report) }, onOpen = { navigator.open(Destination.Report) })
                    FollowersWidget(visible = HomeWidget.Followers in enabled, onRemove = { remove(HomeWidget.Followers) })
                    ShortcutsWidget(
                        visible = HomeWidget.Shortcuts in enabled,
                        onRemove = { remove(HomeWidget.Shortcuts) },
                        onJot = { sheetName = HomeSheet.Jot.name },
                        onVoice = { navigator.open(Destination.YourVoice) },
                        onStore = { navigator.open(Destination.Settings) },
                    )
                }

                TextButton(
                    onClick = { sheetName = HomeSheet.Customize.name },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(if (enabled.isEmpty()) "Customize Home" else "Customize Home · ${enabled.size} added")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    when (sheet) {
        HomeSheet.None -> Unit
        HomeSheet.Notifications -> NotificationsSheet(
            onDismiss = { sheetName = HomeSheet.None.name },
            onOpen = { dest ->
                sheetName = HomeSheet.None.name
                if (dest == Destination.Posts || dest == Destination.Inbox) navigator.switchTab(dest) else navigator.open(dest)
            },
        )
        HomeSheet.Customize -> CustomizeSheet(
            enabled = enabled,
            onToggle = { w, on ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                setWidget(w, on)
            },
            onDismiss = { sheetName = HomeSheet.None.name },
        )
        HomeSheet.Jot -> JotSheet(
            onDismiss = { sheetName = HomeSheet.None.name },
            onSave = {
                sheetName = HomeSheet.None.name
                showSnack("Saved for next check-in")
            },
        )
    }
}

/** "Good morning, Maya" style greeting from the local time. */
private fun greeting(): String {
    val first = SampleData.userName.substringBefore(' ')
    val h = LocalTime.now().hour
    val part = when (h) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    return "$part, $first"
}

/** Staggered entrance: rises in once when the screen first shows. */
@Composable
internal fun Modifier.riseIn(index: Int): Modifier {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, Motion.reveal(delay = 150 + index * Motion.Stagger)) }
    return this.graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 22.dp.toPx()
    }
}

// --- Time helpers for the "Up next" countdown (the sample world is Monday, Oct 20, 9:48 AM) ---

internal val dayOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
internal const val SampleNowMinutes = 9 * 60 + 48

/** Minutes since midnight for "9:10 AM". */
internal fun clockMinutes(time: String): Int {
    val (hm, ap) = time.trim().split(' ').let { it[0] to it.getOrElse(1) { "AM" } }
    val h = hm.substringBefore(':').toIntOrNull() ?: 0
    val m = hm.substringAfter(':', "0").toIntOrNull() ?: 0
    val h24 = (h % 12) + if (ap.equals("PM", true)) 12 else 0
    return h24 * 60 + m
}

internal fun minutesOfWeek(p: Post): Int = dayOrder.indexOf(p.day).coerceAtLeast(0) * 1440 + clockMinutes(p.time)

internal fun formatClock(minutes: Int): String {
    val m = ((minutes % 1440) + 1440) % 1440
    val h24 = m / 60
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    val ap = if (h24 < 12) "AM" else "PM"
    return "$h12:${(m % 60).toString().padStart(2, '0')} $ap"
}

internal fun shiftTime(time: String, deltaMinutes: Int): String = formatClock(clockMinutes(time) + deltaMinutes)
