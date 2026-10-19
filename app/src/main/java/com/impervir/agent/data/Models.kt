package com.impervir.agent.data

import androidx.compose.runtime.Immutable

enum class Platform(val label: String) { LinkedIn("LinkedIn"), X("X"), Threads("Threads"), Telegram("Telegram") }

enum class Store(val label: String) { Stripe("Stripe"), Gumroad("Gumroad"), LemonSqueezy("Lemon Squeezy") }

enum class PostStatus { NeedsApproval, Scheduled, AutoPost, Posted, Skipped }

/** The five checks every draft goes through. */
enum class Check(val label: String) { RealStory("Real story"), YourVoice("Your voice"), Cliche("Cliché check"), YouApprove("You approve"), ItLearns("It learns") }

@Immutable
data class VoiceSource(val label: String, val timestamp: String)

@Immutable
data class Post(
    val id: String,
    val platform: Platform,
    val title: String,
    val body: String,
    val day: String,        // e.g. "Tue"
    val time: String,       // e.g. "9:10 AM"
    val status: PostStatus,
    val source: VoiceSource? = null,
    val checksPassed: Int = 5,
    val stats: PostStats? = null,
)

@Immutable
data class PostStats(
    val views: Int,
    val likes: Int?,
    val comments: Int?,
    val clicks: Int,
    val orders: Int,
    val revenue: Int,
)

@Immutable
data class Story(val id: String, val text: String, val timestamp: String)

@Immutable
data class Comment(
    val id: String,
    val author: String,
    val initials: String,
    val platform: Platform,
    val onPost: String,
    val text: String,
    val ago: String,
    val firstHour: Boolean,
    val suggestions: List<String>,
)

@Immutable
data class Follower(val platform: Platform, val count: Int, val change: Int)

@Immutable
data class VoiceRule(val id: String, val text: String, val enabled: Boolean = true)

@Immutable
data class ChatMessage(val id: String, val fromAgent: Boolean, val text: String, val time: String)

@Immutable
data class WeekTotals(
    val week: Int,
    val range: String,
    val views: Int,
    val clicks: Int,
    val orders: Int,
    val revenue: Int,
    val clickToOrder: Double,
    val revenueByPlatform: Map<Platform, Int>,
)

@Immutable
data class Slot(val day: String, val time: String, val isOpen: Boolean, val post: Post? = null)
