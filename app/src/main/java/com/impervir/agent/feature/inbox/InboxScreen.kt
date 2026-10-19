package com.impervir.agent.feature.inbox

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.impervir.agent.data.Comment
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.data.Platform
import com.impervir.agent.navigation.Destination
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.components.AddPill
import com.impervir.agent.ui.components.Avatar
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.components.PlatformBadge
import com.impervir.agent.ui.ember.Ember
import com.impervir.agent.ui.ember.EmberMood
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FILTER_ALL = "all"
private const val FILTER_HOT = "hot"

/** Which week-42 post a comment sits under (for "Open post"). Feature-local mapping. */
private fun Comment.postId(): String = when (platform) {
    Platform.LinkedIn -> "p42-1"
    Platform.Threads -> "p42-2"
    Platform.X -> "p42-3"
    Platform.Telegram -> "p42-4"
}

/** "12m" -> 12, "2h" -> 120. */
private fun Comment.ageMinutes(): Int {
    val n = ago.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
    return if (ago.endsWith("h")) n * 60 else n
}

private fun Comment.agoLabel(): String = "$ago ago"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(repository: ImpervirRepository, navigator: Navigator, contentPadding: PaddingValues) {
    val comments by repository.comments.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // UI state (survives rotation)
    var searchOn by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    var platformsOn by rememberSaveable { mutableStateOf(false) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by rememberSaveable { mutableStateOf("") }
    var draftIndex by rememberSaveable { mutableIntStateOf(0) }
    var readIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    var headMenu by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }

    // Comments that are leaving (sent / done / muted) but can still be brought back with Undo.
    val leaving = remember { mutableStateListOf<String>() }

    val live = comments.filterNot { it.id in leaving }
    val isUnread: (Comment) -> Boolean = { it.firstHour && it.id !in readIds }
    val visible = live.filter { c ->
        val chipOk = when (filter) {
            FILTER_ALL -> true
            FILTER_HOT -> c.firstHour
            else -> c.platform.name == filter
        }
        val q = query.trim().lowercase()
        val textOk = q.isEmpty() || "${c.author} ${c.onPost} ${c.text}".lowercase().contains(q)
        chipOk && textOk
    }

    fun collapse() { expandedId = null; draft = ""; draftIndex = 0 }

    fun toggle(c: Comment) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (expandedId == c.id) collapse() else {
            expandedId = c.id
            draftIndex = 0
            draft = c.suggestions.firstOrNull().orEmpty()
            if (c.id !in readIds) readIds = readIds + c.id
        }
    }

    /** Hides the comments now; commits to the repository when the snackbar goes away without Undo. */
    fun removeWithUndo(ids: List<String>, message: String) {
        leaving.addAll(ids)
        if (expandedId in ids) collapse()
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            var undone = false
            try {
                val result = snackbar.showSnackbar(message, actionLabel = "Undo", duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) {
                    undone = true
                    leaving.removeAll(ids)
                }
            } finally {
                if (!undone) {
                    ids.forEach(repository::replied)
                    leaving.removeAll(ids)
                }
            }
        }
    }

    fun flash(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    BackHandler(enabled = expandedId != null || searchOn) {
        when {
            expandedId != null -> collapse()
            else -> { searchOn = false; query = "" }
        }
    }

    LaunchedEffect(refreshing) {
        if (refreshing) { delay(1100); refreshing = false }
    }

    Scaffold(
        modifier = Modifier
            .padding(bottom = contentPadding.calculateBottomPadding())
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeTopAppBar(
                title = { BlurRiseText("Inbox", style = LocalTextStyle.current, modifier = Modifier.semantics { heading() }) },
                actions = {
                    IconButton(onClick = {
                        searchOn = !searchOn
                        if (!searchOn) query = ""
                    }) {
                        Icon(if (searchOn) Icons.Rounded.SearchOff else Icons.Rounded.Search, contentDescription = if (searchOn) "Close search" else "Search comments")
                    }
                    Box {
                        IconButton(onClick = { headMenu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More inbox options") }
                        DropdownMenu(expanded = headMenu, onDismissRequest = { headMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Mark all as read") },
                                leadingIcon = { Icon(Icons.Rounded.DoneAll, null) },
                                onClick = {
                                    headMenu = false
                                    readIds = comments.map { it.id }
                                    flash("All marked as read")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Notification settings") },
                                leadingIcon = { Icon(Icons.Rounded.NotificationsNone, null) },
                                onClick = { headMenu = false; navigator.open(Destination.Settings) },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { refreshing = true },
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            val animated = remember { mutableStateListOf<String>() }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(PaddingValues(bottom = contentPadding.calculateBottomPadding()))
                    .imePadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "subtitle") {
                    val n = live.size
                    Text(
                        if (n > 0) "$n ${if (n == 1) "comment" else "comments"} waiting. Replies in the first hour do best."
                        else "Nothing waiting. New comments land here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ImpervirTheme.extras.muted,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                item(key = "search") {
                    SearchField(visible = searchOn, query = query, onQuery = { query = it }, onClear = { query = "" })
                }
                if (live.isNotEmpty()) {
                    item(key = "chips") {
                        FilterRows(
                            comments = live,
                            filter = filter,
                            onFilter = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                filter = it
                            },
                            platformsOn = platformsOn,
                            onPlatformsOn = { on ->
                                platformsOn = on
                                if (!on && filter != FILTER_ALL && filter != FILTER_HOT) filter = FILTER_ALL
                            },
                        )
                    }
                }
                when {
                    live.isEmpty() -> item(key = "done") { AllCaughtUp(onSeeSales = { navigator.switchTab(Destination.Analytics) }) }
                    visible.isEmpty() -> item(key = "nomatch") {
                        Text(
                            "Nothing matches that. Try another filter or clear the search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ImpervirTheme.extras.muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
                        )
                    }
                    else -> items(visible, key = { it.id }) { c ->
                        val index = visible.indexOf(c)
                        val firstShow = c.id !in animated
                        CommentCard(
                            comment = c,
                            unread = isUnread(c),
                            expanded = expandedId == c.id,
                            draft = if (expandedId == c.id) draft else "",
                            onDraft = { draft = it },
                            hasAnotherDraft = c.suggestions.size > 1,
                            onAnotherDraft = {
                                draftIndex = (draftIndex + 1) % c.suggestions.size
                                draft = c.suggestions[draftIndex]
                            },
                            onToggle = { toggle(c) },
                            onSend = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                removeWithUndo(listOf(c.id), "Reply sent on ${c.platform.label}")
                            },
                            onCopy = {
                                clipboard.setText(AnnotatedString(c.text))
                                flash("Comment copied")
                            },
                            onOpenPost = { navigator.open(Destination.PostDetail(c.postId())) },
                            onDone = { removeWithUndo(listOf(c.id), "Marked done") },
                            onMute = {
                                val ids = live.filter { it.author == c.author }.map { it.id }
                                removeWithUndo(ids, "Muted ${c.author}")
                            },
                            modifier = Modifier
                                .animateItem()
                                .staggerIn(if (firstShow) index.coerceAtMost(6) else -1) { animated.add(c.id) },
                        )
                    }
                }
            }
        }
    }
}

/** Fades and lifts an item in once; [order] < 0 skips the animation. */
@Composable
private fun Modifier.staggerIn(order: Int, onDone: () -> Unit): Modifier {
    val p = remember { Animatable(if (order < 0) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (order >= 0) {
            delay(order * Motion.Stagger.toLong())
            p.animateTo(1f, tween(520, easing = Motion.Emphasized))
            onDone()
        }
    }
    return this.graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 18.dp.toPx()
    }
}

@Composable
private fun SearchField(visible: Boolean, query: String, onQuery: (String) -> Unit, onClear: () -> Unit) {
    val focus = remember { FocusRequester() }
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(260)) + fadeIn(tween(260)),
        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
    ) {
        LaunchedEffect(Unit) { focus.requestFocus() }
        TextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text("Search comments") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, contentDescription = "Clear search") }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).focusRequester(focus),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRows(
    comments: List<Comment>,
    filter: String,
    onFilter: (String) -> Unit,
    platformsOn: Boolean,
    onPlatformsOn: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountChip("All", comments.size, filter == FILTER_ALL) { onFilter(FILTER_ALL) }
            CountChip("First hour", comments.count { it.firstHour }, filter == FILTER_HOT) { onFilter(FILTER_HOT) }
            if (!platformsOn) AddPill("Filter by platform", onClick = { onPlatformsOn(true) })
        }
        AnimatedVisibility(
            visible = platformsOn,
            enter = expandVertically(tween(280)) + fadeIn(tween(280)),
            exit = shrinkVertically(tween(220)) + fadeOut(tween(160)),
        ) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(Platform.LinkedIn, Platform.Threads, Platform.X).forEach { p ->
                    FilterChip(
                        selected = filter == p.name,
                        onClick = { onFilter(if (filter == p.name) FILTER_ALL else p.name) },
                        label = { Text(p.label) },
                        leadingIcon = { PlatformBadge(p, size = 20.dp) },
                        trailingIcon = { Text("${comments.count { it.platform == p }}", style = NumberStyle.copy(fontSize = 13.sp)) },
                        shape = RoundedCornerShape(50),
                    )
                }
                IconButton(onClick = { onPlatformsOn(false) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove platform filter", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) ({ Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }) else null,
        trailingIcon = { Text("$count", style = NumberStyle.copy(fontSize = 13.sp)) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            selectedLabelColor = MaterialTheme.colorScheme.inverseOnSurface,
            selectedLeadingIconColor = MaterialTheme.colorScheme.inverseOnSurface,
            selectedTrailingIconColor = MaterialTheme.colorScheme.inverseOnSurface,
        ),
    )
}

@Composable
private fun CommentCard(
    comment: Comment,
    unread: Boolean,
    expanded: Boolean,
    draft: String,
    onDraft: (String) -> Unit,
    hasAnotherDraft: Boolean,
    onAnotherDraft: () -> Unit,
    onToggle: () -> Unit,
    onSend: () -> Unit,
    onCopy: () -> Unit,
    onOpenPost: () -> Unit,
    onDone: () -> Unit,
    onMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = ImpervirTheme.extras
    var menu by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        when {
            expanded -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
            comment.firstHour -> extras.accent.copy(alpha = 0.45f)
            else -> androidx.compose.ui.graphics.Color.Transparent
        },
        label = "border",
    )
    Card(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth().semantics {
            contentDescription = "${comment.author} on ${comment.platform.label}, ${comment.agoLabel()}: ${comment.text}. " +
                if (expanded) "Tap to close." else "Tap to reply."
        },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = extras.card),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(Modifier.padding(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 16.dp).animateContentSize(Motion.soft())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp)) {
                    Avatar(comment.initials, size = 40.dp)
                    PlatformBadge(comment.platform, size = 20.dp, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp))
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            comment.author,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        if (unread) {
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.size(8.dp).background(extras.accent, CircleShape).semantics { contentDescription = "Unread" })
                        }
                    }
                    Text(
                        "on: ${comment.onPost} · ${comment.agoLabel()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.muted,
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More for ${comment.author}'s comment") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Copy comment") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = { menu = false; onCopy() })
                        DropdownMenuItem(text = { Text("Open post") }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) }, onClick = { menu = false; onOpenPost() })
                        DropdownMenuItem(text = { Text("Mark done") }, leadingIcon = { Icon(Icons.Rounded.Check, null) }, onClick = { menu = false; onDone() })
                        DropdownMenuItem(text = { Text("Mute this person") }, leadingIcon = { Icon(Icons.Rounded.VolumeOff, null) }, onClick = { menu = false; onMute() })
                    }
                }
            }
            if (comment.firstHour) {
                FirstHourPill(comment.ageMinutes(), Modifier.padding(top = 10.dp))
            }
            Text(
                comment.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 10.dp, end = 12.dp),
            )
            if (expanded) {
                ReplyBox(
                    comment = comment,
                    draft = draft,
                    onDraft = onDraft,
                    hasAnotherDraft = hasAnotherDraft,
                    onAnotherDraft = onAnotherDraft,
                    onSend = onSend,
                )
            }
        }
    }
}

