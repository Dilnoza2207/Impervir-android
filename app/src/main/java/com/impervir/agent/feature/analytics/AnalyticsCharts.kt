package com.impervir.agent.feature.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.SampleData
import com.impervir.agent.ui.components.PlatformBadge
import com.impervir.agent.ui.components.grouped
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------------------------

/** 0 -> 1 once, when the chart first appears. */
@Composable
internal fun rememberAppear(durationMs: Int = 900, delayMs: Int = 120): Float {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(durationMs, delayMs, Motion.Emphasized)) }
    return p.value
}

/** Smooth path through points (Catmull-Rom converted to cubic Béziers, low tension). */
internal fun smoothPath(points: List<Offset>, tension: Float = 0.18f): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until points.lastIndex) {
        val p0 = points[(i - 1).coerceAtLeast(0)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[(i + 2).coerceAtMost(points.lastIndex)]
        val c1 = Offset(p1.x + (p2.x - p0.x) * tension, p1.y + (p2.y - p0.y) * tension)
        val c2 = Offset(p2.x - (p3.x - p1.x) * tension, p2.y - (p3.y - p1.y) * tension)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}

/** Drag or tap across [count] slots; ticks a haptic each time the slot changes. */
@Composable
internal fun Modifier.scrubber(count: Int, onIndex: (Int) -> Unit): Modifier {
    val haptics = LocalHapticFeedback.current
    val cb by rememberUpdatedState(onIndex)
    return this
        .pointerInput(count) {
            var last = -1
            fun hit(x: Float) {
                val i = (x / size.width * count).toInt().coerceIn(0, count - 1)
                if (i != last) {
                    last = i
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    cb(i)
                }
            }
            detectHorizontalDragGestures(
                onDragStart = { hit(it.x) },
                onDragEnd = { last = -1 },
                onHorizontalDrag = { change, _ -> change.consume(); hit(change.position.x) },
            )
        }
        .pointerInput(count) {
            detectTapGestures { pos ->
                val i = (pos.x / size.width * count).toInt().coerceIn(0, count - 1)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                cb(i)
            }
        }
}

/** A small rounded tooltip with one or two lines, kept inside the canvas. */
internal fun DrawScope.tooltip(
    measurer: TextMeasurer,
    anchor: Offset,
    title: String,
    value: String,
    titleStyle: TextStyle,
    valueStyle: TextStyle,
    bg: Color,
) {
    val t = measurer.measure(title, titleStyle)
    val v = measurer.measure(value, valueStyle)
    val pad = 10.dp.toPx()
    val w = maxOf(t.size.width, v.size.width) + pad * 2
    val h = t.size.height + v.size.height + pad * 1.6f
    val x = (anchor.x - w / 2).coerceIn(0f, size.width - w)
    val y = (anchor.y - h - 10.dp.toPx()).coerceAtLeast(0f)
    drawRoundRect(bg, Offset(x, y), Size(w, h), CornerRadius(12.dp.toPx()))
    drawText(v, topLeft = Offset(x + pad, y + pad * 0.8f))
    drawText(t, topLeft = Offset(x + pad, y + pad * 0.8f + v.size.height))
}

/** The plain-language takeaway above each chart. */
@Composable
internal fun Takeaway(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = ImpervirTheme.extras.muted, modifier = Modifier.padding(bottom = 14.dp))
}

// ---------------------------------------------------------------------------------------------
// Hero sparkline (W35 to W42), glowing
// ---------------------------------------------------------------------------------------------

