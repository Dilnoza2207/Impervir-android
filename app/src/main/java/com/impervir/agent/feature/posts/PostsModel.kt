package com.impervir.agent.feature.posts

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.impervir.agent.data.Check
import com.impervir.agent.data.Platform
import com.impervir.agent.data.PostStatus
import com.impervir.agent.data.SampleData
import com.impervir.agent.ui.theme.Motion

/** Feature-local data and helpers for the Posts tab. */
internal object PostsData {
    const val weekLabel = "Week 43 · Oct 20 to 26"

    val days = listOf(
        "Mon" to "Monday", "Tue" to "Tuesday", "Wed" to "Wednesday", "Thu" to "Thursday",
        "Fri" to "Friday", "Sat" to "Saturday", "Sun" to "Sunday",
    )

    /** Quick time options in the "When it goes out" sheet. */
    val times = listOf("8:00 AM", "8:45 AM", "9:10 AM", "12:30 PM", "6:00 PM")

    val bestTime = mapOf(
        Platform.LinkedIn to "9:10 AM",
        Platform.Threads to "12:30 PM",
        Platform.X to "8:00 AM",
        Platform.Telegram to "6:00 PM",
    )

    val tones = listOf("More casual", "More direct", "Shorter", "Add a question")

    /** The posts that need a yes from Maya this week (the "asks"). */
    val askIds: List<String> = SampleData.week43Drafts.filter { it.status == PostStatus.NeedsApproval }.map { it.id }

    val checkDetail = mapOf(
        Check.RealStory to "Traced to a line you said on the call",
        Check.YourVoice to "Short first line, numbers over adjectives",
        Check.Cliche to "Rewrote 1 phrase into a plain one",
        Check.YouApprove to "Waiting for your tap",
        Check.ItLearns to "Your edits and sales shape the next draft",
    )

    /** A second wording for each ask, so "Redo this draft" has somewhere to go. Same story, new words. */
    val alternates = mapOf(
        "p43-1" to "A customer emailed me last Tuesday: she used to miss one client deadline a week.\n\nEleven days with the Focus Planner. Zero missed since.\n\nNot more discipline. She just stopped keeping deadlines in her head.\n\nWhat are you still keeping in yours?",
        "p43-2" to "A customer told me her favourite page is the one I almost cut.\n\nShip the boring parts.",
        "p43-3" to "Eleven days with the planner.\n\nA customer stopped missing client deadlines and wrote to tell me. One page a morning.",
    )

    fun redraft(id: String, current: String): String {
        val original = SampleData.week43Drafts.firstOrNull { it.id == id }?.body ?: return current
        val alt = alternates[id] ?: return current
        return if (current == alt) original else alt
    }

    /** What Maya actually said at a timestamp (the agent never invents). */
    fun quoteAt(timestamp: String): String? = SampleData.stories.firstOrNull { it.timestamp == timestamp }?.text

    // Week 42, daily Mon..Sun (sums match the posts list).
    val dailyViews = listOf(1180, 8420, 4290, 5980, 1090, 0, 1020)
    val dailyClicks = listOf(31, 212, 123, 40, 22, 0, 18)
    val dailyRevenue = listOf(58, 340, 139, 0, 29, 0, 0)
}

internal fun String.toMinutes(): Int {
    val m = Regex("(\\d+):(\\d+) (AM|PM)").find(this) ?: return 0
    var h = m.groupValues[1].toInt() % 12
    if (m.groupValues[3] == "PM") h += 12
    return h * 60 + m.groupValues[2].toInt()
}

internal fun dayIndex(day: String): Int = PostsData.days.indexOfFirst { it.first == day }.coerceAtLeast(0)

/** Items rise in once, staggered; scrolling back does not replay it. */
@Composable
internal fun Modifier.staggerIn(index: Int): Modifier {
    var done by rememberSaveable { mutableStateOf(false) }
    val p = remember { Animatable(if (done) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!done) {
            p.animateTo(1f, Motion.reveal(delay = index.coerceAtMost(8) * Motion.Stagger))
            done = true
        }
    }
    return this.graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 18.dp.toPx()
    }
}
