package com.impervir.agent.feature.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreTime
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.Post
import com.impervir.agent.data.PostStatus
import com.impervir.agent.ui.components.AddPill
import com.impervir.agent.ui.components.PlatformBadge
import com.impervir.agent.ui.components.SectionTitle
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.NumberStyle

// ---------------------------------------------------------------- layout model

internal sealed interface Entry {
    val key: String
    val minutes: Int
    data class PostEntry(val post: Post) : Entry {
        override val key get() = "p-${post.id}"
        override val minutes get() = post.time.toMinutes()
    }
    data class SlotEntry(val slot: CalSlot) : Entry {
        override val key get() = "s-${slot.id}"
        override val minutes get() = slot.minutes
    }
    data object Now : Entry {
        override val key get() = "now"
        override val minutes get() = CalendarData.nowMinutes
    }
}

/** One hour of the day timeline and everything that starts inside it, in time order. */
internal data class HourSlice(val hour: Int, val entries: List<Entry>)

private fun Entry.order(): Int = when (this) {
    Entry.Now -> 0
    is Entry.PostEntry -> 1
    is Entry.SlotEntry -> 2
}

internal fun dayEntries(day: Int, posts: List<Post>, slots: List<CalSlot>): List<Entry> =
    (posts.filter { dayIndex(it.day) == day }.map { Entry.PostEntry(it) } + slots.filter { it.day == day }.map { Entry.SlotEntry(it) })
        .sortedWith(compareBy<Entry>({ it.minutes }, { it.order() }))

/**
 * Hour rows from 6 AM to 10 PM (wider when something falls outside). Each row holds the items that
 * start in that hour and grows to fit them, so blocks, the now line and hour lines never overlap.
 */
internal fun timelineRows(day: Int, posts: List<Post>, slots: List<CalSlot>): List<HourSlice> {
    val entries = dayEntries(day, posts, slots).let { if (day == CalendarData.todayIndex) (it + Entry.Now).sortedWith(compareBy<Entry>({ e -> e.minutes }, { e -> e.order() })) else it }
    val start = minOf(6, (entries.minOfOrNull { it.minutes } ?: 360) / 60)
    val end = minOf(24, maxOf(22, (entries.maxOfOrNull { it.minutes } ?: 0) / 60 + 1))
    return (start until end).map { h -> HourSlice(h, entries.filter { it.minutes / 60 == h }) }
}

// ---------------------------------------------------------------- pieces

