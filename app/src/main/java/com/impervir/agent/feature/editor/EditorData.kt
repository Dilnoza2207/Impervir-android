package com.impervir.agent.feature.editor

import com.impervir.agent.data.Platform
import com.impervir.agent.data.SampleData

/** One "Ask the agent" tool: the first (from, to) pair whose `from` is in the text is the suggestion. */
internal data class AgentTool(val id: String, val label: String, val pairs: List<Pair<String, String>>)

internal data class PlatformRules(val limit: Int, val best: String, val bestNote: String, val options: List<String>)

internal object EditorData {
    val rules = mapOf(
        Platform.LinkedIn to PlatformRules(3000, "Tue 9:10 AM", "LinkedIn is read most on Tuesday and Wednesday mornings, around 9 AM.", listOf("Tue 9:10 AM", "Wed 9:05 AM", "Thu 9:10 AM")),
        Platform.X to PlatformRules(280, "Thu 8:00 AM", "X is busiest around 8 AM, so Thursday 8:00 is the pick.", listOf("Thu 8:00 AM", "Thu 8:45 AM", "Fri 8:00 AM")),
        Platform.Threads to PlatformRules(500, "Wed 12:30 PM", "Threads is busiest between 12 and 1 PM.", listOf("Wed 12:30 PM", "Thu 12:15 PM", "Fri 12:45 PM")),
        Platform.Telegram to PlatformRules(4096, "Mon 6:00 PM", "Your Telegram channel opens most around 6 PM.", listOf("Mon 6:00 PM", "Wed 6:00 PM", "Fri 6:00 PM")),
    )

    /** The same customer story, written for the other platforms (used when Maya switches platform). */
    val crossPost = mapOf(
        Platform.LinkedIn to "A customer emailed me on Tuesday. She finally stopped missing client deadlines.\n\nShe had used my Notion planner for 11 days, and the part she liked most was the one I almost cut. Her planner is a game-changer.\n\nI nearly deleted that page before launch. It felt extra, so I almost pulled it. Now it is the page people keep asking about, and I'm building a Weekly Review template next.\n\nWhat's something you nearly deleted that turned out to matter?",
        Platform.X to "11 days.\n\nThat's how long it took a customer to stop missing client deadlines with my planner.\n\nThe fix wasn't a new feature. It was a page I almost deleted.\n\n[tracking link]",
        Platform.Threads to "I almost cut one page from my planner before launch.\n\nA customer just told me it's the reason she stopped missing client deadlines.\n\nShip the boring parts. Honestly, they're the ones that stick.",
        Platform.Telegram to "Eleven days, zero missed deadlines.",
    )

    val tools = listOf(
        AgentTool("short", "Shorter", listOf(
            "Eleven days after switching to the Focus Planner, she hasn't missed one." to "Eleven days on the Focus Planner. Zero missed.",
            "She had used my Notion planner for 11 days, and the part she liked most was the one I almost cut." to "Eleven days with my Notion planner, and her favourite part was the one I almost cut.",
            "That's how long it took a customer to stop missing client deadlines with my planner." to "A customer stopped missing client deadlines with it.",
            "A customer just told me it's the reason she stopped missing client deadlines." to "A customer says it's why she stopped missing deadlines.",
            "It's the one customers mention most." to "Customers mention it most.",
            "That's how long it took a customer to stop missing deadlines." to "That's all it took.",
        )),
        AgentTool("punch", "Punchier opening", listOf(
            "A customer told me she used to miss one client deadline a week." to "One missed client deadline a week. That was her normal.",
            "A customer emailed me on Tuesday. She finally stopped missing client deadlines." to "She finally stopped missing client deadlines. A customer told me on Tuesday.",
            "11 days." to "Eleven days. Then it stuck.",
            "I almost cut one page from my planner before launch." to "I almost cut a page before launch.",
            "I almost cut one page from the Focus Planner." to "I almost cut a page.",
        )),
        AgentTool("ask", "Add a question", listOf(
            "I nearly deleted that page before launch." to "I nearly deleted that page before launch. Did you almost cut one of yours?",
            "It was a page I almost deleted." to "It was a page I almost deleted. Which one would you cut?",
            "Ship the boring parts." to "Ship the boring parts. What would you cut?",
            "One page a morning." to "One page a morning. What's on yours?",
        )),
        AgentTool("mine", "More like me", listOf(
            "She didn't get more disciplined." to "No new discipline.",
            "It felt extra, so I almost pulled it." to "It felt like filler, so I almost cut it.",
            "The fix wasn't a new feature." to "No new feature needed.",
            "Honestly, they're the ones that stick." to "They're the ones that stick.",
            "No new habits." to "No new habit to build.",
        )),
        AgentTool("links", "Remove links", listOf("\n\n[tracking link]" to "")),
    )

    /** Phrases that read like AI. The first one is Maya's own rule. */
    val cliches = listOf(
        Triple("game-changer", "saved her 3 hours a week", "Your rule: never says 'game-changer'."),
        Triple("unlock", "get", "Plain words read like you."),
        Triple("delve", "dig", "Plain words read like you."),
    )

    /** Voice-note moments the drafts came from. */
    val sources = listOf(
        "0:06" to "Stopped missing deadlines",
        "0:21" to "Eleven days in",
        "0:48" to "The page I almost cut",
    )

    val previewName = SampleData.userName
}

internal fun AgentTool.suggestionFor(text: String): Pair<String, String>? = pairs.firstOrNull { text.contains(it.first) }

/** Word diff between `a` and `b`: shared prefix, removed words, added words, shared suffix. */
internal data class Diff(val prefix: String, val removed: String, val added: String, val suffix: String)

internal fun wordDiff(a: String, b: String): Diff {
    val aw = a.split(" ")
    val bw = if (b.isEmpty()) emptyList() else b.split(" ")
    var p = 0
    while (p < aw.size && p < bw.size && aw[p] == bw[p]) p++
    var s = 0
    while (s < aw.size - p && s < bw.size - p && aw[aw.size - 1 - s] == bw[bw.size - 1 - s]) s++
    return Diff(
        prefix = aw.subList(0, p).joinToString(" "),
        removed = aw.subList(p, aw.size - s).joinToString(" "),
        added = bw.subList(p, bw.size - s).joinToString(" "),
        suffix = aw.subList(aw.size - s, aw.size).joinToString(" "),
    )
}

internal fun String.toMinutes(): Int {
    val m = Regex("(\\d+):(\\d+) (AM|PM)").find(this) ?: return 0
    var h = m.groupValues[1].toInt() % 12
    if (m.groupValues[3] == "PM") h += 12
    return h * 60 + m.groupValues[2].toInt()
}

internal fun formatTime(hour24: Int, minute: Int): String {
    val h = if (hour24 % 12 == 0) 12 else hour24 % 12
    return "$h:${minute.toString().padStart(2, '0')} ${if (hour24 < 12) "AM" else "PM"}"
}
