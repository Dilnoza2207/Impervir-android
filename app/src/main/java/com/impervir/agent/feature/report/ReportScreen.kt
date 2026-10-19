package com.impervir.agent.feature.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.data.SampleData
import com.impervir.agent.navigation.Destination
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.theme.ImmersiveDark
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.launch

private const val CARD_MS = 4500f

@Composable
fun ReportScreen(repository: ImpervirRepository, navigator: Navigator) {
    ImmersiveDark { ReportStories(navigator) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportStories(navigator: Navigator) {
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var extra by rememberSaveable { mutableStateOf(listOf<Int>()) } // optional card ids that are on
    var paused by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var customizeOpen by rememberSaveable { mutableStateOf(false) }
    var held by remember { mutableStateOf(false) }

    val order = remember(extra) { listOf(0, 1) + extra.sorted() + 5 }
    val pager = rememberPagerState(pageCount = { order.size })
    var currentId by rememberSaveable { mutableStateOf(0) }
    val progress = remember { mutableFloatStateOf(0f) }

    // Keep the same card in view when cards are added or removed.
    LaunchedEffect(order) {
        val target = order.indexOf(currentId).takeIf { it >= 0 } ?: order.indexOfLast { it < currentId }.coerceAtLeast(0)
        if (target != pager.currentPage) pager.scrollToPage(target)
    }
    LaunchedEffect(pager.currentPage) {
        currentId = order.getOrElse(pager.currentPage) { 0 }
        progress.floatValue = 0f
    }

    val isLast = pager.currentPage == order.lastIndex
    val running by remember {
        derivedStateOf { !paused && !held && !menuOpen && !customizeOpen && !pager.isScrollInProgress }
    }

    // Auto-advance: fill the current segment, then move on. The last card stays.
    LaunchedEffect(pager.currentPage, running, order.size) {
        if (!running) return@LaunchedEffect
        if (isLast) { progress.floatValue = 1f; return@LaunchedEffect }
        var last = withFrameMillis { it }
        while (progress.floatValue < 1f) {
            val now = withFrameMillis { it }
            progress.floatValue = (progress.floatValue + (now - last) / CARD_MS).coerceAtMost(1f)
            last = now
        }
        pager.animateScrollToPage(pager.currentPage + 1)
    }

    fun go(delta: Int) {
        val target = (pager.currentPage + delta).coerceIn(0, order.lastIndex)
        if (target == pager.currentPage) {
            if (delta < 0) progress.floatValue = 0f
            return
        }
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch { pager.animateScrollToPage(target) }
    }

    fun flash(msg: String) = scope.launch {
        snackbar.currentSnackbarData?.dismiss()
        snackbar.showSnackbar(msg)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ReportBackdrop(card = currentId)

        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(order.size) {
                    detectTapGestures(
                        onPress = {
                            held = true
                            tryAwaitRelease()
                            held = false
                        },
                        onTap = { pos -> go(if (pos.x < size.width / 3f) -1 else 1) },
                    )
                },
            key = { order[it] },
        ) { page ->
            val id = order[page]
            ReportCard(
                id = id,
                active = page == pager.currentPage,
                onCheckIn = { navigator.switchTab(Destination.Talk) },
                onAnalytics = { navigator.switchTab(Destination.Analytics) },
            )
        }

        // Top chrome: progress segments, title, menu, close.
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .semantics { contentDescription = "Card ${pager.currentPage + 1} of ${order.size}" },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                order.indices.forEach { i ->
                    val fill = when {
                        i < pager.currentPage -> 1f
                        i == pager.currentPage -> progress.floatValue
                        else -> 0f
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.22f)),
                    ) {
                        Box(Modifier.fillMaxWidth(fill).height(3.dp).background(Color.White))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text("Week 42 report", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                    Text(SampleData.lastWeekRange, style = NumberStyle.copy(fontSize = 12.sp), color = ImpervirTheme.extras.muted)
                }
                AnimatedVisibility(visible = paused || held, enter = fadeIn(), exit = fadeOut()) {
                    Row(
                        Modifier
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Paused", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreHoriz, contentDescription = "Report options") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Share as image") }, leadingIcon = { Icon(Icons.Rounded.Image, null) }, onClick = {
                            menuOpen = false; flash("Week 42 saved as an image")
                        })
                        DropdownMenuItem(text = { Text("Copy summary") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = {
                            menuOpen = false
                            clipboard.setText(AnnotatedString(reportSummary))
                            flash("Summary copied")
                        })
                        DropdownMenuItem(
                            text = { Text(if (paused) "Play report" else "Pause report") },
                            leadingIcon = { Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null) },
                            onClick = { menuOpen = false; paused = !paused },
                        )
                        DropdownMenuItem(text = { Text("Customize") }, leadingIcon = { Icon(Icons.Rounded.Tune, null) }, onClick = {
                            menuOpen = false; customizeOpen = true
                        })
                    }
                }
                IconButton(onClick = { navigator.back() }) { Icon(Icons.Rounded.Close, contentDescription = "Close report") }
            }
        }

        // Bottom: quiet Customize entry, and snackbars.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnackbarHost(snackbar)
            if (!isLast) {
                val off = optionalCards.size - extra.size
                TextButton(onClick = { customizeOpen = true }) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.8f))
                    Text(
                        if (off > 0) "Customize · $off more ${if (off == 1) "card" else "cards"} you can add" else "Customize · All cards on",
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }

    if (customizeOpen) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { customizeOpen = false }, sheetState = state) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
                Text("Customize", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() })
                Text(
                    "Your money, your best post and next week always show. Add the cards you want.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ImpervirTheme.extras.muted,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                )
                optionalCards.forEach { (id, label, sub) ->
                    val on = id in extra
                    ListItem(
                        headlineContent = { Text(label, style = MaterialTheme.typography.titleSmall) },
                        supportingContent = { Text(sub) },
                        trailingContent = { Switch(checked = on, onCheckedChange = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                extra = if (on) extra - id else extra + id
                            }
                            .padding(horizontal = 8.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { scope.launch { state.hide() }.invokeOnCompletion { customizeOpen = false } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).height(52.dp),
                ) { Text("Done") }
            }
        }
    }
}
