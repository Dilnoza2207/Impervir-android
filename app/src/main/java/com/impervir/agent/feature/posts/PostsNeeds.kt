package com.impervir.agent.feature.posts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.impervir.agent.data.Check
import com.impervir.agent.data.Platform
import com.impervir.agent.data.Post
import com.impervir.agent.ui.components.ImpervirCard
import com.impervir.agent.ui.components.PlatformBadge
import com.impervir.agent.ui.ember.Ember
import com.impervir.agent.ui.ember.EmberMood
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.NumberStyle

/** Three segments that fill as the asks get handled. */
@Composable
internal fun HandledProgress(handled: Int, total: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "$handled of $total handled" }) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                val color by animateColorAsState(
                    if (i < handled) ImpervirTheme.extras.accent else MaterialTheme.colorScheme.surfaceContainerHighest,
                    label = "bar",
                )
                Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(color))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row {
            Text("$handled", style = NumberStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(" of ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$total", style = NumberStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(" handled · swipe right to approve, left to skip", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun WeekDoneCard(scheduledN: Int, skippedN: Int, onCalendar: () -> Unit, onReviewAgain: () -> Unit, modifier: Modifier = Modifier) {
    ImpervirCard(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Ember(EmberMood.Proud, size = 96.dp)
            Spacer(Modifier.height(12.dp))
            Text("That's the week.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "$scheduledN posts are scheduled at each platform's best time" +
                    (if (skippedN > 0) " and $skippedN skipped" else "") +
                    ". You'll get the sales report on Sunday.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCalendar) { Text("See the calendar") }
                TextButton(onClick = onReviewAgain) { Text("Review again") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun SwipeDraftCard(post: Post, index: Int, redoing: Boolean, actions: DraftActions, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val state = rememberSwipeToDismissBoxState(positionalThreshold = { it * 0.35f })
    var menu by remember { mutableStateOf(false) }
    var checksOpen by rememberSaveable(post.id) { mutableStateOf(false) }

    // A light tick when the swipe crosses the threshold, the real action once it settles.
    LaunchedEffect(state.targetValue) {
        if (state.targetValue != SwipeToDismissBoxValue.Settled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    LaunchedEffect(state.currentValue) {
        when (state.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> actions.approve(post)
            SwipeToDismissBoxValue.EndToStart -> actions.skip(post)
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    SwipeToDismissBox(
        state = state,
        modifier = modifier.staggerIn(index),
        backgroundContent = { SwipeBackground(state.dismissDirection, state.progress) },
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = ImpervirTheme.extras.card),
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .combinedClickable(
                    onClickLabel = "Edit",
                    onLongClickLabel = "More for this post",
                    onClick = { actions.edit(post) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu = true
                    },
                )
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Approve") { actions.approve(post); true },
                        CustomAccessibilityAction("Skip this week") { actions.skip(post); true },
                    )
                },
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformBadge(post.platform, size = 36.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(post.platform.label, style = MaterialTheme.typography.titleSmall)
                        val best = PostsData.bestTime[post.platform] == post.time
                        Text(
                            "${post.day} ${post.time}" + if (best) " · best time" else "",
                            style = NumberStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Rounded.MoreHoriz, contentDescription = "More for this post")
                        }
                        DraftMenu(post, expanded = menu, onDismiss = { menu = false }, actions = actions)
                    }
                }
                Spacer(Modifier.height(14.dp))
                AnimatedContent(
                    targetState = if (redoing) null else post.body,
                    transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(160)) },
                    label = "body",
                ) { body ->
                    if (body == null) RewritingLines() else Text(body, style = MaterialTheme.typography.bodyLarge)
                }
                post.source?.let { src ->
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Mic, contentDescription = null, tint = ImpervirTheme.extras.accent, modifier = Modifier.size(16.dp))
                        Text(
                            " ${src.label} ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(src.timestamp, style = NumberStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                ChecksRow(post.checksPassed, open = checksOpen, onToggle = { checksOpen = !checksOpen })
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { actions.edit(post) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Edit", modifier = Modifier.padding(start = 8.dp))
                    }
                    Button(onClick = { actions.approve(post) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Approve", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue, progress: Float) {
    val approve = direction == SwipeToDismissBoxValue.StartToEnd
    val skip = direction == SwipeToDismissBoxValue.EndToStart
    val bg = when {
        approve -> ImpervirTheme.extras.accent
        skip -> MaterialTheme.colorScheme.inverseSurface
        else -> Color.Transparent
    }
    val fg = if (approve) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.inverseOnSurface
    val scale by animateFloatAsState(if (progress > 0.35f) 1.15f else 0.9f, label = "iconScale")
    Box(
        Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large)
            .background(bg)
            .padding(horizontal = 28.dp),
        contentAlignment = if (approve) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        if (approve || skip) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (skip) Text("Skip", color = fg, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 10.dp))
                Icon(
                    if (approve) Icons.Rounded.Check else Icons.Rounded.SkipNext,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(28.dp).graphicsLayer { scaleX = scale; scaleY = scale },
                )
                if (approve) Text("Approve", color = fg, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}

@Composable
private fun RewritingLines() {
    val t = rememberInfiniteTransition(label = "rewrite")
    val a by t.animateFloat(0.35f, 0.9f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "a")
    Column(
        Modifier.fillMaxWidth().semantics { contentDescription = "Rewriting this post" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(1f, 0.92f, 0.6f, 0f, 0.95f, 0.8f, 0.4f).forEach { w ->
            if (w == 0f) Spacer(Modifier.height(4.dp))
            else Box(
                Modifier.fillMaxWidth(w).height(12.dp).clip(CircleShape)
                    .background(ImpervirTheme.extras.accent.copy(alpha = 0.18f * a + 0.05f)),
            )
        }
    }
}

@Composable
private fun ChecksRow(passed: Int, open: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chev")
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = if (open) "Hide checks" else "Show checks", onClick = onToggle)
                .heightIn(min = 48.dp)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Verified, contentDescription = null, tint = ImpervirTheme.extras.positive, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("$passed of 5", style = NumberStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize))
            Text(" checks", style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp).rotate(rotation))
        }
        AnimatedVisibility(visible = open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                Check.entries.forEach { c ->
                    val ok = c != Check.YouApprove || passed >= 5
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = if (ok) "Passed" else "Waiting",
                            tint = if (ok) ImpervirTheme.extras.positive else ImpervirTheme.extras.accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(c.label, style = MaterialTheme.typography.labelLarge)
                            Text(PostsData.checkDetail[c].orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DraftMenu(post: Post, expanded: Boolean, onDismiss: () -> Unit, actions: DraftActions) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Redo this draft") }, leadingIcon = { Icon(Icons.Rounded.Refresh, null) }, onClick = { onDismiss(); actions.redo(post) })
        DropdownMenuItem(text = { Text("Change the tone") }, leadingIcon = { Icon(Icons.Rounded.Tune, null) }, onClick = { onDismiss(); actions.tone(post) })
        DropdownMenuItem(text = { Text("Change the time") }, leadingIcon = { Icon(Icons.Rounded.AccessTime, null) }, onClick = { onDismiss(); actions.time(post) })
        DropdownMenuItem(text = { Text("Copy text") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = { onDismiss(); actions.copy(post) })
        if (post.source != null) {
            DropdownMenuItem(text = { Text("Show the voice-note source") }, leadingIcon = { Icon(Icons.Rounded.GraphicEq, null) }, onClick = { onDismiss(); actions.source(post) })
        }
        if (post.platform != Platform.Telegram) {
            DropdownMenuItem(text = { Text("Post to Telegram instead") }, leadingIcon = { Icon(Icons.Rounded.Send, null) }, onClick = { onDismiss(); actions.telegram(post) })
        }
        HorizontalDivider()
        DropdownMenuItem(text = { Text("Skip this week") }, leadingIcon = { Icon(Icons.Rounded.SkipNext, null) }, onClick = { onDismiss(); actions.skip(post) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToneSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("Tone", style = MaterialTheme.typography.headlineSmall)
            Text("Redraft this post. Your voice stays.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            PostsData.tones.forEach { tone ->
                ListItem(
                    headlineContent = { Text(tone) },
                    trailingContent = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = ImpervirTheme.extras.accent) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { onPick(tone) },
                )
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TimeSheet(post: Post, onDismiss: () -> Unit, onSave: (day: String, time: String) -> Unit) {
    var day by rememberSaveable { mutableStateOf(post.day) }
    var time by rememberSaveable { mutableStateOf(post.time) }
    val best = PostsData.bestTime[post.platform]
    val times = (PostsData.times + listOfNotNull(best, post.time)).distinct().sortedBy { it.toMinutes() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("When it goes out", style = MaterialTheme.typography.headlineSmall)
            Text("${post.platform.label} · best time $best", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("Day", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PostsData.days.forEach { (d, full) ->
                    FilterChip(
                        selected = d == day,
                        onClick = { day = d },
                        label = { Text(d) },
                        modifier = Modifier.semantics { contentDescription = full },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Time", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                times.forEach { t ->
                    FilterChip(
                        selected = t == time,
                        onClick = { time = t },
                        label = { Text(t, style = NumberStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize)) },
                        leadingIcon = if (t == best) {
                            { Icon(Icons.Rounded.Star, contentDescription = "Best time", tint = ImpervirTheme.extras.accent, modifier = Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { onSave(day, time) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Save time")
            }
        }
    }
}

/** Small status pill used in the Scheduled list. */
@Composable
internal fun StatusPill(label: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