@Composable
fun HeroSparkline(modifier: Modifier = Modifier) {
    val accent = ImpervirTheme.extras.accent
    val muted = ImpervirTheme.extras.muted
    val measurer = rememberTextMeasurer()
    val p = rememberAppear(1200, 250)
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "pulse",
    )
    val values = SampleData.revenueTrend.map { it.second }
    val labelStyle = NumberStyle.copy(fontSize = 11.sp, color = muted, fontWeight = FontWeight.Medium)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(96.dp)
            .semantics { contentDescription = "Up 4.7x since Week 35, from $120 to $566" },
    ) {
        val labelH = 16.dp.toPx()
        val top = 12.dp.toPx()
        val chartH = size.height - labelH - top
        val inset = 6.dp.toPx()
        val max = 600f
        val pts = values.mapIndexed { i, v ->
            Offset(inset + i * (size.width - inset * 2) / values.lastIndex, top + chartH - v / max * chartH)
        }
        val line = smoothPath(pts)
        val area = Path().apply {
            addPath(line)
            lineTo(pts.last().x, top + chartH)
            lineTo(pts.first().x, top + chartH)
            close()
        }
        clipRect(right = size.width * p) {
            drawPath(area, Brush.verticalGradient(listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0f)), startY = top, endY = top + chartH))
            // Glow: wide soft strokes under the line
            drawPath(line, accent.copy(alpha = 0.10f), style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
            drawPath(line, accent.copy(alpha = 0.18f), style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
            drawPath(line, accent, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        }
        if (p > 0.98f) {
            val end = pts.last()
            drawCircle(accent.copy(alpha = (1f - pulse) * 0.35f), radius = 5.dp.toPx() + pulse * 12.dp.toPx(), center = end)
            drawCircle(accent, radius = 5.dp.toPx(), center = end)
            drawCircle(Color.White, radius = 2.dp.toPx(), center = end)
        }
        val l = measurer.measure("W35", labelStyle)
        val r = measurer.measure("W42", labelStyle)
        drawText(l, topLeft = Offset(0f, size.height - l.size.height))
        drawText(r, topLeft = Offset(size.width - r.size.width, size.height - r.size.height))
    }
}

// ---------------------------------------------------------------------------------------------
// Revenue by week: bars, scrubbable
// ---------------------------------------------------------------------------------------------

