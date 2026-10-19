package com.impervir.agent.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.impervir.agent.data.Platform

/** Connection state of an account or store row. */
internal enum class Conn { Idle, Busy, On, NeedsReconnect }

internal val DayShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
internal val DayFull = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** One day of the posting schedule: on/off and its own time (minutes since midnight). */
internal data class ScheduleDay(val on: Boolean, val minutes: Int)

private val defaultSchedule = listOf(
    ScheduleDay(true, 8 * 60 + 30),   // Mon 8:30 AM
    ScheduleDay(false, 9 * 60),       // Tue
    ScheduleDay(true, 12 * 60),       // Wed 12:00 PM
    ScheduleDay(false, 9 * 60),       // Thu
    ScheduleDay(true, 18 * 60),       // Fri 6:00 PM
    ScheduleDay(false, 10 * 60),      // Sat
    ScheduleDay(true, 10 * 60 + 30),  // Sun 10:30 AM
)

/** Posts-per-week limits for each platform (the stepper bounds keep accounts safe). */
internal data class PerWeek(val platform: Platform, val sub: String, val min: Int, val max: Int, val default: Int)

internal val perWeekDefs = listOf(
    PerWeek(Platform.LinkedIn, "2 to 5 a week, rarely links", 2, 5, 3),
    PerWeek(Platform.X, "Never the same text twice", 1, 7, 3),
    PerWeek(Platform.Threads, "Its own style", 1, 7, 3),
    PerWeek(Platform.Telegram, "Safe place for direct sales", 1, 7, 4),
)

internal enum class NotifPref(val label: String, val sub: String) {
    Drafts("Drafts ready", "When this week's posts are written"),
    Comments("New comments", "Nudges you in the first hour"),
    Sales("Sales", "Each sale, credited to its post"),
    Weekly("Weekly report", "Sunday, in dollars"),
}

/** Plain state holder for the Settings screen; saved across rotation with [Saver]. */
@Stable
internal class SettingsState {
    var threads by mutableStateOf(Conn.NeedsReconnect)
    var gumroad by mutableStateOf(Conn.Idle)
    var lemon by mutableStateOf(Conn.Idle)
    var showMoreStores by mutableStateOf(false)

    var telegramAuto by mutableStateOf(true)
    var reshares by mutableStateOf(true)
    var threadsAuto by mutableStateOf(false)

    val schedule = mutableStateListOf<ScheduleDay>().apply { addAll(defaultSchedule) }

    var checkInDay by mutableIntStateOf(0)
    var checkInMinutes by mutableIntStateOf(9 * 60)

    var wallpaperColors by mutableStateOf(false)

    val notifications = mutableStateListOf(*NotifPref.entries.toTypedArray())

    var showPerWeek by mutableStateOf(false)
    val perWeek = mutableStateListOf<Int>().apply { addAll(perWeekDefs.map { it.default }) }

    val daysOn: Int get() = schedule.count { it.on }

    fun telegramSummary(): String =
        if (daysOn == 0) "Turn on a day in Posting schedule"
        else schedule.mapIndexedNotNull { i, d -> if (d.on) "${DayShort[i]} ${formatTime(d.minutes)}" else null }.joinToString(", ")

    companion object {
        val Saver: Saver<SettingsState, Any> = Saver(
            save = { s ->
                arrayListOf<Any>(
                    s.threads.name, s.gumroad.name, s.lemon.name, s.showMoreStores,
                    s.telegramAuto, s.reshares, s.threadsAuto,
                    s.schedule.joinToString(";") { "${it.on},${it.minutes}" },
                    s.checkInDay, s.checkInMinutes, s.wallpaperColors,
                    s.notifications.joinToString(",") { it.name },
                    s.showPerWeek, s.perWeek.joinToString(","),
                )
            },
            restore = { raw ->
                @Suppress("UNCHECKED_CAST")
                val v = raw as List<Any>
                SettingsState().apply {
                    threads = Conn.valueOf(v[0] as String).settled()
                    gumroad = Conn.valueOf(v[1] as String).settled()
                    lemon = Conn.valueOf(v[2] as String).settled()
                    showMoreStores = v[3] as Boolean
                    telegramAuto = v[4] as Boolean
                    reshares = v[5] as Boolean
                    threadsAuto = v[6] as Boolean
                    schedule.clear()
                    schedule.addAll((v[7] as String).split(";").map { p -> p.split(",").let { ScheduleDay(it[0].toBoolean(), it[1].toInt()) } })
                    checkInDay = v[8] as Int
                    checkInMinutes = v[9] as Int
                    wallpaperColors = v[10] as Boolean
                    notifications.clear()
                    notifications.addAll((v[11] as String).split(",").filter { it.isNotBlank() }.map { NotifPref.valueOf(it) })
                    showPerWeek = v[12] as Boolean
                    perWeek.clear()
                    perWeek.addAll((v[13] as String).split(",").map { it.toInt() })
                }
            },
        )

        /** A connection that was mid-flight when the screen was recreated is treated as done. */
        private fun Conn.settled() = if (this == Conn.Busy) Conn.On else this
    }
}

@Composable
internal fun rememberSettingsState(): SettingsState = rememberSaveable(saver = SettingsState.Saver) { SettingsState() }

/** "8:30 AM" from minutes since midnight. */
internal fun formatTime(minutes: Int): String {
    val h24 = (minutes / 60) % 24
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    return "$h12:${(minutes % 60).toString().padStart(2, '0')} ${if (h24 < 12) "AM" else "PM"}"
}
