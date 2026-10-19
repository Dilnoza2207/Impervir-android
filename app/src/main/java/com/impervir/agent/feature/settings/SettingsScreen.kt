package com.impervir.agent.feature.settings

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.data.Platform
import com.impervir.agent.data.SampleData
import com.impervir.agent.data.Store
import com.impervir.agent.navigation.Navigator
import com.impervir.agent.ui.components.AddPill
import com.impervir.agent.ui.components.AddedBlock
import com.impervir.agent.ui.components.Avatar
import com.impervir.agent.ui.components.BlurRiseText
import com.impervir.agent.ui.components.ImpervirCard
import com.impervir.agent.ui.components.PlatformBadge
import com.impervir.agent.ui.components.SectionTitle
import com.impervir.agent.ui.components.StoreBadge
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Which time the picker dialog is editing: a schedule day (0..6) or the weekly check-in. */
private const val PickNone = -1
private const val PickCheckIn = 7

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repository: ImpervirRepository, navigator: Navigator) {
    val state = rememberSettingsState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var pickFor by rememberSaveable { mutableIntStateOf(PickNone) }
    var sheet by rememberSaveable { mutableStateOf("") } // "", "checkin", "notifs"
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    fun snack(msg: String, action: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(msg, action, withDismissAction = action == null) == SnackbarResult.ActionPerformed) onAction()
        }
    }

    fun connect(get: () -> Conn, set: (Conn) -> Unit, done: String) {
        if (get() == Conn.Busy || get() == Conn.On) return
        set(Conn.Busy)
        scope.launch {
            delay(1100)
            set(Conn.On)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            snack(done)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { BlurRiseText("Settings", style = LocalTextStyle.current) },
                navigationIcon = {
                    IconButton(onClick = { navigator.back() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Profile
            ImpervirCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(SampleData.userInitials, size = 52.dp)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(SampleData.userName, style = MaterialTheme.typography.titleMedium)
                        Text("Notion templates · Beta plan", style = MaterialTheme.typography.bodyMedium, color = ImpervirTheme.extras.muted)
                    }
                }
            }

            // Accounts
            Section("Accounts")
            SettingsCard {
                ConnRow({ PlatformBadge(Platform.X, size = 40.dp) }, "X", "@mayaplans", Conn.On)
                RowDivider()
                ConnRow({ PlatformBadge(Platform.LinkedIn, size = 40.dp) }, "LinkedIn", "Maya Chen", Conn.On)
                RowDivider()
                ConnRow(
                    { PlatformBadge(Platform.Threads, size = 40.dp) }, "Threads",
                    if (state.threads == Conn.NeedsReconnect) "@mayaplans · sign-in expired" else "@mayaplans",
                    state.threads, "Reconnect",
                ) { connect({ state.threads }, { state.threads = it }, "Threads is connected again") }
                RowDivider()
                ConnRow({ PlatformBadge(Platform.Telegram, size = 40.dp) }, "Telegram channel", "t.me/mayaplans", Conn.On)
            }

            // Stores
            Section("Stores", "Where your sales come from")
            SettingsCard(Modifier.animateContentSize(Motion.soft())) {
                ConnRow({ StoreBadge(Store.Stripe, size = 40.dp) }, "Stripe", "Tracking links credit each sale to its post", Conn.On)
                val showGum = state.showMoreStores || state.gumroad == Conn.On
                val showLemon = state.showMoreStores || state.lemon == Conn.On
                if (showGum) {
                    RowDivider()
                    ConnRow({ StoreBadge(Store.Gumroad, size = 40.dp) }, "Gumroad", "Tracking links credit each sale", state.gumroad, "Connect") {
                        connect({ state.gumroad }, { state.gumroad = it }, "Gumroad is connected")
                    }
                }
                if (showLemon) {
                    RowDivider()
                    ConnRow({ StoreBadge(Store.LemonSqueezy, size = 40.dp) }, "Lemon Squeezy", "Tracking links credit each sale", state.lemon, "Connect") {
                        connect({ state.lemon }, { state.lemon = it }, "Lemon Squeezy is connected")
                    }
                }
            }
            val unconnectedStores = listOf(state.gumroad, state.lemon).count { it != Conn.On }
            if (!state.showMoreStores && unconnectedStores > 0) {
                AddPill("Add a store", onClick = { state.showMoreStores = true })
            } else if (state.showMoreStores && unconnectedStores > 0) {
                TextButton(onClick = { state.showMoreStores = false }) { Text("Hide other stores") }
            }

            // Autopilot limits
            Section("What I do on my own")
            GroupLabel("Posts on its own")
            SettingsCard {
                ToggleRow("Telegram channel posts", state.telegramSummary(), state.telegramAuto) { state.telegramAuto = it }
                RowDivider()
                ToggleRow("Re-shares of approved posts", "Only posts you already approved", state.reshares) { state.reshares = it }
            }
            GroupLabel("Asks you first")
            SettingsCard {
                InfoRow("New LinkedIn and X posts", "You approve every one", "Always asks")
                RowDivider()
                ToggleRow("New Threads posts", "Auto once I've earned your trust", state.threadsAuto) { state.threadsAuto = it }
                RowDivider()
                InfoRow("Anything about prices or offers", "Always your call", "Always asks")
            }
            GroupLabel("Never does")
            SettingsCard {
                val onLock = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    snack("Locked on purpose, to keep your accounts safe")
                }
                LockRow("Comments or replies on your behalf", "Drafts for you to send, never sent", onLock)
                RowDivider()
                LockRow("Likes and follows", "Your account stays yours", onLock)
                RowDivider()
                LockRow("Same text on every platform", "Each platform gets its own post", onLock)
            }
            LockNote()

            // Posting schedule
            Section("Posting schedule", "Pick a time for each day. Tap a time to change it.")
            SettingsCard {
                state.schedule.forEachIndexed { i, day ->
                    if (i > 0) RowDivider()
                    ScheduleRow(
                        dayName = DayFull[i],
                        day = day,
                        onTime = { pickFor = i },
                        onToggle = { on ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            state.schedule[i] = day.copy(on = on)
                        },
                    )
                }
            }
            Text(
                buildString {
                    append(
                        when (state.daysOn) {
                            0 -> "No days on"
                            1 -> "1 day a week"
                            else -> "${state.daysOn} days a week"
                        },
                    )
                    append(" · approved posts go out at these times.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = ImpervirTheme.extras.muted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            // Weekly check-in
            Section("Weekly check-in")
            SettingsCard {
                NavRow("Two-minute check-in", "${DayFull[state.checkInDay]} · ${formatTime(state.checkInMinutes)}") { sheet = "checkin" }
            }

            // Notifications (details in a sheet)
            Section("Notifications")
            SettingsCard {
                NavRow("Notifications", "${state.notifications.size} of ${NotifPref.entries.size} on") { sheet = "notifs" }
            }

            // Appearance (Android only)
            Section("Appearance")
            SettingsCard {
                val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ToggleRow(
                    "Use wallpaper colors",
                    if (supported) "Match the app to your wallpaper instead of Impervir orange" else "Needs Android 12 or newer",
                    state.wallpaperColors && supported,
                    enabled = supported,
                ) { state.wallpaperColors = it }
            }

            // Optional: posts per week
            AddedBlock(visible = state.showPerWeek, title = "Posts per week", onRemove = { state.showPerWeek = false }) {
                perWeekDefs.forEachIndexed { i, def ->
                    if (i > 0) RowDivider()
                    StepperRow(def, state.perWeek[i]) { state.perWeek[i] = it.coerceIn(def.min, def.max) }
                }
            }
            if (!state.showPerWeek) {
                Section("More settings", "Optional. The defaults work for most people.")
                AddPill("Posts per week", onClick = { state.showPerWeek = true })
            }

            // Plan
            Section("Plan")
            ImpervirCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Beta", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(50), color = ImpervirTheme.extras.positive.copy(alpha = 0.14f)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(ImpervirTheme.extras.positive))
                            Text("Current plan", style = MaterialTheme.typography.labelMedium, color = ImpervirTheme.extras.positive, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
                Text(
                    "Early access to every feature while we build.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ImpervirTheme.extras.muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
                OutlinedButton(onClick = { snack("Opens your plan page") }, modifier = Modifier.padding(top = 14.dp)) { Text("Manage plan") }
            }

            // Sign out (separate, never next to other actions)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { confirmSignOut = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign out")
            }
        }
    }

    // Dialogs and sheets
    if (pickFor != PickNone) {
        val isCheckIn = pickFor == PickCheckIn
        val dayIndex = pickFor
        TimePickerDialog(
            title = if (isCheckIn) "Check-in time" else "${DayFull[dayIndex]} post time",
            initialMinutes = if (isCheckIn) state.checkInMinutes else state.schedule[dayIndex].minutes,
            onDismiss = { pickFor = PickNone },
            onConfirm = { m ->
                if (isCheckIn) state.checkInMinutes = m
                else {
                    state.schedule[dayIndex] = state.schedule[dayIndex].copy(minutes = m)
                    snack("${DayFull[dayIndex]} posts go out at ${formatTime(m)}")
                }
                pickFor = PickNone
            },
        )
    }
    when (sheet) {
        "checkin" -> CheckInSheet(
            state = state,
            onPickTime = { pickFor = PickCheckIn },
            onDismiss = { sheet = "" },
        )
        "notifs" -> NotificationsSheet(state = state, onDismiss = { sheet = "" })
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("Your agent keeps your drafts and schedule. Sign in again any time.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    snack("Signed out on this phone")
                }) { Text("Sign out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
}

// ------------------------------------------------------------------ building blocks

@Composable
private fun Section(title: String, sub: String? = null) {
    Column(Modifier.padding(start = 4.dp, top = 14.dp, end = 4.dp)) {
        SectionTitle(title, Modifier.semantics { heading() })
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = ImpervirTheme.extras.muted,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp).semantics { heading() },
    )
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = ImpervirTheme.extras.card, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun RowText(label: String, sub: String?, modifier: Modifier = Modifier, muted: Boolean = false) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = if (muted) ImpervirTheme.extras.muted else MaterialTheme.colorScheme.onSurface)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted)
    }
}

