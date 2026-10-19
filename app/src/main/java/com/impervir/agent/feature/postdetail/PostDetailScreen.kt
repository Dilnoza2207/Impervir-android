package com.impervir.agent.feature.postdetail

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.data.Post
import com.impervir.agent.navigation.Destination
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.components.AddPill
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.components.PlatformBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(repository: ImpervirRepository, navigator: Navigator, postId: String) {
    val post = remember(postId) { repository.post(postId) }
    if (post == null) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text("Post") },
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
            )
        }) { inner ->
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) { Text("This post is no longer in your history.") }
        }
        return
    }
    DetailContent(repository, navigator, post)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(repository: ImpervirRepository, navigator: Navigator, post: Post) {
    val comments by repository.comments.collectAsStateWithLifecycle()
    val waiting = comments.filter { c ->
        c.platform == post.platform && (post.title.startsWith(c.onPost, ignoreCase = true) || post.title.contains(c.onPost, ignoreCase = true))
    }

    var menu by remember { mutableStateOf(false) }
    var pinned by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var showStats by rememberSaveable { mutableStateOf(false) }
    var showChart by rememberSaveable { mutableStateOf(false) }
    var showOrders by rememberSaveable { mutableStateOf(false) }
    var showAudience by rememberSaveable { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val uri = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val stats = post.stats

    fun notify(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message, withDismissAction = true)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeTopAppBar(
                title = { BlurRiseText(DetailData.headline(post), style = LocalTextStyle.current) },
                navigationIcon = {
                    IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Reshare to Telegram") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Send, null) },
                            onClick = { menu = false; notify("Reshare goes out on Telegram today at 6 PM") },
                        )
                        DropdownMenuItem(
                            text = { Text(if (pinned) "Unpin voice example" else "Pin as voice example") },
                            leadingIcon = { Icon(Icons.Rounded.PushPin, null) },
                            onClick = {
                                menu = false
                                pinned = !pinned
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                notify(if (pinned) "Pinned as a voice example" else "Unpinned")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Copy post") },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                            onClick = {
                                menu = false
                                clipboard.setText(AnnotatedString(DetailData.segments[post.id]?.joinToString("") { it.first } ?: post.body))
                                notify("Post copied")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Copy tracking link") },
                            leadingIcon = { Icon(Icons.Rounded.Link, null) },
                            onClick = {
                                menu = false
                                clipboard.setText(AnnotatedString("https://impervir.link/t/${post.id}"))
                                notify("Tracking link copied")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Open on ${post.platform.label}") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) },
                            onClick = {
                                menu = false
                                val url = when (post.platform) {
                                    com.impervir.agent.data.Platform.LinkedIn -> "https://www.linkedin.com/"
                                    com.impervir.agent.data.Platform.X -> "https://x.com/"
                                    com.impervir.agent.data.Platform.Threads -> "https://www.threads.net/"
                                    com.impervir.agent.data.Platform.Telegram -> "https://t.me/"
                                }
                                runCatching { uri.openUri(url) }.onFailure { notify("Opening on ${post.platform.label}") }
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete from history", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; confirmDelete = true },
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    navigator.switchTab(Destination.Talk)
                },
                icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                text = { Text("Make a similar post") },
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 96.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformBadge(post.platform, size = 36.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Week 42 · ${post.platform.label}", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Posted ${post.day}, ${DetailData.dates[post.day]} · ${post.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DetailData.quote(post)?.let {
                Text(
                    "“$it”",
                    style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PostTextCard(post)
            if (stats != null) {
                RevenueCard(post, stats)
                MoneyTrail(post, stats)
            }
            if (waiting.isNotEmpty()) {
                CommentsCard(waiting, onOpen = { navigator.switchTab(Destination.Inbox) })
            }

            if (stats != null) {
                AllStatsBlock(showStats, stats, onRemove = { showStats = false })
                ChartBlock(showChart, stats, onRemove = { showChart = false })
                OrdersBlock(showOrders, post, stats, onRemove = { showOrders = false })
            }
            AudienceBlock(showAudience, onRemove = { showAudience = false })

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stats != null) {
                    if (!showStats) AddPill("All stats", onClick = { showStats = true })
                    if (!showChart) AddPill("Views chart", onClick = { showChart = true })
                    if (!showOrders) AddPill("Orders list", onClick = { showOrders = true })
                }
                if (!showAudience) AddPill("Who engaged", onClick = { showAudience = true })
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text("Delete from history?") },
            text = { Text("The post stays on ${post.platform.label}. Impervir forgets its stats and stops crediting sales to it.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    notify("Removed from history")
                    scope.launch {
                        delay(900)
                        navigator.back()
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
