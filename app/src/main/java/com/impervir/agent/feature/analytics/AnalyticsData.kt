package com.impervir.agent.feature.analytics

import androidx.compose.runtime.Immutable
import com.impervir.agent.data.Platform
import com.impervir.agent.data.SampleData

/** Feature-local view data for Analytics. All numbers agree with SampleData and the iOS design. */
enum class Range(val label: String) { Week("Week"), Month("Month"), Eight("8 weeks") }

@Immutable
data class RangeData(
    val name: String,
    val note: String,
    val revenue: Int,
    val pill: String,
    val views: String,
    val clicks: String,
    val orders: String,
    val clickToOrder: String,
)

val rangeData = mapOf(
    Range.Week to RangeData("Week 42", "Made in Week 42, from 446 clicks", SampleData.week42.revenue, "+36% vs last week", "21,980", "446", "20", "4.5%"),
    Range.Month to RangeData("Last 4 weeks", "Made in the last 4 weeks, from 1,290 clicks", 1571, "+147% vs the 4 weeks before", "65.5k", "1,290", "57", "4.4%"),
    Range.Eight to RangeData("Last 8 weeks", "Made in the last 8 weeks, from 1,840 clicks", 2206, "Up from $120 in W35", "97.1k", "1,840", "79", "4.3%"),
)

@Immutable
data class Kpi(
    val title: String,
    val caption: String,
    val meaning: String,
    val counted: String,
    val now: String,
    val prev: String,
    val delta: String,
    val weekChip: String,
    val good: Boolean = true,
)

/** Index 0..5: Views, Clicks, Orders, Click to order, New followers, Edit rate. Clicks and Orders show by default. */
val kpis = listOf(
    Kpi("Views", "People who saw your posts", "How many times your posts were shown to people this week. One person seeing a post twice counts as two views.",
        "Views from every post published in the week, added up across LinkedIn, Threads, X and Telegram, as each platform reports them.", "21,980", "17,200", "Up 28%, about 4,800 more views.", "+28%"),
    Kpi("Clicks", "People who tapped your links", "People who tapped a tracking link in one of your posts. A tap counts even if they leave without buying.",
        "Each tap on a tracking link in a post, from all four platforms.", "446", "375", "Up 19%, 71 more taps.", "+19%"),
    Kpi("Orders", "People who bought", "People who bought after tapping a link. Each sale is credited to the post that earned it.",
        "Sales from Stripe, Gumroad and Lemon Squeezy that came through your tracking links, matched to the post that sent them.", "20", "15", "Up 33%, 5 more orders.", "+33%"),
    Kpi("Click to order", "Out of every 100 taps, buyers", "Out of every 100 people who tapped a link, how many bought. Higher means your links and posts are working.",
        "Orders divided by clicks in the same week. 20 orders from 446 clicks is 4.5%.", "4.5%", "4.0%", "Up 0.5 points.", "+0.5 pts"),
    Kpi("New followers", "Across all 4 platforms", "People who started following your accounts this week, across all four platforms.",
        "New follows added up: LinkedIn +126, X +41, Threads +63, Telegram +38.", "+268", "—", "Last week's follower gain is not in your report yet, so there is no change to show.", "Up this week"),
    Kpi("Edit rate", "Share of my drafts you rewrote. Lower is better.", "Share of my drafts you rewrote before approving. Lower means I sound more like you.",
        "Drafts you changed before approving, divided by all the drafts I wrote that week.", "11%", "16%", "Down 5 points. Lower is better.", "Down from 16%"),
)

fun Kpi.value(r: RangeData): String = when (title) {
    "Views" -> r.views
    "Clicks" -> r.clicks
    "Orders" -> r.orders
    "Click to order" -> r.clickToOrder
    else -> now
}

enum class PostSort(val label: String, val caption: String) {
    Revenue("Revenue", "Your top sellers this week."),
    Views("Views", "Your most-seen posts this week."),
    Clicks("Clicks", "The posts that sent the most people to your links."),
}

/** One insight, with a plain action. */
@Immutable
data class Insight(val text: String, val action: String, val confirm: String)

val insights = listOf(
    Insight("Customer stories made $340. Tips made $0.", "Plan 2 more", "I'll ask about 2 more customer stories in Monday's check-in."),
    Insight("X drove 5,980 views and no sales.", "Add product link on X", "Your next X draft will carry the product link. You approve it first."),
    Insight("Telegram buyers: 4 of 98 clicks bought (4.1%).", "Post more offers there", "I'll draft 2 Telegram offers. You approve them before they go out."),
)

/** Optional charts behind Customize (all off by default). */
enum class ChartKind(val label: String, val sub: String) {
    RevenueByWeek("Revenue by week", "Drag across eight weeks of sales"),
    Funnel("Where sales drop off", "Views to clicks to orders"),
    ByPlatform("By platform", "Revenue, views, clicks or followers"),
    WhatSells("What sells", "Money by kind of post"),
    Timing("When your posts work best", "Sales by day and time"),
    Voice("Sounds like you", "How much you edit my drafts"),
}

enum class PlatformMetric(val label: String, val centerLabel: String, val takeaway: String) {
    Revenue("Revenue", "revenue", "LinkedIn brings 60% of your money."),
    Views("Views", "views", "LinkedIn gets 38% of your views."),
    Clicks("Clicks", "clicks", "LinkedIn gets 48% of your clicks."),
    Followers("Followers", "new followers", "LinkedIn adds 47% of your new followers."),
}

@Immutable
data class PlatformRow(val platform: Platform, val revenue: Int, val views: Int, val clicks: Int, val followers: Int) {
    fun of(m: PlatformMetric) = when (m) {
        PlatformMetric.Revenue -> revenue
        PlatformMetric.Views -> views
        PlatformMetric.Clicks -> clicks
        PlatformMetric.Followers -> followers
    }
}

val platformRows = listOf(
    PlatformRow(Platform.LinkedIn, 340, 8420, 212, 126),
    PlatformRow(Platform.Threads, 120, 3150, 96, 63),
    PlatformRow(Platform.Telegram, 106, 4430, 98, 38),
    PlatformRow(Platform.X, 0, 5980, 40, 41),
)

val contentTypes = listOf("Customer stories" to 60, "Behind the scenes" to 21, "Product how-to" to 19, "Tips" to 0)

/** Week 42 revenue per day (Mon..Sun) x slot (Morning, Midday, Evening, Night). */
val heatRevenue = listOf(
    listOf(0, 0, 58, 0), listOf(340, 0, 0, 0), listOf(0, 120, 19, 0), listOf(0, 0, 0, 0),
    listOf(0, 0, 29, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0),
)
val heatLabels = mapOf((1 to 0) to "LinkedIn", (2 to 1) to "Threads", (0 to 2) to "Telegram")
val heatDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
val heatSlots = listOf("Morning", "Midday", "Evening", "Night")

const val summaryText = "Week 42: $566 from 20 orders, up 36%. Customer stories made 60% of it. Next week: 2 customer stories, 1 behind-the-scenes post, and a Telegram offer on Friday."
