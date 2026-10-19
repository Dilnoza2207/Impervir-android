package com.impervir.agent.feature.report

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.Platform
import com.impervir.agent.data.SampleData
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.components.PlatformBadge
import com.impervir.agent.ui.components.grouped
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Optional cards, behind Customize. Ids 0, 1 and 5 always show. */
internal val optionalCards = listOf(
    Triple(2, "Where the money came from", "Revenue by platform"),
    Triple(3, "Stories sell", "What kind of post made the money"),
    Triple(4, "I'm learning your voice", "How much you edit my drafts"),
)

internal const val reportSummary =
    "Week 42: $566 from 20 orders, up 36%. Customer stories made 60% of it. Next week: 2 customer stories, 1 behind-the-scenes post, and a Telegram offer on Friday."

/** Counts from 0 to [value] each time the card becomes active. */
@Composable
private fun storyCount(value: Int, active: Boolean, delayMs: Long = 350): Int {
    val a = remember { Animatable(if (active) 0f else value.toFloat()) }
    LaunchedEffect(active) {
        if (active) {
            a.snapTo(0f)
            delay(delayMs)
            a.animateTo(value.toFloat(), tween(1300, easing = FastOutSlowInEasing))
        }
    }
    return a.value.roundToInt()
}

/** 0 -> 1 each time the card becomes active (bars, lines). */
@Composable
private fun storyProgress(active: Boolean, delayMs: Int = 400, durationMs: Int = 1000): Float {
    val a = remember { Animatable(if (active) 0f else 1f) }
    LaunchedEffect(active) {
        if (active) {
            a.snapTo(0f)
            a.animateTo(1f, tween(durationMs, delayMs, Motion.Emphasized))
        }
    }
    return a.value
}

/** Fade-and-rise for supporting lines, re-played on activation. */
@Composable
private fun Modifier.rise(active: Boolean, delayMs: Int): Modifier {
    val p = storyProgress(active, delayMs, 700)
    return graphicsLayer { alpha = p; translationY = (1f - p) * 14.dp.toPx() }
}

