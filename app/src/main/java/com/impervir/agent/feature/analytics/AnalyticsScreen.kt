package com.impervir.agent.feature.analytics

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.data.SampleData
import com.impervir.agent.navigation.Destination
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.components.AddPill
import com.impervir.agent.ui.components.AddedBlock
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.components.CountUpText
import com.impervir.agent.ui.components.ImpervirCard
import com.impervir.agent.ui.components.PlatformBadge
import com.impervir.agent.ui.components.SectionTitle
import com.impervir.agent.ui.components.grouped
import com.impervir.agent.ui.components.money
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(repository: ImpervirRepository, navigator: Navigator, contentPadding: PaddingValues) {
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var range by rememberSaveable { mutableStateOf(Range.Week) }
    var moreStats by rememberSaveable { mutableStateOf(false) }
    var moreInsights by rememberSaveable { mutableStateOf(false) }
    var allPosts by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(PostSort.Revenue) }
    var platformMetric by rememberSaveable { mutableStateOf(PlatformMetric.Revenue) }
    var charts by rememberSaveable { mutableStateOf(listOf<String>()) } // ChartKind names that are on
    var customizeOpen by rememberSaveable { mutableStateOf(false) }
    var kpiOpen by rememberSaveable { mutableStateOf<Int?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    fun flash(msg: String) = scope.launch {
        snackbar.currentSnackbarData?.dismiss()
        snackbar.showSnackbar(msg)
    }
    fun isOn(k: ChartKind) = k.name in charts
    fun setChart(k: ChartKind, on: Boolean) {
        charts = if (on) (charts + k.name).distinct() else charts - k.name
    }

    val data = rangeData.getValue(range)

    Scaffold(
        modifier = Modifier
            .padding(bottom = contentPadding.calculateBottomPadding())
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeTopAppBar(
                title = { BlurRiseText("Analytics", style = LocalTextStyle.current, modifier = Modifier.semantics { heading() }) },
                actions = {
                    TextButton(onClick = { navigator.open(Destination.Report) }) {
                        Icon(Icons.Rounded.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Report", modifier = Modifier.padding(start = 6.dp))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More analytics options") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Share report") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = {
                                menuOpen = false; flash("Report link copied. Send it to anyone.")
                            })
                            DropdownMenuItem(text = { Text("Export CSV") }, leadingIcon = { Icon(Icons.Rounded.Download, null) }, onClick = {
                                menuOpen = false; flash("CSV download started. Week 42, 7 posts.")
                            })
                            DropdownMenuItem(text = { Text("Copy summary") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = {
                                menuOpen = false
                                clipboard.setText(AnnotatedString(summaryText))
                                flash("Summary copied. Paste it anywhere.")
                            })
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "intro") {
                Column(Modifier.padding(horizontal = 4.dp)) {
                    Text("Week 42 · ${SampleData.lastWeekRange}", style = NumberStyle.copy(fontSize = 13.sp), color = ImpervirTheme.extras.muted)
                    Text("Last week's sales, and what to do next.", style = MaterialTheme.typography.bodyMedium, color = ImpervirTheme.extras.muted)
                }
            }
            item(key = "range") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Range.entries.forEachIndexed { i, r ->
                        SegmentedButton(
                            selected = range == r,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                range = r
                            },
                            shape = SegmentedButtonDefaults.itemShape(i, Range.entries.size),
                            icon = {},
                        ) { Text(r.label) }
                    }
                }
            }
            item(key = "hero") { HeroCard(data, range) }
            item(key = "tiles") {
                StatTiles(data, range, moreStats, onOpen = { kpiOpen = it })
            }
            item(key = "morestats") {
                Box(Modifier.fillMaxWidth()) {
                    if (!moreStats) AddPill("More stats", onClick = { moreStats = true })
                    else TextButton(onClick = { moreStats = false }) { Text("Show fewer stats") }
                }
            }
            item(key = "top") {
                TopPosts(
                    sort = sort,
                    onSort = { sort = it },
                    all = allPosts,
                    onAll = { allPosts = !allPosts },
                    onOpen = { navigator.open(Destination.PostDetail(it)) },
                )
            }
            item(key = "insights") {
                Insights(
                    more = moreInsights,
                    onMore = { moreInsights = it },
                    onAction = { insight ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        flash(insight.confirm)
                    },
                )
            }
            ChartKind.entries.forEach { k ->
                item(key = "chart-${k.name}") {
                    AddedBlock(visible = isOn(k), title = k.label, onRemove = { setChart(k, false) }) {
                        when (k) {
                            ChartKind.RevenueByWeek -> RevenueByWeekChart()
                            ChartKind.Funnel -> FunnelChart()
                            ChartKind.ByPlatform -> {
                                MetricPicker(platformMetric) { platformMetric = it }
                                PlatformDonut(platformMetric)
                            }
                            ChartKind.WhatSells -> WhatSellsChart()
                            ChartKind.Timing -> TimingHeatmap()
                            ChartKind.Voice -> {
                                EditRateChart()
                                TextButton(onClick = { navigator.open(Destination.YourVoice) }, modifier = Modifier.padding(top = 4.dp)) {
                                    Text("See what I learned about you")
                                }
                            }
                        }
                    }
                }
            }
            item(key = "customize") {
                val on = charts.size
                Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { customizeOpen = true }) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Customize", modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(
                        if (on > 0) "$on of ${ChartKind.entries.size} extra charts shown" else "${ChartKind.entries.size} more charts you can add",
                        style = NumberStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        color = ImpervirTheme.extras.muted,
                    )
                }
            }
        }
    }

    if (customizeOpen) {
        CustomizeSheet(
            isOn = ::isOn,
            onToggle = { k, v ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                setChart(k, v)
            },
            onDismiss = { customizeOpen = false },
        )
    }
    kpiOpen?.let { i -> KpiSheet(kpis[i]) { kpiOpen = null } }

    BackHandler(enabled = menuOpen) { menuOpen = false }
}