@Composable
private fun ConnRow(
    leading: @Composable () -> Unit,
    name: String,
    sub: String,
    conn: Conn,
    buttonLabel: String = "Connect",
    onConnect: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        RowText(name, sub, Modifier.weight(1f).padding(horizontal = 12.dp))
        AnimatedContent(conn, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "conn") { c ->
            when (c) {
                Conn.On -> ConnectedChip()
                Conn.Busy -> CircularProgressIndicator(Modifier.size(24.dp).semantics { contentDescription = "Connecting" }, strokeWidth = 2.5.dp)
                Conn.Idle, Conn.NeedsReconnect -> FilledTonalButton(onClick = onConnect) { Text(buttonLabel) }
            }
        }
    }
}

@Composable
private fun ConnectedChip() {
    val pos = ImpervirTheme.extras.positive
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(pos))
        Text("Connected", style = MaterialTheme.typography.labelMedium, color = pos, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun ToggleRow(label: String, sub: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(label, sub, Modifier.weight(1f).padding(end = 12.dp), muted = !enabled)
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun InfoRow(label: String, sub: String, trailing: String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(label, sub, Modifier.weight(1f).padding(end = 12.dp))
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun NavRow(label: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(label, sub, Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = ImpervirTheme.extras.muted)
    }
}

/** A limit that can't be turned on. Tapping it shakes and explains why. */
@Composable
private fun LockRow(label: String, sub: String, onTap: () -> Unit) {
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClickLabel = "Why is this locked") {
                onTap()
                scope.launch { for (x in listOf(-8f, 8f, -6f, 6f, -3f, 0f)) shake.animateTo(x, tween(45)) }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { stateDescription = "Locked on purpose" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) }.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) }
        RowText(label, sub, Modifier.weight(1f).padding(horizontal = 12.dp))
        Text("Never", style = MaterialTheme.typography.labelLarge, color = ImpervirTheme.extras.muted)
    }
}

