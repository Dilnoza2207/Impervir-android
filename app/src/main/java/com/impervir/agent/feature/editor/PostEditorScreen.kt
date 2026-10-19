package com.impervir.agent.feature.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.data.Platform
import com.impervir.agent.data.Post
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.components.AddPill
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.components.PlatformBadge
import com.impervir.agent.ui.theme.ImpervirTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val textsSaver = listSaver<SnapshotStateMap<Platform, String>, String>(
    save = { m -> m.flatMap { listOf(it.key.name, it.value) } },
    restore = { l -> mutableStateMapOf<Platform, String>().apply { l.chunked(2).forEach { put(Platform.valueOf(it[0]), it[1]) } } },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostEditorScreen(repository: ImpervirRepository, navigator: Navigator, postId: String) {
    val post = remember(postId) { repository.post(postId) }
    if (post == null) {
        MissingPost(onBack = navigator::back)
        return
    }
    EditorContent(repository, navigator, post)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditorContent(repository: ImpervirRepository, navigator: Navigator, post: Post) {
    val platforms = if (post.platform == Platform.Telegram) listOf(Platform.Telegram) else listOf(Platform.LinkedIn, Platform.X, Platform.Threads)
    val initialTexts = remember(post.id) { platforms.associateWith { if (it == post.platform) post.body else EditorData.crossPost.getValue(it) } }

    val texts = rememberSaveable(post.id, saver = textsSaver) { mutableStateMapOf<Platform, String>().apply { putAll(initialTexts) } }
    var saved by remember(post.id) { mutableStateOf(initialTexts) }
    var platform by rememberSaveable { mutableStateOf(post.platform) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var day by rememberSaveable { mutableStateOf(post.day) }
    var time by rememberSaveable { mutableStateOf(post.time) }
    var savedWhen by remember(post.id) { mutableStateOf(post.day to post.time) }
    var savedExtras by remember(post.id) { mutableStateOf(listOf("", "", "")) }
    var showTools by rememberSaveable { mutableStateOf(false) }
    var showLink by rememberSaveable { mutableStateOf(false) }
    var showComment by rememberSaveable { mutableStateOf(false) }
    var showTags by rememberSaveable { mutableStateOf(false) }
    var link by rememberSaveable { mutableStateOf("") }
    var comment by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var toolId by rememberSaveable { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(-1) }
    var timeDialog by rememberSaveable { mutableStateOf(false) }
    var leaveDialog by rememberSaveable { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val text = texts[platform].orEmpty()
    val rules = EditorData.rules.getValue(platform)
    val dirty = texts.toMap() != saved || (day to time) != savedWhen || listOf(link, comment, tags) != savedExtras

    fun notify(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message, withDismissAction = true)
        }
    }

    fun save() {
        repository.updateBody(post.id, texts[post.platform].orEmpty())
        repository.reschedule(post.id, day, time)
        saved = texts.toMap()
        savedWhen = day to time
        savedExtras = listOf(link, comment, tags)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun approve() {
        if (leaving) return
        val edited = texts[post.platform] != post.body
        save()
        repository.approve(post.id)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        leaving = true
        notify(if (edited) "Scheduled. Your edit taught me 1 new rule." else "Approved · $day $time")
        scope.launch {
            delay(1100)
            navigator.back()
        }
    }

    fun tryLeave() {
        if (dirty && !leaving) leaveDialog = true else navigator.back()
    }

    BackHandler(enabled = dirty && !leaving) { leaveDialog = true }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeTopAppBar(
                title = { BlurRiseText("Edit post", style = LocalTextStyle.current) },
                navigationIcon = {
                    IconButton(onClick = ::tryLeave) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Posts")
                    }
                },
                actions = {
                    TextButton(onClick = { save(); notify("Saved") }, enabled = dirty) { Text("Save") }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            EditorBottomBar(
                whenLabel = "$day $time",
                isBest = "$day $time" == rules.best,
                onTime = { timeDialog = true },
                onApprove = ::approve,
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Week 43 · ${post.title.substringBefore(":")}",
                style = MaterialTheme.typography.labelMedium,
                color = ImpervirTheme.extras.muted,
            )

            if (platforms.size > 1) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    platforms.forEachIndexed { i, p ->
                        SegmentedButton(
                            selected = p == platform,
                            onClick = {
                                platform = p
                                toolId = null
                                playing = -1
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            shape = SegmentedButtonDefaults.itemShape(i, platforms.size),
                            icon = { PlatformBadge(p, size = 20.dp) },
                            label = { Text(p.label, maxLines = 1) },
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(false to "Edit", true to "Preview").forEachIndexed { i, (isPreview, label) ->
                    SegmentedButton(
                        selected = preview == isPreview,
                        onClick = { preview = isPreview; if (isPreview) toolId = null },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        label = { Text(label) },
                    )
                }
            }

            val cliche = EditorData.cliches.firstOrNull { text.contains(it.first, ignoreCase = true) }
            AnimatedVisibility(visible = cliche != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                val c = cliche ?: EditorData.cliches.first()
                ClicheWarning(
                    phrase = c.first,
                    rule = c.third,
                    onFix = {
                        texts[platform] = text.replace(Regex(Regex.escape(c.first), RegexOption.IGNORE_CASE), c.second)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        notify("Swapped in: ${c.second}")
                    },
                )
            }

            AnimatedContent(
                targetState = preview,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "mode",
                modifier = Modifier.animateContentSize(),
            ) { isPreview ->
                if (isPreview) {
                    PreviewCard(text = text, whenLabel = "$day $time", platform = platform)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PostTextField(text = text, limit = rules.limit, onChange = { texts[platform] = it; toolId = null })
                        VoiceSourceChips(playing = playing, onToggle = { i ->
                            playing = if (playing == i) -1 else i
                            if (playing == i) scope.launch {
                                delay(2600)
                                if (playing == i) playing = -1
                            }
                        })
                    }
                }
            }

            if (!preview) {
                AskAgentBlock(
                    visible = showTools,
                    text = text,
                    selected = toolId,
                    onPick = { toolId = if (toolId == it) null else it },
                    onAccept = { from, to ->
                        if (text.contains(from)) {
                            texts[platform] = text.replaceFirst(from, to)
                            notify("Suggestion applied")
                        } else notify("That text has changed")
                        toolId = null
                    },
                    onReject = { toolId = null },
                    onRemove = { showTools = false; toolId = null },
                )
                ExtraField(
                    visible = showLink, title = "Link", value = link, onValue = { link = it },
                    placeholder = "Paste a link. Sales get credited to this post.",
                    note = if (platform == Platform.LinkedIn) "LinkedIn rarely shows posts with links. A first comment works better." else "Each click is tracked, so sales get credited here.",
                    onRemove = { showLink = false; link = "" },
                )
                ExtraField(
                    visible = showComment, title = "First comment", value = comment, onValue = { comment = it },
                    placeholder = "Goes up right after the post, from your account.", note = null, singleLine = false,
                    onRemove = { showComment = false; comment = "" },
                )
                ExtraField(
                    visible = showTags, title = "Hashtags", value = tags, onValue = { tags = it },
                    placeholder = "#notion #planning", note = "Two or three is plenty. More reads like a bot.",
                    onRemove = { showTags = false; tags = "" },
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!showTools) AddPill("Ask the agent", onClick = { showTools = true })
                    if (!showLink) AddPill("Add a link", onClick = { showLink = true })
                    if (!showComment) AddPill("Add first comment", onClick = { showComment = true })
                    if (!showTags) AddPill("Add hashtags", onClick = { showTags = true })
                }
            } else {
                Text(
                    "This is how it will look once it posts. Nothing is live yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (timeDialog) {
        TimePickDialog(
            platform = platform,
            day = day,
            time = time,
            onDismiss = { timeDialog = false },
            onPick = { d, t ->
                timeDialog = false
                day = d
                time = t
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                notify("Moved to $d $t")
            },
        )
    }

    if (leaveDialog) {
        AlertDialog(
            onDismissRequest = { leaveDialog = false },
            title = { Text("Leave without saving?") },
            text = { Text("Your edits to this post haven't been saved yet.") },
            confirmButton = {
                TextButton(onClick = {
                    leaveDialog = false
                    save()
                    navigator.back()
                }) { Text("Save and leave") }
            },
            dismissButton = {
                TextButton(onClick = {
                    leaveDialog = false
                    leaving = true
                    navigator.back()
                }) { Text("Discard") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissingPost(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Edit post") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
            Text("This post is gone. It may have been skipped or deleted.", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