@Composable
internal fun TodayBadge(modifier: Modifier = Modifier) {
    Text(
        "Today",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.clip(CircleShape).background(ImpervirTheme.extras.accent).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
internal fun DayPills(posts: List<Post>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CalendarData.days.forEachIndexed { i, d ->
            val on = i == selected
            val dayPosts = posts.filter { dayIndex(it.day) == i }.sortedBy { it.time.toMinutes() }
            val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surfaceContainerHigh, label = "pill")
            val fg = if (on) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
            Surface(
                onClick = { onSelect(i) },
                shape = RoundedCornerShape(18.dp),
                color = bg,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 72.dp)
                    .semantics {
                        contentDescription = "${d.full}, ${d.date}, " +
                            if (dayPosts.isEmpty()) "nothing scheduled" else "${dayPosts.size} post${if (dayPosts.size > 1) "s" else ""}"
                    },
            ) {
                Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(d.short, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.75f))
                    Text("${d.number}", style = NumberStyle.copy(fontSize = 18.sp), color = fg)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(5.dp)) {
                        dayPosts.take(3).forEach { p ->
                            Box(Modifier.size(5.dp).clip(CircleShape).background(CalendarData.bandColor.getValue(p.platform)))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BestLegend(bands: List<Band>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        bands.sortedBy { it.from }.forEach { b ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(b.color))
                Text(" ${b.platform.label} ", style = MaterialTheme.typography.labelMedium)
                Text(b.label, style = NumberStyle.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun HourRow(
    row: HourSlice,
    bands: List<Band>,
    goingNow: List<String>,
    onOpen: (Post) -> Unit,
    onPostTime: (Post) -> Unit,
    onSlotTime: (CalSlot) -> Unit,
    onRemoveSlot: (CalSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inBands = bands.filter { row.hour * 60 >= it.from && row.hour * 60 < it.to }
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(
            hourLabel(row.hour),
            style = NumberStyle.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp),
        )
        Box(Modifier.weight(1f)) {
            // Best-time band: a soft wash with a thin colored edge per platform.
            if (inBands.isNotEmpty()) {
                Box(Modifier.matchParentSize().background(inBands.first().color.copy(alpha = 0.10f)))
                Row(Modifier.matchParentSize()) {
                    inBands.forEach { b -> Box(Modifier.width(3.dp).fillMaxHeight().background(b.color)) }
                }
            }
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Column(
                    Modifier.fillMaxWidth().padding(start = if (inBands.isEmpty()) 0.dp else 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.entries.forEach { e ->
                        when (e) {
                            Entry.Now -> NowLine()
                            is Entry.PostEntry -> CalPostCard(e.post, e.post.id in goingNow, onOpen = { onOpen(e.post) }, onTime = { onPostTime(e.post) })
                            is Entry.SlotEntry -> SlotCard(e.slot, onTime = { onSlotTime(e.slot) }, onRemove = { onRemoveSlot(e.slot) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowLine() {
    val accent = ImpervirTheme.extras.accent
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = "Now, ${CalendarData.nowMinutes.asTime()}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(CalendarData.nowMinutes.asTime().substringBefore(" "), style = NumberStyle.copy(fontSize = 11.sp), color = accent)
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
        Box(Modifier.weight(1f).height(2.dp).background(accent))
    }
}

private data class StatusLook(val label: String, val container: Color, val content: Color, val border: Color?, val dot: Color)

@Composable
private fun statusLook(post: Post, goingNow: Boolean): StatusLook {
    val s = MaterialTheme.colorScheme
    val e = ImpervirTheme.extras
    return when {
        goingNow -> StatusLook("Going out now", s.inverseSurface, s.inverseOnSurface, null, e.positive)
        post.status == PostStatus.AutoPost -> StatusLook("Goes out on its own", s.secondaryContainer, s.onSecondaryContainer, null, s.onSecondaryContainer)
        post.status == PostStatus.Scheduled -> StatusLook("Approved", s.inverseSurface, s.inverseOnSurface, null, e.accent)
        post.status == PostStatus.Posted -> StatusLook("Posted", s.surfaceContainerHigh, s.onSurfaceVariant, null, e.positive)
        else -> StatusLook("Waiting for you", e.card, s.onSurface, e.accent, e.accent)
    }
}

@Composable
internal fun CalPostCard(post: Post, goingNow: Boolean, onOpen: () -> Unit, onTime: () -> Unit, showTime: Boolean = true) {
    val look = statusLook(post, goingNow)
    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.medium,
        color = look.container,
        contentColor = look.content,
        border = look.border?.let { BorderStroke(1.5.dp, it) },
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Open ${post.platform.label} post at ${post.time}, ${look.label}" },
    ) {
        Column(Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformBadge(post.platform, size = 24.dp)
                Text(post.platform.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f).padding(start = 8.dp))
                if (showTime) TimeChip(post.time, contentColor = look.content, onClick = onTime, description = "Change time, now ${post.time}")
            }
            Text(
                post.body.lineSequence().first(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 6.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(look.dot))
                Text(" ${look.label}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TimeChip(time: String, contentColor: Color, onClick: () -> Unit, description: String) {
    AssistChip(
        onClick = onClick,
        label = { Text(time, style = NumberStyle.copy(fontSize = 13.sp)) },
        leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
        shape = CircleShape,
        colors = AssistChipDefaults.assistChipColors(labelColor = contentColor, leadingIconContentColor = contentColor),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.3f)),
        modifier = Modifier.semantics { contentDescription = description },
    )
}

@Composable
internal fun SlotCard(slot: CalSlot, onTime: () -> Unit, onRemove: () -> Unit) {
    val dash = MaterialTheme.colorScheme.outline
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = dash,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))),
                )
            }
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.MoreTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text("Open time", style = MaterialTheme.typography.labelLarge)
            Text("Filled from your next check-in", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TimeChip(slot.minutes.asTime(), MaterialTheme.colorScheme.onSurface, onTime, "Change open time, now ${slot.minutes.asTime()}")
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.Close, contentDescription = "Remove open time ${slot.minutes.asTime()}", modifier = Modifier.size(18.dp))
        }
    }
}

// ---------------------------------------------------------------- sheet & dialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PostSheet(
    post: Post,
    goingNow: Boolean,
    onDismiss: () -> Unit,
    onMove: (Int) -> Unit,
    onCustom: () -> Unit,
    onEdit: () -> Unit,
    onPostNow: () -> Unit,
    onCopy: () -> Unit,
    onSkip: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var more by rememberSaveable(post.id) { mutableStateOf(false) }
    val d = CalendarData.days[dayIndex(post.day)]
    val current = post.time.toMinutes()
    val best = CalendarData.best.getValue(post.platform)
    val main = listOf(current, best).distinct().sorted()
    val extra = (CalendarData.quick + best).distinct().sorted().filterNot { it in main }
    val look = statusLook(post, goingNow)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformBadge(post.platform, size = 40.dp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(post.platform.label, style = MaterialTheme.typography.titleMedium)
                    Text("${d.full} · ${d.date} · ${post.time}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreHoriz, contentDescription = "More actions") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Copy text") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = { menu = false; onCopy() })
                        DropdownMenuItem(text = { Text("Skip this post") }, leadingIcon = { Icon(Icons.Rounded.SkipNext, null) }, onClick = { menu = false; onSkip() })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.clip(CircleShape).background(look.container).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(look.dot))
                Text(" ${look.label}", style = MaterialTheme.typography.labelSmall, color = look.content)
            }
            Spacer(Modifier.height(12.dp))
            Text(post.body, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Mic, contentDescription = null, tint = ImpervirTheme.extras.accent, modifier = Modifier.size(16.dp))
                Text(
                    post.source?.let { " From your voice note, ${it.timestamp}" } ?: " From your weekly check-in",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(18.dp))
            SectionTitle("Post time", trailing = { TextButton(onClick = onCustom) { Text("Custom") } })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                main.forEach { m -> TimeOption(m, selected = m == current, best = m == best, onClick = { onMove(m) }) }
                if (!more && extra.isNotEmpty()) AddPill("More times", onClick = { more = true }, modifier = Modifier.padding(top = 4.dp))
            }
            AnimatedVisibility(visible = more, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
                    extra.forEach { m -> TimeOption(m, selected = m == current, best = false, onClick = { onMove(m) }) }
                    IconButton(onClick = { more = false }) { Icon(Icons.Rounded.Close, contentDescription = "Hide extra times", modifier = Modifier.size(18.dp)) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Edit", modifier = Modifier.padding(start = 8.dp))
                }
                Button(onClick = onPostNow, enabled = !goingNow, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                    Text(if (goingNow) "Going out now" else "Post now")
                }
            }
        }
    }
}

