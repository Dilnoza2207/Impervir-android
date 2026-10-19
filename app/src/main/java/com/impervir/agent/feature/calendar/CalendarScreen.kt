package com.impervir.agent.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.data.Platform
import com.impervir.agent.data.Post
import com.impervir.agent.data.PostStatus
import com.impervir.agent.navigation.Destination
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.components.AddPill
import com.impervir.agent.ui.components.AddedBlock
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.launch

private val slotsSaver = listSaver<SnapshotStateList<CalSlot>, String>(
    save = { list -> list.map { "${it.id}|${it.day}|${it.minutes}" } },
    restore = { saved -> mutableStateListOf<CalSlot>().apply { saved.forEach { s -> val p = s.split("|"); add(CalSlot(p[0], p[1].toInt(), p[2].toInt())) } } },
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(repository: ImpervirRepository, navigator: Navigator) {
    val drafts by repository.drafts.collectAsStateWithLifecycle()
    val posts = drafts.filter { it.status != PostStatus.Skipped }

    var listView by rememberSaveable { mutableStateOf(false) }
    var sel by rememberSaveable { mutableStateOf(CalendarData.todayIndex) }
    var showBest by rememberSaveable { mutableStateOf(false) }
    val slots = rememberSaveable(saver = slotsSaver) { mutableStateListOf<CalSlot>().apply { addAll(CalendarData.initialSlots) } }
    var nextSlot by rememberSaveable { mutableStateOf(100) }
    var openPost by rememberSaveable { mutableStateOf<String?>(null) }
    var pickPost by rememberSaveable { mutableStateOf<String?>(null) }
    var pickSlot by rememberSaveable { mutableStateOf<String?>(null) }
    var goingNow by rememberSaveable { mutableStateOf(listOf<String>()) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun notify(message: String, onUndo: (() -> Unit)? = null) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(message, actionLabel = onUndo?.let { "Undo" }, withDismissAction = onUndo == null, duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) onUndo?.invoke()
        }
    }

    /** Today's earlier times are gone; everything else is fair game. */
    fun allowed(day: Int, minutes: Int): Boolean {
        if (day == CalendarData.todayIndex && minutes < CalendarData.nowMinutes) {
            notify("${CalendarData.nowMinutes.asTime()} has passed today. Pick a later time")
            return false
        }
        return true
    }

    fun movePost(post: Post, minutes: Int) {
        val day = dayIndex(post.day)
        if (!allowed(day, minutes)) return
        val before = post.time
        repository.reschedule(post.id, post.day, minutes.asTime())
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        notify("Moved to ${post.day} ${minutes.asTime()}") { repository.reschedule(post.id, post.day, before) }
    }

    fun addSlot(day: Int) {
        val used = posts.filter { dayIndex(it.day) == day }.map { it.time.toMinutes() } + slots.filter { it.day == day }.map { it.minutes }
        val slot = CalSlot("n${nextSlot++}", day, CalendarData.suggestSlot(day, used))
        slots.add(slot)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        pickSlot = slot.id
    }

    fun removeSlot(slot: CalSlot) {
        val at = slots.indexOf(slot)
        slots.remove(slot)
        notify("Removed ${CalendarData.days[slot.day].short} ${slot.minutes.asTime()}") {
            slots.add(at.coerceIn(0, slots.size), slot)
        }
    }

    val waiting = posts.count { it.status == PostStatus.NeedsApproval }
    val telegram = posts.count { it.platform == Platform.Telegram }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeTopAppBar(
                title = { BlurRiseText("Calendar", style = LocalTextStyle.current) },
                navigationIcon = {
                    IconButton(onClick = navigator::back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Posts")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            item(key = "head") {
                Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text(CalendarData.weekLabel, style = MaterialTheme.typography.labelMedium, color = ImpervirTheme.extras.muted)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${posts.size}", style = NumberStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize))
                        Text(" posts · ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$waiting", style = NumberStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize))
                        Text(" waiting · ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$telegram", style = NumberStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize))
                        Text(" Telegram", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(14.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(false to "Week", true to "List").forEachIndexed { i, (isList, label) ->
                            SegmentedButton(
                                selected = listView == isList,
                                onClick = { listView = isList; haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                                shape = SegmentedButtonDefaults.itemShape(i, 2),
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            if (!listView) {
                item(key = "pills") {
                    DayPills(posts = posts, selected = sel, onSelect = { sel = it; haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) })
                }
                item(key = "dayhead") {
                    val dayPosts = posts.filter { dayIndex(it.day) == sel }
                    val daySlots = slots.filter { it.day == sel }
                    Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp).animateItem()) {
                        DayHeader(sel, dayPosts, daySlots.size)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AddPill("Add time", onClick = { addSlot(sel) })
                            if (!showBest) AddPill("Best times", onClick = { showBest = true })
                        }
                        AddedBlock(visible = showBest, title = "Best times", onRemove = { showBest = false }, modifier = Modifier.padding(top = 10.dp)) {
                            BestLegend(CalendarData.bands(sel))
                        }
                    }
                }
                val rows = timelineRows(sel, posts, slots)
                items(rows, key = { "h${sel}-${it.hour}" }) { row ->
                    HourRow(
                        row = row,
                        bands = if (showBest) CalendarData.bands(sel) else emptyList(),
                        goingNow = goingNow,
                        onOpen = { openPost = it.id },
                        onPostTime = { pickPost = it.id },
                        onSlotTime = { pickSlot = it.id },
                        onRemoveSlot = ::removeSlot,
                        modifier = Modifier.animateItem(),
                    )
                }
            } else {
                CalendarData.days.forEachIndexed { i, d ->
                    val dayPosts = posts.filter { dayIndex(it.day) == i }
                    val entries = dayEntries(i, posts, slots)
                    item(key = "lh$i") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = if (i == 0) 0.dp else 14.dp, bottom = 6.dp).animateItem(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }, verticalAlignment = Alignment.CenterVertically) {
                                Text("${d.full} · ${d.date}", style = MaterialTheme.typography.titleMedium)
                                if (i == CalendarData.todayIndex) TodayBadge(Modifier.padding(start = 8.dp))
                            }
                            if (dayPosts.isNotEmpty()) {
                                Text(
                                    "${dayPosts.size} post${if (dayPosts.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { addSlot(i) }) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add a posting time on ${d.full}", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    if (entries.isEmpty()) {
                        item(key = "le$i") {
                            Text(
                                "Nothing scheduled",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp).animateItem(),
                            )
                        }
                    }
                    items(entries, key = { e -> e.key }) { e ->
                        Column(Modifier.padding(bottom = 10.dp).animateItem()) {
                            when (e) {
                                is Entry.PostEntry -> CalPostCard(e.post, e.post.id in goingNow, onOpen = { openPost = e.post.id }, onTime = { pickPost = e.post.id }, showTime = true)
                                is Entry.SlotEntry -> SlotCard(e.slot, onTime = { pickSlot = e.slot.id }, onRemove = { removeSlot(e.slot) })
                                Entry.Now -> Unit
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- post sheet ----
    val sheetPost = openPost?.let { id -> posts.firstOrNull { it.id == id } }
    if (sheetPost != null) {
        PostSheet(
            post = sheetPost,
            goingNow = sheetPost.id in goingNow,
            onDismiss = { openPost = null },
            onMove = { m -> movePost(sheetPost, m) },
            onCustom = { pickPost = sheetPost.id },
            onEdit = {
                openPost = null
                navigator.open(Destination.PostEditor(sheetPost.id))
            },
            onPostNow = {
                openPost = null
                goingNow = goingNow + sheetPost.id
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                notify("Posting to ${sheetPost.platform.label} now")
            },
            onCopy = {
                clipboard.setText(AnnotatedString(sheetPost.body))
                notify("Copied post text")
            },
            onSkip = {
                openPost = null
                val prev = sheetPost.status
                repository.skip(sheetPost.id)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                notify("Skipped ${sheetPost.platform.label} post") { repository.undo(sheetPost.id, prev) }
            },
        )
    }

    // ---- time pickers ----
    val pickingPost = pickPost?.let { id -> posts.firstOrNull { it.id == id } }
    if (pickingPost != null) {
        TimeDialog(
            title = "${pickingPost.platform.label} · ${pickingPost.day}",
            initialMinutes = pickingPost.time.toMinutes(),
            bestMinutes = CalendarData.best[pickingPost.platform],
            onDismiss = { pickPost = null },
            onSave = { m ->
                if (allowed(dayIndex(pickingPost.day), m)) {
                    pickPost = null
                    movePost(pickingPost, m)
                }
            },
        )
    }
    val pickingSlot = pickSlot?.let { id -> slots.firstOrNull { it.id == id } }
    if (pickingSlot != null) {
        TimeDialog(
            title = "Open time · ${CalendarData.days[pickingSlot.day].short}",
            initialMinutes = pickingSlot.minutes,
            bestMinutes = null,
            onDismiss = { pickSlot = null },
            onSave = { m ->
                if (allowed(pickingSlot.day, m)) {
                    pickSlot = null
                    val i = slots.indexOfFirst { it.id == pickingSlot.id }
                    if (i >= 0) slots[i] = pickingSlot.copy(minutes = m)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    notify("Open time set for ${CalendarData.days[pickingSlot.day].short} ${m.asTime()}")
                }
            },
        )
    }
}

@Composable
private fun DayHeader(day: Int, dayPosts: List<Post>, openTimes: Int) {
    val d = CalendarData.days[day]
    Row(Modifier.semantics(mergeDescendants = true) { heading() }, verticalAlignment = Alignment.CenterVertically) {
        Text("${d.full}, ${d.date}", style = MaterialTheme.typography.titleLarge)
        if (day == CalendarData.todayIndex) TodayBadge(Modifier.padding(start = 8.dp))
    }
    val waiting = dayPosts.count { it.status == PostStatus.NeedsApproval }
    val text = buildString {
        append(if (dayPosts.isEmpty()) "No posts" else "${dayPosts.size} post${if (dayPosts.size > 1) "s" else ""}")
        if (waiting > 0) append(" · $waiting waiting")
        if (openTimes > 0) append(" · $openTimes open time${if (openTimes > 1) "s" else ""}")
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