@Composable
private fun HeroCard(data: RangeData, range: Range) {
    val extras = ImpervirTheme.extras
    ImpervirCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Your posts made", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(data.name, style = MaterialTheme.typography.labelMedium, color = extras.muted)
        }
        CountUpText(
            value = data.revenue,
            format = { it.money() },
            style = NumberStyle.copy(fontSize = 56.sp, lineHeight = 60.sp),
            durationMs = 1100,
            modifier = Modifier.padding(top = 6.dp).semantics { contentDescription = "${data.revenue.money()} ${data.name}" },
        )
        AnimatedContent(
            targetState = range,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
            label = "heroPill",
        ) { r ->
            val d = rangeData.getValue(r)
            Column {
                Row(
                    Modifier
                        .padding(top = 4.dp)
                        .background(extras.positive.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = extras.positive, modifier = Modifier.size(14.dp))
                    Text(d.pill, style = NumberStyle.copy(fontSize = 13.sp), color = extras.positive, modifier = Modifier.padding(start = 6.dp))
                }
                Text(d.note, style = MaterialTheme.typography.bodySmall, color = extras.muted, modifier = Modifier.padding(top = 8.dp))
            }
        }
        HeroSparkline(Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun StatTiles(data: RangeData, range: Range, more: Boolean, onOpen: (Int) -> Unit) {
    val shown = if (more) kpis.indices.toList() else listOf(1, 2)
    Column(Modifier.animateContentSize(Motion.soft()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        shown.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { i ->
                    val k = kpis[i]
                    val weekly = range == Range.Week || i >= 4
                    StatTile(k, k.value(data), if (weekly) k.weekChip else data.name, weekly, Modifier.weight(1f)) { onOpen(i) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(k: Kpi, value: String, chip: String, highlight: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val extras = ImpervirTheme.extras
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 132.dp).semantics { contentDescription = "${k.title}: $value. $chip. Tap for what this means." },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = extras.card),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(k.title, style = MaterialTheme.typography.labelLarge)
            AnimatedContent(targetState = value, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "kpi") { v ->
                Text(v, style = NumberStyle.copy(fontSize = 30.sp), modifier = Modifier.padding(top = 4.dp))
            }
            Text(
                chip,
                style = NumberStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                color = if (highlight) extras.positive else extras.muted,
            )
            Text(k.caption, style = MaterialTheme.typography.bodySmall, color = extras.muted, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

private data class TopPost(val id: String, val title: String, val meta: String, val revenue: Int, val views: Int, val clicks: Int, val platform: com.impervir.agent.data.Platform)

private val topPosts = SampleData.week42Posts.map { p ->
    val s = p.stats!!
    val title = if (p.platform == com.impervir.agent.data.Platform.Telegram) "Telegram: ${p.title.replaceFirstChar { it.lowercase() }}" else p.title
    TopPost(p.id, title, "${p.platform.label} · ${p.day} ${p.time} · ${s.orders} orders", s.revenue, s.views, s.clicks, p.platform)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopPosts(sort: PostSort, onSort: (PostSort) -> Unit, all: Boolean, onAll: () -> Unit, onOpen: (String) -> Unit) {
    val extras = ImpervirTheme.extras
    var sortMenu by remember { mutableStateOf(false) }
    val sorted = topPosts.sortedByDescending {
        when (sort) { PostSort.Revenue -> it.revenue; PostSort.Views -> it.views; PostSort.Clicks -> it.clicks }
    }
    val shown = if (all) sorted else sorted.take(4)
    ImpervirCard(Modifier.fillMaxWidth()) {
        SectionTitle("Top posts", Modifier.semantics { heading() }) {
            Box {
                TextButton(onClick = { sortMenu = true }) {
                    Text(sort.label)
                    Icon(Icons.Rounded.ExpandMore, contentDescription = "Sort posts by", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    PostSort.entries.forEach { s ->
                        DropdownMenuItem(text = { Text(s.label) }, onClick = { sortMenu = false; onSort(s) })
                    }
                }
            }
        }
        Text(sort.caption, style = MaterialTheme.typography.bodySmall, color = extras.muted)
        Column(Modifier.padding(top = 8.dp).animateContentSize(Motion.soft())) {
            shown.forEachIndexed { i, p ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val main = when (sort) {
                    PostSort.Revenue -> p.revenue.money()
                    PostSort.Views -> p.views.grouped()
                    PostSort.Clicks -> p.clicks.grouped()
                }
                val top = i == 0
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(p.id) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlatformBadge(p.platform, size = 32.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(p.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        Text(p.meta, style = MaterialTheme.typography.bodySmall, color = extras.muted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(main, style = NumberStyle.copy(fontSize = 17.sp), color = if (top) extras.accent else MaterialTheme.colorScheme.onSurface)
                        Text(sort.label.lowercase(), style = MaterialTheme.typography.labelSmall, color = extras.muted)
                    }
                }
            }
        }
        TextButton(onClick = onAll) { Text(if (all) "Show top 4" else "Show all ${topPosts.size}") }
    }
}

@Composable
private fun Insights(more: Boolean, onMore: (Boolean) -> Unit, onAction: (Insight) -> Unit) {
    val extras = ImpervirTheme.extras
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("What I noticed", Modifier.padding(horizontal = 4.dp).semantics { heading() })
        InsightCard(insights[0], onAction)
        AnimatedVisibility(
            visible = more,
            enter = expandVertically(tween(280)) + fadeIn(tween(280)),
            exit = shrinkVertically(tween(220)) + fadeOut(tween(160)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                insights.drop(1).forEach { InsightCard(it, onAction) }
            }
        }
        Box {
            if (!more) AddPill("2 more insights", onClick = { onMore(true) })
            else TextButton(onClick = { onMore(false) }) { Text("Show fewer", color = extras.muted) }
        }
    }
}

@Composable
private fun InsightCard(insight: Insight, onAction: (Insight) -> Unit) {
    val extras = ImpervirTheme.extras
    ImpervirCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(28.dp).background(extras.accent.copy(alpha = 0.14f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = extras.accent, modifier = Modifier.size(16.dp)) }
            Text("From your week", style = MaterialTheme.typography.labelMedium, color = extras.muted, modifier = Modifier.padding(start = 10.dp))
        }
        Text(insight.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
        Button(onClick = { onAction(insight) }, modifier = Modifier.padding(top = 12.dp)) { Text(insight.action) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricPicker(metric: PlatformMetric, onPick: (PlatformMetric) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        PlatformMetric.entries.forEachIndexed { i, m ->
            SegmentedButton(
                selected = metric == m,
                onClick = { onPick(m) },
                shape = SegmentedButtonDefaults.itemShape(i, PlatformMetric.entries.size),
                icon = {},
            ) { Text(m.label, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizeSheet(isOn: (ChartKind) -> Boolean, onToggle: (ChartKind, Boolean) -> Unit, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Text("Customize", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() })
            Text(
                "Revenue and your top posts always stay. Add the charts you want.",
                style = MaterialTheme.typography.bodyMedium,
                color = ImpervirTheme.extras.muted,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            )
            ChartKind.entries.forEach { k ->
                val on = isOn(k)
                ListItem(
                    headlineContent = { Text(k.label, style = MaterialTheme.typography.titleSmall) },
                    supportingContent = { Text(k.sub) },
                    trailingContent = { Switch(checked = on, onCheckedChange = null) },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    modifier = Modifier.clickable { onToggle(k, !on) }.padding(horizontal = 8.dp),
                )
            }
            Button(
                onClick = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).height(52.dp),
            ) { Text("Done") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KpiSheet(k: Kpi, onDismiss: () -> Unit) {
    val extras = ImpervirTheme.extras
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 12.dp)) {
            Text(k.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text("What this means", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            Text(k.meaning, style = MaterialTheme.typography.bodyMedium, color = extras.muted, modifier = Modifier.padding(top = 4.dp))
            Text("How it's counted", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            Text(k.counted, style = MaterialTheme.typography.bodyMedium, color = extras.muted, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium).padding(14.dp)) {
                    Text("This week (W42)", style = MaterialTheme.typography.labelMedium, color = extras.muted)
                    Text(k.now, style = NumberStyle.copy(fontSize = 26.sp), color = extras.accent)
                }
                Column(Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium).padding(14.dp)) {
                    Text("Last week (W41)", style = MaterialTheme.typography.labelMedium, color = extras.muted)
                    Text(k.prev, style = NumberStyle.copy(fontSize = 26.sp))
                }
            }
            Text(k.delta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
            Button(
                onClick = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(52.dp),
            ) { Text("Got it") }
        }
    }
}
