package com.impervir.agent.feature.postdetail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.Comment
import com.impervir.agent.data.Post
import com.impervir.agent.data.PostStats
import com.impervir.agent.data.SampleData
import com.impervir.agent.ui.components.AddedBlock
import com.impervir.agent.ui.components.Avatar
import com.impervir.agent.ui.components.CountUpText
import com.impervir.agent.ui.components.ImpervirCard
import com.impervir.agent.ui.components.grouped
import com.impervir.agent.ui.components.money
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.NumberStyle

@Composable
internal fun PostTextCard(post: Post) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val accent = ImpervirTheme.extras.accent
    val segs = DetailData.segments[post.id]
    ImpervirCard(Modifier.fillMaxWidth().animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(SampleData.userInitials, size = 36.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text(SampleData.userName, style = MaterialTheme.typography.titleSmall)
                Text("The post as published", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        val text = buildAnnotatedString {
            if (segs == null) append(post.body)
            else segs.forEach { (t, hl) ->
                if (hl) withStyle(SpanStyle(background = accent.copy(alpha = 0.22f))) { append(t) } else append(t)
            }
        }
        val long = text.length > 140
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (expanded || !long) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
        )
        if (long) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.offset(x = (-12).dp)) {
                Text(if (expanded) "Show less" else "Show more")
            }
        } else Spacer(Modifier.height(10.dp))
        val times = DetailData.sourceTimes[post.id] ?: listOfNotNull(post.source?.timestamp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Mic, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                when {
                    times.isEmpty() -> "Written from your weekly check-in"
                    segs != null -> "Highlighted words came from your voice note · ${times.joinToString(" and ")}"
                    else -> "From your voice note · ${times.joinToString(" and ")}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun RevenueCard(post: Post, stats: PostStats) {
    val rank = DetailData.rank(post)
    ImpervirCard(Modifier.fillMaxWidth()) {
        Text("Revenue from this post", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(4.dp))
        CountUpText(
            stats.revenue,
            style = NumberStyle.copy(fontSize = 64.sp, lineHeight = 68.sp),
            format = { it.money() },
            modifier = Modifier.semantics { contentDescription = "${stats.revenue.money()} revenue" },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (stats.orders > 0) ImpervirTheme.extras.positive else MaterialTheme.colorScheme.outline))
            Spacer(Modifier.width(8.dp))
            Text(
                if (stats.orders > 0) "from ${stats.orders} order${if (stats.orders == 1) "" else "s"}" + (if (rank == 1) " · #1 post this week" else " · #$rank this week")
                else "No orders from this post yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun MoneyTrail(post: Post, stats: PostStats) {
    ImpervirCard(Modifier.fillMaxWidth()) {
        Text("The money trail", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text("Every sale traces back to one tracking link.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        val steps = listOf(
            Triple("1", "Post" to "", "${post.day} ${post.time} · ${post.platform.label}"),
            Triple("2", stats.clicks.grouped() to " clicks", "${DetailData.pct(stats.clicks, stats.views)} of the ${stats.views.grouped()} people who saw it"),
            Triple("3", stats.orders.grouped() to " orders", "${DetailData.pct(stats.orders, stats.clicks)} of the clicks turned into a buy"),
            Triple("$", stats.revenue.money() to " via ${DetailData.store.label}", "Credited to this post by its tracking link"),
        )
        steps.forEachIndexed { i, (n, head, sub) ->
            val last = i == steps.lastIndex
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp).fillMaxHeight()) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (last) ImpervirTheme.extras.accent else MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(n, style = NumberStyle.copy(fontSize = 13.sp), color = if (last) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                    if (!last) Box(Modifier.width(2.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
                }
                Column(Modifier.padding(start = 14.dp, top = 3.dp).semantics(mergeDescendants = true) {}) {
                    Row {
                        val numeric = head.second.isNotEmpty()
                        Text(head.first, style = if (numeric) NumberStyle.copy(fontSize = 16.sp) else MaterialTheme.typography.titleSmall)
                        if (numeric) Text(head.second, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun CommentsCard(waiting: List<Comment>, onOpen: () -> Unit) {
    ImpervirCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = ImpervirTheme.extras.accent)
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    "${waiting.size} comment${if (waiting.size == 1) "" else "s"} still need${if (waiting.size == 1) "s" else ""} you",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text("The agent drafted a reply for each. You send them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val faces = waiting.map { it.initials }.distinct().take(3)
            Box(Modifier.width((28 + (faces.size - 1) * 20).dp)) {
                faces.forEachIndexed { i, f ->
                    Avatar(
                        f,
                        size = 28.dp,
                        modifier = Modifier.offset(x = (i * 20).dp).border(2.dp, ImpervirTheme.extras.card, CircleShape),
                    )
                }
            }
            val names = waiting.map { it.author }.distinct()
            Text(
                when (names.size) {
                    1 -> names[0]
                    2 -> "${names[0]} and ${names[1]}"
                    else -> "${names[0]}, ${names[1]} and ${waiting.size - waiting.count { it.author == names[0] || it.author == names[1] }} more"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Answer in Inbox")
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp).size(18.dp))
        }
    }
}

@Composable
internal fun AllStatsBlock(visible: Boolean, stats: PostStats, onRemove: () -> Unit) {
    AddedBlock(visible = visible, title = "All stats", onRemove = onRemove) {
        val items = listOfNotNull(
            "Views" to stats.views.grouped(),
            stats.likes?.let { "Likes" to it.grouped() },
            stats.comments?.let { "Comments" to it.grouped() },
            "Clicks" to stats.clicks.grouped(),
            "Click rate" to DetailData.pct(stats.clicks, stats.views),
            "Orders" to stats.orders.grouped(),
            "Buy rate" to DetailData.pct(stats.orders, stats.clicks),
            "Revenue" to stats.revenue.money(),
        )
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, style = NumberStyle.copy(fontSize = 20.sp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChartBlock(visible: Boolean, stats: PostStats, onRemove: () -> Unit) {
    var clicks by rememberSaveable { mutableStateOf(false) }
    AddedBlock(visible = visible, title = "Views over time", onRemove = onRemove) {
        Text("First 72 hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(false to "Views", true to "Clicks").forEachIndexed { i, (isClicks, label) ->
                SegmentedButton(
                    selected = clicks == isClicks,
                    onClick = { clicks = isClicks },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    label = { Text(label) },
                )
            }
        }
        val values = if (clicks) DetailData.clickCurve(stats.clicks) else DetailData.viewCurve(stats.views)
        val total = values.last()
        val firstHour = values[1]
        val first24 = values[DetailData.hours.indexOf(24)]
        val first12 = values[DetailData.hours.indexOf(12)]
        Spacer(Modifier.height(10.dp))
        Text(
            if (clicks) "Clicks came in one wave: ${first24.grouped()} of ${total.grouped()} in the first 24 hours."
            else "${DetailData.pct(first12, total).substringBefore(".")}% of the views came in the first 12 hours.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(10.dp))
        LineChart(values, Modifier.fillMaxWidth().height(150.dp), ink = clicks)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0h", "24h", "48h", "72h").forEach {
                Text(it, style = NumberStyle.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            "First hour: ${firstHour.grouped()} ${if (clicks) "clicks" else "views"}",
            style = MaterialTheme.typography.labelMedium,
            color = ImpervirTheme.extras.accent,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun LineChart(values: List<Int>, modifier: Modifier, ink: Boolean) {
    val accent = ImpervirTheme.extras.accent
    val color = if (ink) MaterialTheme.colorScheme.onSurface else accent
    val grid = MaterialTheme.colorScheme.outlineVariant
    val progress = remember(ink) { Animatable(0f) }
    LaunchedEffect(ink) { progress.animateTo(1f, tween(700)) }
    Canvas(modifier.semantics { contentDescription = "Line chart over the first 72 hours" }) {
        val max = (values.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
        val pts = DetailData.hours.mapIndexed { i, h ->
            Offset(h / 72f * size.width, size.height - (values[i] / max) * (size.height - 8f) * progress.value)
        }
        listOf(0.25f, 0.5f, 0.75f).forEach { f ->
            drawLine(grid, Offset(0f, size.height * f), Offset(size.width, size.height * f), strokeWidth = 1f)
        }
        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(pts.last().x, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, color.copy(alpha = 0.12f))
        drawPath(line, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(accent, radius = 5.dp.toPx(), center = pts[1])
        drawCircle(accent.copy(alpha = 0.25f), radius = 10.dp.toPx(), center = pts[1])
    }
}

@Composable
internal fun OrdersBlock(visible: Boolean, post: Post, stats: PostStats, onRemove: () -> Unit) {
    var all by rememberSaveable { mutableStateOf(false) }
    val orders = DetailData.orders[post.id].orEmpty()
    AddedBlock(visible = visible, title = "Orders from this post", onRemove = onRemove) {
        Text(
            "${stats.orders} order${if (stats.orders == 1) "" else "s"} · ${stats.revenue.money()}",
            style = NumberStyle.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (orders.isEmpty()) {
            Text("No orders yet. Views without clicks usually mean the post had no link.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
        Column(Modifier.animateContentSize()) {
            val shown = if (all) orders else orders.take(5)
            shown.forEachIndexed { i, o ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(o.product, style = MaterialTheme.typography.bodyMedium)
                        Text("${o.time} · ${DetailData.store.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(o.price.money(), style = NumberStyle.copy(fontSize = 15.sp))
                }
            }
        }
        if (orders.size > 5) {
            TextButton(onClick = { all = !all }) {
                Text(if (all) "Show fewer" else "Show all ${orders.size}")
                Icon(if (all) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
internal fun AudienceBlock(visible: Boolean, onRemove: () -> Unit) {
    AddedBlock(visible = visible, title = "Who engaged", onRemove = onRemove) {
        Text("Founders made up 41% of the people who engaged.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        DetailData.audience.forEachIndexed { i, (name, pct) ->
            val p = remember { Animatable(0f) }
            LaunchedEffect(Unit) { p.animateTo(pct / 100f, tween(700, delayMillis = i * 80)) }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp).semantics(mergeDescendants = true) { contentDescription = "$name $pct%" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(96.dp))
                Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Box(
                        Modifier
                            .fillMaxWidth((p.value / 0.41f).coerceIn(0f, 1f))
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(if (i == 0) ImpervirTheme.extras.accent else MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
                Text("$pct%", style = NumberStyle.copy(fontSize = 13.sp), modifier = Modifier.width(44.dp).padding(start = 8.dp))
            }
        }
    }
}