@Composable
fun RevenueByWeekChart() {
    val extras = ImpervirTheme.extras
    val onSurface = MaterialTheme.colorScheme.onSurface
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val tipBg = MaterialTheme.colorScheme.inverseSurface
    val tipFg = MaterialTheme.colorScheme.inverseOnSurface
    val measurer = rememberTextMeasurer()
    val weeks = SampleData.revenueTrend
    var sel by rememberSaveable { mutableIntStateOf(weeks.lastIndex) }
    val p = rememberAppear(900)
    val bars = remember { weeks.map { Animatable(0f) } }
    LaunchedEffect(Unit) {
        bars.forEachIndexed { i, a ->
            launch { a.animateTo(1f, tween(700, 80 + i * 55, Motion.Emphasized)) }
        }
    }
    val labelStyle = NumberStyle.copy(fontSize = 11.sp, color = extras.muted, fontWeight = FontWeight.Medium)
    val labelOn = labelStyle.copy(color = onSurface, fontWeight = FontWeight.SemiBold)
    Takeaway("Week 42 was your best: $566, up 36% on Week 41. Drag across the chart to see each week.")
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .scrubber(weeks.size) { sel = it }
            .semantics { contentDescription = "Revenue by week. " + weeks.joinToString { "Week ${it.first}: $${it.second}" } },
    ) {
        val labelH = 20.dp.toPx()
        val top = 58.dp.toPx()
        val chartH = size.height - labelH - top
        val slot = size.width / weeks.size
        val barW = slot * 0.56f
        val max = 600f
        weeks.forEachIndexed { i, (wk, v) ->
            val h = v / max * chartH * bars[i].value
            val x = i * slot + (slot - barW) / 2
            val on = i == sel
            drawRoundRect(
                color = if (on) extras.accent else track,
                topLeft = Offset(x, top + chartH - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
            val lbl = measurer.measure("W$wk", if (on) labelOn else labelStyle)
            drawText(lbl, topLeft = Offset(i * slot + (slot - lbl.size.width) / 2, size.height - lbl.size.height))
        }
        if (p > 0.6f) {
            val (wk, v) = weeks[sel]
            val prev = weeks.getOrNull(sel - 1)?.second
            val chip = if (prev != null) {
                val pct = ((v - prev) * 100f / prev).roundToInt()
                (if (pct >= 0) "+" else "") + "$pct% vs W${weeks[sel - 1].first}"
            } else "First week shown"
            val h = v / max * chartH
            tooltip(
                measurer,
                anchor = Offset(sel * slot + slot / 2, top + chartH - h),
                title = "Week $wk · $chip",
                value = "$$v",
                titleStyle = TextStyle(fontSize = 11.sp, color = tipFg.copy(alpha = 0.75f)),
                valueStyle = NumberStyle.copy(fontSize = 17.sp, color = tipFg),
                bg = tipBg,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Funnel: where sales drop off
// ---------------------------------------------------------------------------------------------

@Composable
fun FunnelChart() {
    val extras = ImpervirTheme.extras
    val onSurface = MaterialTheme.colorScheme.onSurface
    val steps = listOf(
        Triple("Views", "21,980", 1f),
        Triple("Clicks", "446", 0.78f),
        Triple("Orders", "20", 0.58f),
        Triple("Revenue", "$566", 0.42f),
    )
    val conv = listOf("2.0% of viewers clicked", "4.5% of clickers bought", "$28 average per order")
    Takeaway("About 2 in 100 viewers click, and 4 or 5 in 100 clickers buy.")
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        steps.forEachIndexed { i, (label, value, w) ->
            val last = i == steps.lastIndex
            val bar = if (last) extras.accent else onSurface.copy(alpha = 0.88f - i * 0.22f)
            val fg = if (last) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface
            val p = rememberAppear(700, 150 + i * 140)
            Box(Modifier.fillMaxWidth().height(44.dp)) {
                Canvas(Modifier.fillMaxWidth().height(44.dp)) {
                    drawRoundRect(bar, size = Size(size.width * w * p, size.height), cornerRadius = CornerRadius(size.height / 2))
                }
                Row(
                    Modifier.fillMaxWidth(w).height(44.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = fg.copy(alpha = p), modifier = Modifier.weight(1f))
                    Text(value, style = NumberStyle.copy(fontSize = 16.sp), color = fg.copy(alpha = p))
                }
            }
            if (i < conv.size) {
                Row(Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("↓", style = MaterialTheme.typography.labelMedium, color = extras.muted)
                    Text(conv[i], style = MaterialTheme.typography.labelMedium, color = extras.muted, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// By platform: donut
// ---------------------------------------------------------------------------------------------

@Composable
fun PlatformDonut(metric: PlatformMetric) {
    val extras = ImpervirTheme.extras
    val onSurface = MaterialTheme.colorScheme.onSurface
    val colors = listOf(extras.accent, onSurface.copy(alpha = 0.85f), extras.lilac, onSurface.copy(alpha = 0.18f))
    val total = platformRows.sumOf { it.of(metric) }
    fun show(v: Int) = when (metric) {
        PlatformMetric.Revenue -> "$" + v.grouped()
        PlatformMetric.Followers -> "+" + v.grouped()
        else -> v.grouped()
    }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(metric) {
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(900, 100, Motion.Emphasized))
    }
    Takeaway(metric.takeaway)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(148.dp), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .size(148.dp)
                    .semantics { contentDescription = "Share by platform: " + platformRows.joinToString { it.platform.label + " " + show(it.of(metric)) } },
            ) {
                val stroke = 18.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                var start = -90f
                val gap = 3f
                platformRows.forEachIndexed { i, row ->
                    val full = if (total == 0) 0f else row.of(metric) * 360f / total
                    val s = (full - gap).coerceAtLeast(0f) * sweep.value
                    if (s > 0f) {
                        drawArc(colors[i], start + gap / 2, s, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                    }
                    start += full * sweep.value
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(show(total), style = NumberStyle.copy(fontSize = 22.sp))
                Text(metric.centerLabel, style = MaterialTheme.typography.labelSmall, color = extras.muted)
            }
        }
        Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            platformRows.forEachIndexed { i, row ->
                val pct = if (total == 0) 0 else (row.of(metric) * 100f / total).roundToInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).padding(0.dp)) { Canvas(Modifier.size(8.dp)) { drawCircle(colors[i]) } }
                    Spacer(Modifier.width(8.dp))
                    PlatformBadge(row.platform, size = 22.dp)
                    Text(row.platform.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(show(row.of(metric)), style = NumberStyle.copy(fontSize = 14.sp))
                        Text("$pct%", style = NumberStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = extras.muted)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// What sells: horizontal bars
// ---------------------------------------------------------------------------------------------

@Composable
fun WhatSellsChart() {
    val extras = ImpervirTheme.extras
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Takeaway("Customer stories made 60% of your money.")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        contentTypes.forEachIndexed { i, (label, pct) ->
            val hot = i == 0
            val p = rememberAppear(800, 150 + i * 110)
            Column {
                Row {
                    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (hot) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    Text("$pct%", style = NumberStyle.copy(fontSize = 14.sp), color = if (hot) extras.accent else MaterialTheme.colorScheme.onSurface)
                }
                Canvas(Modifier.fillMaxWidth().height(10.dp).padding(top = 4.dp)) {
                    val r = CornerRadius(size.height / 2)
                    drawRoundRect(track, cornerRadius = r)
                    val w = size.width * pct / 100f * p
                    if (w > 0f) drawRoundRect(if (hot) extras.accent else extras.muted.copy(alpha = 0.55f), size = Size(w, size.height), cornerRadius = r)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// When your posts work best: heatmap 7 x 4
// ---------------------------------------------------------------------------------------------

@Composable
fun TimingHeatmap() {
    val extras = ImpervirTheme.extras
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    val ink = MaterialTheme.colorScheme.onSurface
    val measurer = rememberTextMeasurer()
    val p = rememberAppear(900, 100)
    val max = 340f
    val dayStyle = TextStyle(fontSize = 11.sp, color = extras.muted, fontWeight = FontWeight.Medium)
    val cellStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    Takeaway("Tuesday morning on LinkedIn sold the most last week.")
    Row(Modifier.fillMaxWidth().padding(start = 40.dp)) {
        heatSlots.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = extras.muted, modifier = Modifier.weight(1f), maxLines = 1) }
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(34.dp * 7 + 6.dp)
            .padding(top = 6.dp)
            .semantics { contentDescription = "Sales by day and time. Tuesday morning on LinkedIn sold $340, Wednesday midday on Threads $120, Monday evening on Telegram $58." },
    ) {
        val labelW = 40.dp.toPx()
        val gap = 4.dp.toPx()
        val cw = (size.width - labelW) / 4
        val ch = size.height / 7
        heatRevenue.forEachIndexed { r, row ->
            val d = measurer.measure(heatDays[r], dayStyle)
            drawText(d, topLeft = Offset(0f, r * ch + (ch - d.size.height) / 2))
            row.forEachIndexed { c, v ->
                val delay = (r + c) * 0.05f
                val local = ((p - delay) / (1f - 0.5f)).coerceIn(0f, 1f)
                val color = if (v > 0) extras.accent.copy(alpha = (0.18f + 0.82f * v / max) * local) else empty.copy(alpha = local)
                val tl = Offset(labelW + c * cw + gap / 2, r * ch + gap / 2)
                val sz = Size(cw - gap, ch - gap)
                val scale = 0.85f + 0.15f * local
                val sc = Size(sz.width * scale, sz.height * scale)
                drawRoundRect(color, Offset(tl.x + (sz.width - sc.width) / 2, tl.y + (sz.height - sc.height) / 2), sc, CornerRadius(8.dp.toPx()))
                heatLabels[r to c]?.let { lbl ->
                    val fg = if (v / max > 0.6f) Color(0xFF160A05) else ink
                    val t = measurer.measure(lbl, cellStyle.copy(color = fg.copy(alpha = local)))
                    drawText(t, topLeft = Offset(tl.x + (sz.width - t.size.width) / 2, tl.y + (sz.height - t.size.height) / 2))
                }
            }
        }
    }
    Text(
        "Darker means more sales. Each label is the top seller in that slot.",
        style = MaterialTheme.typography.bodySmall,
        color = extras.muted,
        modifier = Modifier.padding(top = 8.dp),
    )
}

// ---------------------------------------------------------------------------------------------
// Sounds like you: edit rate line, scrubbable
// ---------------------------------------------------------------------------------------------

@Composable
fun EditRateChart() {
    val extras = ImpervirTheme.extras
    val onSurface = MaterialTheme.colorScheme.onSurface
    val grid = MaterialTheme.colorScheme.outlineVariant
    val tipBg = MaterialTheme.colorScheme.inverseSurface
    val tipFg = MaterialTheme.colorScheme.inverseOnSurface
    val measurer = rememberTextMeasurer()
    val data = SampleData.editRateTrend
    var sel by rememberSaveable { mutableIntStateOf(data.lastIndex) }
    val p = rememberAppear(1100, 150)
    val labelStyle = NumberStyle.copy(fontSize = 11.sp, color = extras.muted, fontWeight = FontWeight.Medium)
    Takeaway("You rewrote 11% of my drafts in Week 42, down from 34% in Week 38.")
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(170.dp)
            .scrubber(data.size) { sel = it }
            .semantics { contentDescription = "Edit rate by week. " + data.joinToString { "Week ${it.first}: ${it.second}%" } },
    ) {
        val top = 56.dp.toPx()
        val labelH = 20.dp.toPx()
        val chartH = size.height - top - labelH - 6.dp.toPx()
        val slot = size.width / data.size
        val pts = data.mapIndexed { i, (_, v) -> Offset(slot * i + slot / 2, top + chartH - (v - 5f) / 35f * chartH) }
        // Baseline grid
        drawLine(grid, Offset(0f, top + chartH), Offset(size.width, top + chartH), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        val line = smoothPath(pts)
        val area = Path().apply { addPath(line); lineTo(pts.last().x, top + chartH); lineTo(pts.first().x, top + chartH); close() }
        clipRect(right = size.width * p) {
            drawPath(area, Brush.verticalGradient(listOf(extras.lilac.copy(alpha = 0.22f), extras.lilac.copy(alpha = 0f)), startY = top, endY = top + chartH))
            drawPath(line, onSurface.copy(alpha = 0.75f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
        // Scrub guide
        val sp = pts[sel]
        drawLine(onSurface.copy(alpha = 0.18f), Offset(sp.x, top - 4.dp.toPx()), Offset(sp.x, top + chartH), 1.dp.toPx())
        pts.forEachIndexed { i, pt ->
            if (pt.x <= size.width * p + 1f) {
                val hot = i == data.lastIndex
                drawCircle(if (hot) extras.accent else onSurface.copy(alpha = 0.75f), radius = if (i == sel) 5.dp.toPx() else 3.5.dp.toPx(), center = pt)
            }
            val lbl = measurer.measure("W${data[i].first}", if (i == sel) labelStyle.copy(color = onSurface) else labelStyle)
            drawText(lbl, topLeft = Offset(pt.x - lbl.size.width / 2, size.height - lbl.size.height))
        }
        if (p > 0.95f) {
            val (wk, v) = data[sel]
            val first = data.first().second
            val note = if (sel == 0) "Where we started" else "${first - v} points lower than W${data.first().first}"
            tooltip(
                measurer, sp, "Week $wk · $note", "$v%",
                TextStyle(fontSize = 11.sp, color = tipFg.copy(alpha = 0.75f)),
                NumberStyle.copy(fontSize = 17.sp, color = tipFg),
                tipBg,
            )
        }
    }
}
