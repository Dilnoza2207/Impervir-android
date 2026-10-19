package com.impervir.agent.feature.posts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.data.Platform
import com.impervir.agent.data.Post
import com.impervir.agent.data.PostStatus
import com.impervir.agent.data.SampleData
import com.impervir.agent.navigation.Destination
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class PostsTab { Needs, Scheduled, Posted }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsScreen(repository: ImpervirRepository, navigator: Navigator, contentPadding: PaddingValues) {
    val drafts by repository.drafts.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(PostsTab.Needs) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var headerMenu by remember { mutableStateOf(false) }
    var toneFor by rememberSaveable { mutableStateOf<String?>(null) }
    var timeFor by rememberSaveable { mutableStateOf<String?>(null) }
    var redoing by remember { mutableStateOf<String?>(null) }
    var goingNow by rememberSaveable { mutableStateOf(listOf<String>()) }
    var refreshing by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val needs = drafts.filter { it.status == PostStatus.NeedsApproval }
    val autoN = drafts.count { it.status == PostStatus.AutoPost }
    val skippedN = drafts.count { it.status == PostStatus.Skipped }
    val handledN = drafts.count { it.id in PostsData.askIds && it.status != PostStatus.NeedsApproval }

    fun notify(message: String, onUndo: (() -> Unit)? = null) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = message,
                actionLabel = if (onUndo != null) "Undo" else null,
                withDismissAction = onUndo == null,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onUndo?.invoke()
        }
    }

    val actions = DraftActions(
        approve = { p ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            repository.approve(p.id)
            notify("Approved · ${p.day} ${p.time}") { repository.undo(p.id, p.status) }
        },
        skip = { p ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            repository.skip(p.id)
            notify("Skipped for this week.") { repository.undo(p.id, p.status) }
        },
        edit = { p -> navigator.open(Destination.PostEditor(p.id)) },
        redo = { p ->
            scope.launch {
                redoing = p.id
                delay(1200)
                repository.updateBody(p.id, PostsData.redraft(p.id, p.body))
                redoing = null
                notify("Rewrote it. Same story, new words.")
            }
        },
        tone = { p -> toneFor = p.id },
        time = { p -> timeFor = p.id },
        copy = { p ->
            clipboard.setText(AnnotatedString(p.body))
            notify("Copied")
        },
        source = { p ->
            val src = p.source ?: return@DraftActions
            val quote = PostsData.quoteAt(src.timestamp)
            notify("From your voice note, ${src.timestamp}" + (quote?.let { ": “$it”" } ?: ""))
        },
        telegram = { notify("Will post on Telegram instead.") },
        postNow = { p ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            goingNow = goingNow + p.id
            notify("Posting to ${p.platform.label} now")
        },
        restore = { p -> repository.undo(p.id, if (p.platform == Platform.Telegram) PostStatus.AutoPost else PostStatus.NeedsApproval) },
    )

    Scaffold(
        modifier = Modifier
            .padding(contentPadding)
            .consumeWindowInsets(contentPadding)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeTopAppBar(
                title = { BlurRiseText("Your week, written.", style = LocalTextStyle.current) },
                actions = {
                    IconButton(onClick = { navigator.open(Destination.Calendar) }) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = "Open calendar")
                    }
                    IconButton(onClick = { headerMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = headerMenu, onDismissRequest = { headerMenu = false }) {
                        DropdownMenuItem(text = { Text("Approve all safe posts") }, onClick = {
                            headerMenu = false
                            notify("The $autoN Telegram posts already go out on their own. The ${needs.size} asks stay with you.")
                        })
                        DropdownMenuItem(text = { Text(if (paused) "Resume posting" else "Pause posting this week") }, onClick = {
                            headerMenu = false
                            paused = !paused
                            notify(if (paused) "Posting paused for Week 43" else "Posting resumed")
                        })
                        DropdownMenuItem(text = { Text("Posting rules") }, onClick = {
                            headerMenu = false
                            navigator.open(Destination.Settings)
                        })
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    delay(900)
                    refreshing = false
                    notify("Up to date. Nothing new from the agent.")
                }
            },
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    PostsHeader(
                        tab = tab,
                        onTab = { tab = it; haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                        needsN = needs.size,
                        scheduledN = drafts.size - skippedN,
                        autoN = autoN,
                        paused = paused,
                    )
                }
                when (tab) {
                    PostsTab.Needs -> {
                        item(key = "progress") {
                            HandledProgress(handled = handledN, total = PostsData.askIds.size, modifier = Modifier.animateItem())
                        }
                        if (needs.isEmpty()) {
                            item(key = "done") {
                                WeekDoneCard(
                                    scheduledN = drafts.size - skippedN,
                                    skippedN = skippedN,
                                    onCalendar = { navigator.open(Destination.Calendar) },
                                    onReviewAgain = {
                                        PostsData.askIds.forEach { repository.undo(it, PostStatus.NeedsApproval) }
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                        items(needs, key = { it.id }) { post ->
                            SwipeDraftCard(
                                post = post,
                                index = needs.indexOf(post),
                                redoing = redoing == post.id,
                                actions = actions,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    PostsTab.Scheduled -> scheduledItems(drafts, goingNow, actions)
                    PostsTab.Posted -> postedItems(SampleData.week42Posts) { navigator.open(Destination.PostDetail(it.id)) }
                }
            }
        }
    }

    toneFor?.let { id ->
        val post = drafts.firstOrNull { it.id == id }
        if (post != null) ToneSheet(onDismiss = { toneFor = null }, onPick = { label ->
            toneFor = null
            scope.launch {
                redoing = post.id
                delay(1200)
                repository.updateBody(post.id, PostsData.redraft(post.id, post.body))
                redoing = null
                notify("Tone: ${label.replaceFirstChar { it.lowercase() }}")
            }
        })
    }
    timeFor?.let { id ->
        val post = drafts.firstOrNull { it.id == id }
        if (post != null) TimeSheet(post = post, onDismiss = { timeFor = null }, onSave = { day, time ->
            timeFor = null
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val before = post.day to post.time
            repository.reschedule(post.id, day, time)
            notify("Scheduled for $day $time") { repository.reschedule(post.id, before.first, before.second) }
        })
    }
}

/** Every per-post action, hoisted so cards stay stateless. */
internal class DraftActions(
    val approve: (Post) -> Unit,
    val skip: (Post) -> Unit,
    val edit: (Post) -> Unit,
    val redo: (Post) -> Unit,
    val tone: (Post) -> Unit,
    val time: (Post) -> Unit,
    val copy: (Post) -> Unit,
    val source: (Post) -> Unit,
    val telegram: (Post) -> Unit,
    val postNow: (Post) -> Unit,
    val restore: (Post) -> Unit,
)

@Composable
private fun PostsHeader(tab: PostsTab, onTab: (PostsTab) -> Unit, needsN: Int, scheduledN: Int, autoN: Int, paused: Boolean) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)) {
        Text(
            PostsData.weekLabel,
            style = MaterialTheme.typography.labelMedium,
            color = ImpervirTheme.extras.muted,
        )
        Spacer(Modifier.height(6.dp))
        val subline = when (tab) {
            PostsTab.Needs -> if (needsN > 0) "$needsN need your okay. $autoN Telegram posts go out on their own."
            else "Nothing needs you. The $autoN Telegram posts go out on their own."
            PostsTab.Scheduled -> if (paused) "Posting is paused. Nothing goes out until you resume."
            else "Each post goes out at its platform's best time unless you move it."
            PostsTab.Posted -> "Week 42 is final. Customer stories made ${SampleData.customerStoriesShare}% of the money."
        }
        AnimatedContent(targetState = subline, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "subline") {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().semantics { heading() }) {
            val labels = listOf(
                PostsTab.Needs to "Needs you",
                PostsTab.Scheduled to "Scheduled",
                PostsTab.Posted to "Posted",
            )
            labels.forEachIndexed { i, (t, label) ->
                SegmentedButton(
                    selected = tab == t,
                    onClick = { onTab(t) },
                    shape = SegmentedButtonDefaults.itemShape(i, labels.size),
                    icon = {},
                    label = {
                        val count = when (t) {
                            PostsTab.Needs -> needsN
                            PostsTab.Scheduled -> scheduledN
                            PostsTab.Posted -> null
                        }
                        androidx.compose.foundation.layout.Row {
                            Text(label, maxLines = 1)
                            if (count != null) {
                                AnimatedContent(targetState = count, label = "count") { n ->
                                    Text(" $n", style = NumberStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize), color = ImpervirTheme.extras.accent)
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}