@Composable
private fun Headline(text: String, active: Boolean) {
    key(active) {
        BlurRiseText(
            text,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, lineHeight = 58.sp, color = MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun CardFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 120.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

private val glass = Color.White.copy(alpha = 0.07f)
private val glassLine = Color.White.copy(alpha = 0.12f)

@Composable
internal fun ReportCard(id: Int, active: Boolean, onCheckIn: () -> Unit, onAnalytics: () -> Unit) {
    when (id) {
        0 -> MoneyCard(active)
        1 -> BestPostCard(active)
        2 -> PlatformsCard(active)
        3 -> StoriesCard(active)
        4 -> VoiceCard(active)
        else -> NextWeekCard(active, onCheckIn, onAnalytics)
    }
}

@Composable
private fun MoneyCard(active: Boolean) {
    val w = SampleData.week42
    val accent = ImpervirTheme.extras.accent
    val muted = ImpervirTheme.extras.muted
    CardFrame {
        Headline("Your posts made", active)
        val rev = storyCount(w.revenue, active)
        Text(
            "$$rev",
            style = NumberStyle.copy(
                fontSize = 112.sp, lineHeight = 116.sp,
                brush = Brush.verticalGradient(listOf(Color.White, accent)),
            ),
            modifier = Modifier.padding(top = 8.dp).semantics { contentDescription = "$566" },
        )
        Text(
            "Best week yet. Up 36%.",
            style = MaterialTheme.typography.headlineSmall.copy(fontStyle = FontStyle.Italic),
            color = accent,
            modifier = Modifier.padding(top = 8.dp).rise(active, 900),
        )
        val o = storyCount(w.orders, active)
        val c = storyCount(w.clicks, active)
        val v = storyCount(w.views, active)
        Text(
            "$o orders · ${c.grouped()} clicks · ${v.grouped()} views",
            style = NumberStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            color = muted,
            modifier = Modifier.padding(top = 10.dp).rise(active, 1100),
        )
    }
}

@Composable
private fun BestPostCard(active: Boolean) {
    val muted = ImpervirTheme.extras.muted
    CardFrame {
        Headline("Your best post", active)
        Column(
            Modifier
                .padding(top = 24.dp)
                .rise(active, 500)
                .fillMaxWidth()
                .background(glass, RoundedCornerShape(24.dp))
                .border(1.dp, glassLine, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformBadge(Platform.LinkedIn, size = 32.dp)
                Column(Modifier.padding(start = 10.dp)) {
                    Text("LinkedIn", style = MaterialTheme.typography.titleSmall)
                    Text("Tue · 9:10 AM", style = NumberStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = muted)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Customer story",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Text(
                "Last Tuesday a customer emailed me: “I finally stopped missing client deadlines.” She’d used my Notion planner for 11 days. The part she liked most was the one I almost cut.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        Row(Modifier.padding(top = 20.dp).rise(active, 800), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Stat("$" + storyCount(340, active), "revenue", highlight = true)
            Stat(storyCount(12, active).toString(), "orders")
            Stat(storyCount(8420, active).grouped(), "views")
        }
    }
}

@Composable
private fun Stat(value: String, label: String, highlight: Boolean = false) {
    Column {
        Text(value, style = NumberStyle.copy(fontSize = 30.sp), color = if (highlight) ImpervirTheme.extras.accent else Color.White)
        Text(label, style = MaterialTheme.typography.labelMedium, color = ImpervirTheme.extras.muted)
    }
}

@Composable
private fun PlatformsCard(active: Boolean) {
    val accent = ImpervirTheme.extras.accent
    val rows = listOf(Platform.LinkedIn to 340, Platform.Threads to 120, Platform.Telegram to 106, Platform.X to 0)
    CardFrame {
        Headline("Where the money came from", active)
        Text(
            "LinkedIn made $340 of your $566. Nearly 3 times Threads.",
            style = MaterialTheme.typography.bodyLarge,
            color = ImpervirTheme.extras.muted,
            modifier = Modifier.padding(top = 12.dp).rise(active, 600),
        )
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEachIndexed { i, (p, v) ->
                val prog = storyProgress(active, 700 + i * 120, 900)
                val shown = storyCount(v, active, 700L + i * 120)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformBadge(p, size = 28.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Row {
                            Text(p.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text("$$shown", style = NumberStyle.copy(fontSize = 16.sp), color = if (i == 0) accent else Color.White)
                        }
                        Canvas(Modifier.fillMaxWidth().height(8.dp).padding(top = 2.dp)) {
                            val r = CornerRadius(size.height / 2)
                            drawRoundRect(Color.White.copy(alpha = 0.10f), cornerRadius = r)
                            val w = size.width * v / 340f * prog
                            if (w > 0f) drawRoundRect(if (i == 0) accent else Color.White.copy(alpha = 0.55f), size = Size(w, size.height), cornerRadius = r)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoriesCard(active: Boolean) {
    val accent = ImpervirTheme.extras.accent
    CardFrame {
        Headline("Stories sell.", active)
        val pct = storyCount(SampleData.customerStoriesShare, active)
        Text("$pct%", style = NumberStyle.copy(fontSize = 120.sp, lineHeight = 124.sp), color = accent, modifier = Modifier.padding(top = 8.dp))
        Text(
            "of your money came from customer stories. Tips made $0.",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.rise(active, 900),
        )
        Text(
            "$" + storyCount(340, active) + " of $" + storyCount(566, active) + " · the LinkedIn customer story",
            style = NumberStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            color = ImpervirTheme.extras.muted,
            modifier = Modifier.padding(top = 14.dp).rise(active, 1100),
        )
    }
}

@Composable
private fun VoiceCard(active: Boolean) {
    val accent = ImpervirTheme.extras.accent
    val lilac = ImpervirTheme.extras.softLilac
    val muted = ImpervirTheme.extras.muted
    val measurer = rememberTextMeasurer()
    val data = SampleData.editRateTrend
    val prog = storyProgress(active, 600, 1400)
    CardFrame {
        Headline("I'm learning your voice.", active)
        Text(
            "Edits per draft fell from 34% to 11% in four weeks.",
            style = MaterialTheme.typography.bodyLarge,
            color = muted,
            modifier = Modifier.padding(top = 12.dp).rise(active, 600),
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(top = 20.dp)
                .semantics { contentDescription = "Edit rate: Week 38 34%, Week 42 11%" },
        ) {
            val top = 26.dp.toPx()
            val bottomPad = 8.dp.toPx()
            val chartH = size.height - top - bottomPad
            val inset = 18.dp.toPx()
            val pts = data.mapIndexed { i, (_, v) ->
                Offset(inset + i * (size.width - inset * 2) / data.lastIndex, top + chartH - (v - 5f) / 32f * chartH)
            }
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1..pts.lastIndex) {
                    val a = pts[i - 1]; val b = pts[i]
                    val mx = (a.x + b.x) / 2
                    cubicTo(mx, a.y, mx, b.y, b.x, b.y)
                }
            }
            val area = Path().apply { addPath(path); lineTo(pts.last().x, size.height); lineTo(pts.first().x, size.height); close() }
            clipRect(right = size.width * prog) {
                drawPath(area, Brush.verticalGradient(listOf(lilac.copy(alpha = 0.35f), Color.Transparent)))
                drawPath(path, lilac.copy(alpha = 0.35f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path, Color.White, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            }
            val style = NumberStyle.copy(fontSize = 12.sp, color = Color.White)
            val first = measurer.measure("W38 · 34%", style.copy(color = muted))
            drawText(first, topLeft = Offset(0f, pts[0].y - first.size.height - 8.dp.toPx()))
            drawCircle(Color.White, 4.dp.toPx(), pts[0])
            if (prog > 0.95f) {
                val last = measurer.measure("W42 · 11%", style.copy(color = accent))
                drawCircle(accent.copy(alpha = 0.3f), 11.dp.toPx(), pts.last())
                drawCircle(accent, 5.dp.toPx(), pts.last())
                drawText(last, topLeft = Offset(size.width - last.size.width, pts.last().y - last.size.height - 14.dp.toPx()))
            }
        }
        Text(
            "Each week it learns from your edits. Lower means it sounds more like you.",
            style = MaterialTheme.typography.bodySmall,
            color = muted,
            modifier = Modifier.padding(top = 12.dp).rise(active, 1200),
        )
    }
}

@Composable
private fun NextWeekCard(active: Boolean, onCheckIn: () -> Unit, onAnalytics: () -> Unit) {
    val accent = ImpervirTheme.extras.accent
    val plan = listOf("2 customer stories", "1 behind-the-scenes post", "A Telegram offer on Friday", "Sunday report")
    CardFrame {
        Headline("Next week.", active)
        Text(
            "Week 43 starts Monday, Oct 20. Here is the plan.",
            style = MaterialTheme.typography.bodyLarge,
            color = ImpervirTheme.extras.muted,
            modifier = Modifier.padding(top = 12.dp).rise(active, 500),
        )
        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            plan.forEachIndexed { i, item ->
                Row(Modifier.rise(active, 650 + i * 110), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(30.dp).background(if (i == 0) accent else glass, CircleShape).border(1.dp, glassLine, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", style = NumberStyle.copy(fontSize = 14.sp), color = if (i == 0) MaterialTheme.colorScheme.onPrimary else Color.White)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(item, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Column(Modifier.padding(top = 28.dp).rise(active, 1100), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onCheckIn, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Start Monday check-in") }
            OutlinedButton(onClick = onAnalytics, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("See full analytics", color = Color.White) }
        }
    }
}
