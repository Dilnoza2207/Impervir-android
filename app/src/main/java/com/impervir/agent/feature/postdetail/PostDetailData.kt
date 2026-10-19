package com.impervir.agent.feature.postdetail

import com.impervir.agent.data.Post
import com.impervir.agent.data.SampleData
import com.impervir.agent.data.Store

internal data class Order(val time: String, val product: String, val price: Int)

/** Feature-local detail for Week 42's posts (numbers agree with SampleData.week42Posts). */
internal object DetailData {
    val dates = mapOf("Mon" to "Oct 13", "Tue" to "Oct 14", "Wed" to "Oct 15", "Thu" to "Oct 16", "Fri" to "Oct 17", "Sat" to "Oct 18", "Sun" to "Oct 19")

    /** The published text with the words that came straight from the voice note marked. */
    val segments = mapOf(
        "p42-1" to listOf(
            "Last Tuesday a customer emailed me: " to false,
            "“I finally stopped missing client deadlines.”" to true,
            " She'd used my Notion planner for " to false,
            "11 days" to true,
            ". The part she liked most was " to false,
            "the one I almost cut." to true,
            "\n\nWhat's something you nearly deleted that turned out to matter?" to false,
        ),
        "p42-2" to listOf(
            "Her favourite part was " to false,
            "the page I almost cut." to true,
        ),
    )

    /** Which voice-note moments a post's highlighted words came from. */
    val sourceTimes = mapOf("p42-1" to listOf("0:06", "0:48"))

    // 12 orders, exact total $340: 1 Focus Planner ($29) + 5 Weekly Review Kit ($19) + 6 Bundle ($36)
    val orders = mapOf(
        "p42-1" to listOf(
            Order("Tue 9:24 AM", "Focus Planner", 29), Order("Tue 9:41 AM", "Bundle", 36),
            Order("Tue 11:08 AM", "Weekly Review Kit", 19), Order("Tue 3:52 PM", "Bundle", 36),
            Order("Wed 8:15 AM", "Weekly Review Kit", 19), Order("Wed 12:40 PM", "Bundle", 36),
            Order("Wed 6:03 PM", "Weekly Review Kit", 19), Order("Thu 7:30 AM", "Bundle", 36),
            Order("Thu 10:12 AM", "Weekly Review Kit", 19), Order("Fri 1:45 PM", "Bundle", 36),
            Order("Sat 9:00 AM", "Weekly Review Kit", 19), Order("Sun 4:20 PM", "Bundle", 36),
        ),
        "p42-2" to listOf(
            Order("Wed 12:58 PM", "Bundle", 36), Order("Wed 2:10 PM", "Focus Planner", 29),
            Order("Thu 9:02 AM", "Bundle", 36), Order("Fri 11:30 AM", "Weekly Review Kit", 19),
        ),
        "p42-4" to listOf(Order("Mon 6:12 PM", "Focus Planner", 29), Order("Mon 8:40 PM", "Focus Planner", 29)),
        "p42-5" to listOf(Order("Wed 6:25 PM", "Weekly Review Kit", 19)),
        "p42-6" to listOf(Order("Fri 7:05 PM", "Focus Planner", 29)),
    )

    val store = Store.Stripe

    // First 72 hours, shaped like the LinkedIn post and scaled to each post's totals.
    val hours = listOf(0, 1, 2, 4, 6, 8, 12, 18, 24, 36, 48, 60, 72)
    private val viewShape = listOf(0, 1240, 2100, 3300, 4300, 5100, 6200, 7200, 7750, 8150, 8330, 8400, 8420)
    private val clickShape = listOf(0, 38, 62, 92, 112, 130, 158, 184, 198, 206, 210, 211, 212)
    fun viewCurve(total: Int) = viewShape.map { it * total / 8420 }
    fun clickCurve(total: Int) = clickShape.map { it * total / 212 }

    val audience = listOf("Founders" to 41, "Freelancers" to 33, "Students" to 14, "Other" to 12)

    fun headline(post: Post): String = post.title.substringBefore(":").trim()
    fun quote(post: Post): String? = post.title.substringAfter(":", "").trim().trim('"').takeIf { it.isNotEmpty() }

    fun rank(post: Post): Int = SampleData.week42Posts.sortedByDescending { it.stats?.revenue ?: 0 }.indexOfFirst { it.id == post.id } + 1

    fun pct(part: Int, whole: Int): String = if (whole == 0) "0%" else "%.1f%%".format(java.util.Locale.US, part * 100.0 / whole)
}