@Composable
private fun TimeOption(minutes: Int, selected: Boolean, best: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(minutes.asTime(), style = NumberStyle.copy(fontSize = 13.sp))
            if (best) Text("  Best", style = MaterialTheme.typography.labelSmall, color = ImpervirTheme.extras.accent)
        },
        leadingIcon = if (best) {
            { Icon(Icons.Rounded.Star, contentDescription = null, tint = ImpervirTheme.extras.accent, modifier = Modifier.size(16.dp)) }
        } else null,
        modifier = Modifier.semantics { contentDescription = "Move to ${minutes.asTime()}" + if (best) ", best time" else "" },
    )
}

/** Material 3 time picker (12-hour, AM/PM) in a dialog, with a "Use best time" shortcut for posts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeDialog(title: String, initialMinutes: Int, bestMinutes: Int?, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var start by rememberSaveable { mutableStateOf(initialMinutes) }
    var typing by rememberSaveable { mutableStateOf(false) }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp) {
            key(start) {
                val state = rememberTimePickerState(initialHour = start / 60, initialMinute = start % 60, is24Hour = false)
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                    Text("Pick a time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (typing) TimeInput(state) else TimePicker(state)
                    }
                    if (bestMinutes != null && bestMinutes != state.hour * 60 + state.minute) {
                        TextButton(onClick = { start = bestMinutes }) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = ImpervirTheme.extras.accent, modifier = Modifier.size(16.dp))
                            Text("  Use best time, ${bestMinutes.asTime()}")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { typing = !typing }) {
                            Icon(
                                if (typing) Icons.Rounded.Schedule else Icons.Rounded.Keyboard,
                                contentDescription = if (typing) "Switch to clock" else "Switch to typing",
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        TextButton(onClick = { onSave(state.hour * 60 + state.minute) }) { Text("Save time") }
                    }
                }
            }
        }
    }
}