/** The readable explanation under the locked limits. */
@Composable
private fun LockNote() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    ) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(20.dp).padding(top = 2.dp))
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    "Locked on purpose. These keep your accounts safe from bans.",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "LinkedIn and X ban or limit accounts that automate these. Nothing is broken.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ScheduleRow(dayName: String, day: ScheduleDay, onTime: () -> Unit, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            dayName,
            style = MaterialTheme.typography.titleSmall,
            color = if (day.on) MaterialTheme.colorScheme.onSurface else ImpervirTheme.extras.muted,
            modifier = Modifier.weight(1f),
        )
        AnimatedContent(day.on, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "dayOn") { on ->
            if (on) {
                FilledTonalButton(
                    onClick = onTime,
                    modifier = Modifier.semantics { contentDescription = "Change $dayName time, now ${formatTime(day.minutes)}" },
                ) {
                    Text(formatTime(day.minutes), style = NumberStyle.copy(fontSize = 15.sp))
                }
            } else {
                Text("No post", style = MaterialTheme.typography.bodyMedium, color = ImpervirTheme.extras.muted, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = day.on,
            onCheckedChange = onToggle,
            modifier = Modifier.semantics { contentDescription = "Post on $dayName" },
        )
    }
}

@Composable
private fun StepperRow(def: PerWeek, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        PlatformBadge(def.platform, size = 36.dp)
        RowText(def.platform.label, def.sub, Modifier.weight(1f).padding(horizontal = 12.dp))
        OutlinedIconButton(onClick = { onChange(value - 1) }, enabled = value > def.min) {
            Icon(Icons.Rounded.Remove, contentDescription = "Fewer ${def.platform.label} posts")
        }
        AnimatedContent(value, label = "perWeek") { v ->
            Text(
                "$v",
                style = NumberStyle.copy(fontSize = 18.sp),
                modifier = Modifier.width(32.dp).semantics { liveRegion = LiveRegionMode.Polite },
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        OutlinedIconButton(onClick = { onChange(value + 1) }, enabled = value < def.max) {
            Icon(Icons.Rounded.Add, contentDescription = "More ${def.platform.label} posts")
        }
    }
}