/** "First hour · 47:12 left": replies in the first hour do best, so the clock is visible. */
@Composable
private fun FirstHourPill(ageMinutes: Int, modifier: Modifier = Modifier) {
    var elapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); elapsed++ } }
    val remaining = (3600 - ageMinutes * 60 - elapsed).coerceAtLeast(0)
    val label = if (remaining > 0) "%02d:%02d left".format(remaining / 60, remaining % 60) else "Hour's up"
    Row(
        modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(ImpervirTheme.extras.accent, CircleShape))
        Text("First hour", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(start = 6.dp))
        Text(" · $label", style = NumberStyle.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun ReplyBox(
    comment: Comment,
    draft: String,
    onDraft: (String) -> Unit,
    hasAnotherDraft: Boolean,
    onAnotherDraft: () -> Unit,
    onSend: () -> Unit,
) {
    val extras = ImpervirTheme.extras
    Column(Modifier.padding(top = 14.dp, end = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = extras.accent, modifier = Modifier.size(14.dp))
            Text(
                "Suggested by your agent, in your voice",
                style = MaterialTheme.typography.labelMedium,
                color = extras.muted,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraft,
            minLines = 2,
            label = { Text("Reply to ${comment.author.substringBefore(' ')}") },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = extras.accent),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (hasAnotherDraft) AddPill("Another draft", onClick = onAnotherDraft)
            Spacer(Modifier.weight(1f))
            Button(onClick = onSend, enabled = draft.isNotBlank(), modifier = Modifier.height(44.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Send", modifier = Modifier.padding(start = 8.dp))
            }
        }
        Text(
            "You send it. I never reply for you.",
            style = MaterialTheme.typography.bodySmall,
            color = extras.muted,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun AllCaughtUp(onSeeSales: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Ember(EmberMood.Happy, size = 140.dp)
        Text(
            "All caught up.",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp).semantics { heading() },
        )
        Text(
            "Every comment has an answer or is muted. New ones land here.",
            style = MaterialTheme.typography.bodyMedium,
            color = ImpervirTheme.extras.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        FilledTonalButton(onClick = onSeeSales, modifier = Modifier.padding(top = 20.dp)) { Text("See which posts sold") }
    }
}
