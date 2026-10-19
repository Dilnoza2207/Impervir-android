package com.impervir.agent.feature.calendar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.impervir.agent.data.Platform
import com.impervir.agent.data.SampleData

/** An open posting time the agent fills from the next check-in. Minutes since midnight. */
@Immutable
internal data class CalSlot(val id: String, val day: Int, val minutes: Int)

internal data class CalDay(val short: String, val full: String, val date: String, val number: Int)

/** A best-time band on the day timeline. */
internal data class Band(val platform: Platform, val from: Int, val to: Int, val label: String, val color: Color)

/** What the time picker is editing. */
internal sealed interface PickTarget {
    data class ForPost(val id: String) : PickTarget
    data class ForSlot(val id: String) : PickTarget
}

internal object CalendarData {
    const val weekLabel = "Week 43 · Oct 20 to 26"
    const val todayIndex = 0
    /** "Now" on the prototype's Monday: 9:40 AM. */
    const val nowMinutes = 580

    val days = listOf(
        CalDay("Mon", "Monday", "Oct 20", 20), CalDay("Tue", "Tuesday", "Oct 21", 21), CalDay("Wed", "Wednesday", "Oct 22", 22),
        CalDay("Thu", "Thursday", "Oct 23", 23), CalDay("Fri", "Friday", "Oct 24", 24), CalDay("Sat", "Saturday", "Oct 25", 25),
        CalDay("Sun", "Sunday", "Oct 26", 26),
    )

    /** Best minute of the day per platform (agrees with Posts and the editor). */
    val best = mapOf(Platform.LinkedIn to 550, Platform.X to 480, Platform.Threads to 750, Platform.Telegram to 1080)

    /** Extra quick times offered behind "+ More times". */
    val quick = listOf(465, 510, 750, 1080)

    // Calm band colors, one per platform (dots on the day pills use the same).
    val bandColor = mapOf(
        Platform.LinkedIn to Color(0xFF8C7BFF),
        Platform.X to Color(0xFF3DDC97),
        Platform.Threads to Color(0xFFFF9F7A),
        Platform.Telegram to Color(0xFF9A96A0),
    )

    fun bands(day: Int): List<Band> = buildList {
        if (day == 1 || day == 2) add(Band(Platform.LinkedIn, 480, 600, "9 AM", bandColor.getValue(Platform.LinkedIn)))
        add(Band(Platform.X, 480, 540, "8 AM", bandColor.getValue(Platform.X)))
        add(Band(Platform.Threads, 720, 780, "12 PM", bandColor.getValue(Platform.Threads)))
        add(Band(Platform.Telegram, 1080, 1140, "6 PM", bandColor.getValue(Platform.Telegram)))
    }

    val initialSlots: List<CalSlot> = SampleData.openSlots.mapIndexed { i, s -> CalSlot("s$i", dayIndex(s.day), s.time.toMinutes()) }

    fun suggestSlot(day: Int, used: List<Int>): Int {
        val opts = if (day == todayIndex) listOf(660, 840, 990, 1200) else listOf(480, 600, 840, 990, 1200)
        return opts.firstOrNull { it !in used } ?: opts.last()
    }
}

internal fun dayIndex(day: String): Int = CalendarData.days.indexOfFirst { it.short == day }.coerceAtLeast(0)

internal fun String.toMinutes(): Int {
    val m = Regex("(\\d+):(\\d+) (AM|PM)").find(this) ?: return 0
    var h = m.groupValues[1].toInt() % 12
    if (m.groupValues[3] == "PM") h += 12
    return h * 60 + m.groupValues[2].toInt()
}

internal fun Int.asTime(): String {
    val h24 = (this / 60) % 24
    val h = if (h24 % 12 == 0) 12 else h24 % 12
    return "$h:${(this % 60).toString().padStart(2, '0')} ${if (h24 < 12) "AM" else "PM"}"
}

internal fun hourLabel(h: Int): String = when {
    h % 24 == 0 -> "12 AM"
    h == 12 -> "12 PM"
    h < 12 -> "$h AM"
    else -> "${h - 12} PM"
}
