package com.impervir.agent.feature.posts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.Platform
import com.impervir.agent.data.Post
import com.impervir.agent.data.PostStatus
import com.impervir.agent.data.SampleData
import com.impervir.agent.ui.components.AddPill
import com.impervir.agent.ui.components.CountUpText
import com.impervir.agent.ui.components.ImpervirCard
import com.impervir.agent.ui.components.PlatformBadge
import com.impervir.agent.ui.components.SectionTitle
import com.impervir.agent.ui.components.grouped
import com.impervir.agent.ui.components.money
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.NumberStyle

// ---------------------------------------------------------------- Scheduled

internal fun LazyListScope.scheduledItems(drafts: List<Post>, goingNow: List<String>, actions: DraftActions) {
    PostsData.days.forEachIndexed { i, (d, full) ->
        val rows = drafts.filter { it.day == d }.sortedBy { it.time.toMinutes() }
        item(key = "day-$d") {
            Column(Modifier.animateItem().staggerIn(i)) {
                Row(Modifier.padding(top = 8.dp, bottom = 8.dp).semantics(mergeDescendants = true) { heading() }, verticalAlignment = Alignment.Bottom) {
                    Text(d, style = MaterialTheme.typography.titleMedium)
                    Text("  $full", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (rows.isEmpty()) {
                    Text(
                        "Nothing planned. Rest day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                    )
                } else {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = ImpervirTheme.extras.card),
                    ) {
                        rows.forEachIndexed { n, p ->
                            if (n > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                            ScheduledRow(p, goingNow = p.id in goingNow, actions = actions)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduledRow(post: Post, goingNow: Boolean, actions: DraftActions) {
    var menu by remember { mutableStateOf(false) }
    val skipped = post.status == PostStatus.Skipped
    val scheme = MaterialTheme.colorScheme
    val (label, bg, fg) = when {
        skipped -> Triple("Skipped", scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
        goingNow -> Triple("Going out now", scheme.inverseSurface, scheme.inverseOnSurface)
        post.status == PostStatus.AutoPost -> Triple("Goes out on its own", scheme.secondaryContainer, scheme.onSecondaryContainer)
        post.status == PostStatus.Scheduled -> Triple("Approved", scheme.inverseSurface, scheme.inverseOnSurface)
        post.status == PostStatus.Posted -> Triple("Posted", scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
        else -> Triple("Waiting for you", scheme.primaryContainer, scheme.onPrimaryContainer)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Edit") { actions.edit(post) }
            .alpha(if (skipped) 0.5f else 1f)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(post.time, style = NumberStyle.copy(fontSize = 13.sp), modifier = Modifier.width(66.dp))
        PlatformBadge(post.platform, size = 28.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                post.body.lineSequence().first(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            StatusPill(label, bg, fg)
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreHoriz, contentDescription = "More for this post") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; actions.edit(post) })
                DropdownMenuItem(text = { Text("Move time") }, leadingIcon = { Icon(Icons.Rounded.AccessTime, null) }, onClick = { menu = false; actions.time(post) })
                if (!skipped) {
                    DropdownMenuItem(text = { Text("Post now") }, leadingIcon = { Icon(Icons.Rounded.Send, null) }, onClick = { menu = false; actions.postNow(post) })
                    DropdownMenuItem(text = { Text("Skip") }, leadingIcon = { Icon(Icons.Rounded.SkipNext, null) }, onClick = { menu = false; actions.skip(post) })
                } else {
                    DropdownMenuItem(text = { Text("Bring it back") }, leadingIcon = { Icon(Icons.Rounded.Restore, null) }, onClick = { menu = false; actions.restore(post) })
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Posted

internal fun LazyListScope.postedItems(posts: List<Post>, onOpen: (Post) -> Unit) {
    item(key = "totals") { Week42Totals(Modifier.animateItem()) }
    item(key = "posted-list") { PostedList(posts, onOpen, Modifier.animateItem()) }
}

@Composable
private fun Week42Totals(modifier: Modifier = Modifier) {
    var showStats by rememberSaveable { mutableStateOf(false) }
    val w = SampleData.week42
    ImpervirCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Week ${w.week} totals", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
            Text(w.range, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TotalStat(Icons.Rounded.Visibility, w.views, "views", PostsData.dailyViews, accent = false, format = { it.grouped() }, modifier = Modifier.weight(1f))
            TotalStat(Icons.Rounded.TouchApp, w.clicks, "clicks", PostsData.dailyClicks, accent = false, format = { it.grouped() }, modifier = Modifier.weight(1f))
            TotalStat(Icons.Rounded.Payments, w.revenue, "revenue", PostsData.dailyRevenue, accent = true, format = { it.money() }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text("Daily, Mon to Sun. Tuesday did most of the work.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        if (!showStats) {
            AddPill("Add orders and conversion", onClick = { showStats = true })
        }
        AnimatedVisibility(visible = showStats, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${w.orders}", style = NumberStyle.copy(fontSize = 15.sp))
                        Text(" orders  ·  ", style = MaterialTheme.typography.bodySmall)
                        Text("${w.clickToOrder}%", style = NumberStyle.copy(fontSize = 15.sp))
                        Text(" click to order", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlatformBadge(Platform.LinkedIn, size = 20.dp)
                        Text(" ${(w.revenueByPlatform[Platform.LinkedIn] ?: 0).money()}", style = NumberStyle.copy(fontSize = 15.sp))
                        Text(" from LinkedIn", style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(onClick = { showStats = false }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove orders and conversion", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun TotalStat(icon: ImageVector, value: Int, label: String, daily: List<Int>, accent: Boolean, format: (Int) -> String, modifier: Modifier = Modifier) {
    val tint = if (accent) ImpervirTheme.extras.accent else MaterialTheme.colorScheme.onSurface
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "${format(value)} $label" }) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(6.dp))
        CountUpText(value, style = NumberStyle.copy(fontSize = 22.sp), format = format)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Sparkline(daily, tint, Modifier.fillMaxWidth().height(22.dp))
    }
}

@Composable
internal fun Sparkline(values: List<Int>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val max = (values.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
        val stepX = size.width / (values.size - 1)
        val pts = values.mapIndexed { i, v -> Offset(i * stepX, size.height - 2f - (v / max) * (size.height - 4f)) }
        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, color.copy(alpha = 0.12f))
        drawPath(line, color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun PostedList(posts: List<Post>, onOpen: (Post) -> Unit, modifier: Modifier = Modifier) {
    var showMetrics by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        SectionTitle(
            "Last week's posts",
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp).semantics { heading() },
            trailing = {
                if (!showMetrics) AddPill("Views and clicks", onClick = { showMetrics = true })
                else TextButton(onClick = { showMetrics = false }) { Text("Hide views and clicks") }
            },
        )
        Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = ImpervirTheme.extras.card)) {
            posts.forEachIndexed { i, p ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                PostedRow(p, showMetrics, onOpen, Modifier.staggerIn(i))
            }
        }
    }
}

@Composable
private fun PostedRow(post: Post, showMetrics: Boolean, onOpen: (Post) -> Unit, modifier: Modifier = Modifier) {
    val stats = post.stats
    val revenue = stats?.revenue ?: 0
    Column(
        modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open post") { onOpen(post) }
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformBadge(post.platform, size = 32.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(post.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${post.day} ${post.time} · ${post.platform.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                revenue.money(),
                style = NumberStyle.copy(fontSize = 16.sp),
                color = if (revenue > 0) ImpervirTheme.extras.accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(visible = showMetrics && stats != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(Modifier.padding(start = 44.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Metric("Views", stats?.views ?: 0)
                Metric("Clicks", stats?.clicks ?: 0)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: Int) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.grouped(), style = NumberStyle.copy(fontSize = 15.sp))
    }
}
