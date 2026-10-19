package com.impervir.agent.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.Comment
import com.impervir.agent.data.Platform
import com.impervir.agent.data.Post
import com.impervir.agent.data.PostStatus
import com.impervir.agent.data.SampleData
import com.impervir.agent.ui.components.AddedBlock
import com.impervir.agent.ui.components.CountUpText
import com.impervir.agent.ui.components.ImpervirCard
import com.impervir.agent.ui.components.PlatformBadge
import com.impervir.agent.ui.components.SectionTitle
import com.impervir.agent.ui.components.grouped
import com.impervir.agent.ui.components.money
import com.impervir.agent.ui.ember.Ember
import com.impervir.agent.ui.ember.EmberMood
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.delay

// ---------------------------------------------------------------- Agent card

@Composable
internal fun AgentCard(needApproval: Int, totalPosts: Int, onReview: () -> Unit, modifier: Modifier = Modifier) {
    val full = when (needApproval) {
        0 -> "Your check-in is done. I wrote $totalPosts posts. All of them are approved."
        1 -> "Your check-in is done. I wrote $totalPosts posts. 1 needs your okay."
        else -> "Your check-in is done. I wrote $totalPosts posts. $needApproval need your okay."
    }
    // Type the sentence once (Ember talks while it types, then is happy, then settles).
    var typedCount by rememberSaveable { mutableIntStateOf(0) }
    var settled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(full) {
        if (typedCount >= full.length) { typedCount = full.length; return@LaunchedEffect }
        delay(450)
        while (typedCount < full.length) { typedCount = (typedCount + 2).coerceAtMost(full.length); delay(28) }
        delay(1400)
        settled = true
    }
    val done = typedCount >= full.length
    val mood = when {
        !done && typedCount > 0 -> EmberMood.Talking
        done && !settled -> EmberMood.Happy
        else -> EmberMood.Idle
    }

    ImpervirCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ember(mood = mood, size = 56.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Your agent", style = MaterialTheme.typography.titleMedium)
                Text("Monday check-in, 2 min", style = MaterialTheme.typography.bodyMedium, color = ImpervirTheme.extras.muted)
            }
        }
        Text(
            text = full.take(typedCount),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(top = 14.dp)
                .heightIn(min = 56.dp)
                .semantics { contentDescription = full; liveRegion = LiveRegionMode.Polite },
        )
        AnimatedVisibility(visible = done, enter = fadeIn(tween(260)) + scaleIn(Motion.pop(), initialScale = 0.9f)) {
            Button(
                onClick = onReview,
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(52.dp),
            ) {
                Text(if (needApproval == 0) "See this week's posts" else "Review $needApproval post${if (needApproval == 1) "" else "s"}")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- This week

private enum class StepState { Done, Now, Later }
private data class WeekStep(val name: String, val date: String, val state: StepState)

private val weekSteps = listOf(
    WeekStep("Talk", "Oct 20", StepState.Done),
    WeekStep("Agent writes", "Oct 20", StepState.Done),
    WeekStep("You approve", "Today", StepState.Now),
    WeekStep("It posts", "Oct 21", StepState.Later),
    WeekStep("You see sales", "Oct 26", StepState.Later),
)

@Composable
internal fun ThisWeekCard(modifier: Modifier = Modifier) {
    ImpervirCard(modifier.fillMaxWidth()) {
        SectionTitle("This week", Modifier.semantics { heading() }) {
            Text("Week ${SampleData.thisWeek}", style = NumberStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), color = ImpervirTheme.extras.muted)
        }
        Text(
            "Monday check-in is done. Your approval is next.",
            style = MaterialTheme.typography.bodyMedium,
            color = ImpervirTheme.extras.muted,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        val accent = ImpervirTheme.extras.accent
        val track = MaterialTheme.colorScheme.outlineVariant
        val doneCount = weekSteps.count { it.state == StepState.Done }
        val progress by animateFloatAsState(
            targetValue = doneCount.toFloat() / (weekSteps.size - 1),
            animationSpec = tween(900, 300, Motion.Emphasized), label = "weekProgress",
        )
        Box(Modifier.fillMaxWidth()) {
            // Connector line behind the nodes, filled up to "You approve".
            Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                val inset = size.width / (weekSteps.size * 2)
                val y = size.height / 2
                drawLine(track, Offset(inset, y), Offset(size.width - inset, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(accent, Offset(inset, y), Offset(inset + (size.width - 2 * inset) * progress, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
            Row(Modifier.fillMaxWidth()) {
                weekSteps.forEachIndexed { i, step ->
                    Column(
                        Modifier.weight(1f).semantics(mergeDescendants = true) {},
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        StepNode(i + 1, step.state)
                        Text(
                            step.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (step.state == StepState.Later) FontWeight.Normal else FontWeight.SemiBold,
                                letterSpacing = 0.sp,
                            ),
                            color = if (step.state == StepState.Later) ImpervirTheme.extras.muted else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(step.date, style = NumberStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = ImpervirTheme.extras.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepNode(number: Int, state: StepState) {
    val accent = ImpervirTheme.extras.accent
    val bg = when (state) {
        StepState.Done -> MaterialTheme.colorScheme.inverseSurface
        StepState.Now -> accent
        StepState.Later -> ImpervirTheme.extras.card
    }
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bg)
            .then(if (state == StepState.Later) Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            StepState.Done -> Icon(Icons.Rounded.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.size(16.dp))
            StepState.Now -> Text("$number", style = NumberStyle.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onPrimary)
            StepState.Later -> Text("$number", style = NumberStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), color = ImpervirTheme.extras.muted)
        }
    }
}

// ---------------------------------------------------------------- Last week

@Composable
internal fun LastWeekCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val week = SampleData.week42
    val trend = SampleData.revenueTrend
    val prev = trend[trend.lastIndex - 1].second
    val growth = ((week.revenue - prev) * 100f / prev).toInt()
    ImpervirCard(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "Last week: ${week.revenue.money()} revenue, up $growth percent. Open analytics"
        },
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Last week", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = ImpervirTheme.extras.muted)
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                CountUpText(week.revenue, format = { it.money() }, style = NumberStyle.copy(fontSize = 44.sp, lineHeight = 46.sp))
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), color = ImpervirTheme.extras.positive.copy(alpha = 0.14f)) {
                        Text(
                            "+$growth%",
                            style = NumberStyle.copy(fontSize = 13.sp),
                            color = ImpervirTheme.extras.positive,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Text("vs W${SampleData.lastWeek - 1}", style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Sparkline(trend.map { it.second }, Modifier.width(120.dp).height(48.dp))
        }
        Text(
            "Your best week yet, up from ${prev.money()}.",
            style = MaterialTheme.typography.bodyMedium,
            color = ImpervirTheme.extras.muted,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun Sparkline(values: List<Int>, modifier: Modifier = Modifier) {
    val accent = ImpervirTheme.extras.accent
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val p by animateFloatAsState(if (started) 1f else 0f, tween(1100, 250, Motion.Emphasized), label = "sparkDraw")
    Canvas(modifier.semantics { contentDescription = "Revenue rising over 8 weeks" }) {
        val min = values.min().toFloat()
        val max = values.max().toFloat()
        val pts = values.mapIndexed { i, v ->
            Offset(i * size.width / (values.size - 1), 4.dp.toPx() + (1f - (v - min) / (max - min)) * (size.height - 8.dp.toPx()))
        }
        val visible = (pts.size - 1) * p
        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val t = (visible - (i - 1)).coerceIn(0f, 1f)
                if (t <= 0f) break
                lineTo(pts[i - 1].x + (pts[i].x - pts[i - 1].x) * t, pts[i - 1].y + (pts[i].y - pts[i - 1].y) * t)
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(pts[0].x + (size.width) * p, size.height)
            lineTo(pts[0].x, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent)))
        drawPath(line, accent, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (p >= 0.99f) drawCircle(accent, 4.dp.toPx(), pts.last())
    }
}

// ---------------------------------------------------------------- Comments nudge

@Composable
internal fun CommentsNudge(comments: List<Comment>, onReply: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val byPlatform = comments.groupingBy { it.platform }.eachCount()
    val split = listOf(Platform.LinkedIn, Platform.Threads, Platform.X, Platform.Telegram)
        .mapNotNull { p -> byPlatform[p]?.let { "$it on ${p.label}" } }
        .joinToString(", ")
    val avatars = comments.map { it.initials }.distinct().take(5)
    val tints = listOf(
        MaterialTheme.colorScheme.surfaceVariant, ImpervirTheme.extras.softLilac, ImpervirTheme.extras.softPeach,
        MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.inverseSurface,
    )
    ImpervirCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f)) {
                avatars.forEachIndexed { i, a ->
                    val bg = tints[i % tints.size]
                    val fg = if (i % tints.size == 4) MaterialTheme.colorScheme.inverseOnSurface else Color(0xFF141318)
                    Box(
                        Modifier
                            .offset(x = (-8 * i).dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ImpervirTheme.extras.card)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(bg),
                        contentAlignment = Alignment.Center,
                    ) { Text(a, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = fg) }
                }
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Dismiss this nudge", tint = ImpervirTheme.extras.muted) }
        }
        Text(
            "${comments.size} comment${if (comments.size == 1) " is" else "s are"} waiting.",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
        )
        Text(
            "Answering in the first hour gets 30 to 42% more engagement. $split.",
            style = MaterialTheme.typography.bodyMedium,
            color = ImpervirTheme.extras.muted,
            modifier = Modifier.padding(top = 4.dp),
        )
        Button(
            onClick = onReply,
            modifier = Modifier.padding(top = 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ),
        ) {
            Text("Reply now")
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

// ---------------------------------------------------------------- Up next

@Composable
internal fun UpNextSection(
    posts: List<Post>,
    onOpen: (Post) -> Unit,
    onOpenCalendar: () -> Unit,
    onLater: (Post) -> Unit,
    onSkip: (Post) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The countdown ticks once a minute from the sample "now".
    var elapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); elapsed++ } }
    ImpervirCard(modifier.fillMaxWidth()) {
        SectionTitle("Up next", Modifier.semantics { heading() })
        Spacer(Modifier.height(4.dp))
        if (posts.isEmpty()) {
            Text("Nothing scheduled yet.", style = MaterialTheme.typography.bodyMedium, color = ImpervirTheme.extras.muted, modifier = Modifier.padding(top = 8.dp))
        }
        posts.forEachIndexed { i, post ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            UpNextRow(post, SampleNowMinutes + elapsed, onOpen, onOpenCalendar, onLater, onSkip)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UpNextRow(
    post: Post,
    nowMinutes: Int,
    onOpen: (Post) -> Unit,
    onOpenCalendar: () -> Unit,
    onLater: (Post) -> Unit,
    onSkip: (Post) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val mins = (minutesOfWeek(post) - nowMinutes).coerceAtLeast(0)
    val auto = post.status == PostStatus.AutoPost
    val chip = when (post.status) {
        PostStatus.AutoPost -> "Goes out on its own"
        PostStatus.Scheduled -> "Approved"
        else -> "Waiting for you"
    }
    val chipBg by animateColorAsState(
        if (post.status == PostStatus.NeedsApproval) ImpervirTheme.extras.accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        label = "chipBg",
    )
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .combinedClickable(
                    onClickLabel = "Open post",
                    onLongClickLabel = "More options",
                    onClick = { onOpen(post) },
                    onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); menu = true },
                )
                .padding(vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformBadge(post.platform, size = 36.dp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(post.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text(
                        "${post.day} · ${post.time} · ${post.platform.label}",
                        style = NumberStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
                        color = ImpervirTheme.extras.muted,
                    )
                }
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More options for ${post.title}", tint = ImpervirTheme.extras.muted)
                }
            }
            Row(Modifier.padding(start = 48.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("in ${mins / 60}h ${mins % 60}m", style = NumberStyle.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurface)
                Surface(shape = RoundedCornerShape(50), color = chipBg) {
                    Text(
                        chip,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (auto) ImpervirTheme.extras.muted else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Open in calendar") }, onClick = { menu = false; onOpenCalendar() })
                DropdownMenuItem(text = { Text("Move 1 hour later") }, onClick = { menu = false; onLater(post) })
                DropdownMenuItem(
                    text = { Text("Skip this post", color = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onSkip(post) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Optional widgets

@Composable
internal fun ReportWidget(visible: Boolean, onRemove: () -> Unit, onOpen: () -> Unit) {
    AddedBlock(visible = visible, title = "Week ${SampleData.lastWeek} report", onRemove = onRemove) {
        Text(
            "Customer stories made ${SampleData.customerStoriesShare}% of your sales.",
            style = MaterialTheme.typography.headlineSmall,
        )
        Button(
            onClick = onOpen,
            modifier = Modifier.padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface),
        ) {
            Text("Watch your Week ${SampleData.lastWeek} recap")
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun FollowersWidget(visible: Boolean, onRemove: () -> Unit) {
    val total = SampleData.followers.sumOf { it.change }
    AddedBlock(visible = visible, title = "Followers", onRemove = onRemove) {
        Text("+${total.grouped()} this week", style = NumberStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), color = ImpervirTheme.extras.positive)
        Spacer(Modifier.height(8.dp))
        SampleData.followers.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                pair.forEach { f ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        PlatformBadge(f.platform, size = 34.dp)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(f.count.grouped(), style = NumberStyle.copy(fontSize = 20.sp))
                            Row {
                                Text("+${f.change}", style = NumberStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = ImpervirTheme.extras.positive)
                                Text(" · ${f.platform.label}", style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ShortcutsWidget(visible: Boolean, onRemove: () -> Unit, onJot: () -> Unit, onVoice: () -> Unit, onStore: () -> Unit) {
    AddedBlock(visible = visible, title = "Shortcuts", onRemove = onRemove) {
        ShortcutRow("Jot an idea", "Keep it for your next check-in", onJot)
        ShortcutRow("Your voice", "You edited 11% of drafts", onVoice)
        ShortcutRow("Connect a store", "Stripe, Gumroad or Lemon Squeezy", onStore)
    }
}

@Composable
private fun ShortcutRow(title: String, sub: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.small, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = ImpervirTheme.extras.muted)
        }
    }
}
