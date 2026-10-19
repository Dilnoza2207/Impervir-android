package com.impervir.agent.feature.voice

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.min

/** One prompt Ember asks, what Maya says, and the story phrases inside it (word index ranges) with their tags. */
internal class VoicePrompt(val question: String, text: String, private val phrases: IntArray, val tags: List<String>) {
    val words: List<String> = text.split(" ")

    /** Index of the story phrase word [i] belongs to, or -1. */
    fun phraseOf(i: Int): Int {
        var k = 0
        while (k < phrases.size) { if (i >= phrases[k] && i <= phrases[k + 1]) return k / 2; k += 2 }
        return -1
    }

    fun phraseEnd(p: Int) = phrases[p * 2 + 1]
    val phraseCount get() = phrases.size / 2
}

internal val VoicePrompts = listOf(
    VoicePrompt(
        "What did a customer tell you this week?",
        "So a customer emailed me on Tuesday. She said she finally stopped missing client deadlines. She'd used my Notion planner for eleven days, and the part she liked most was the one I almost cut.",
        intArrayOf(10, 14, 21, 22, 30, 34), listOf("Customer win", "Eleven days", "Favourite part"),
    ),
    VoicePrompt(
        "What went wrong, and what did you learn?",
        "Honestly, the launch email went out with a broken link. Forty people replied to tell me. I fixed it in ten minutes and sent a sorry note, and that note got more replies than the launch.",
        intArrayOf(8, 9), listOf("Lesson learned"),
    ),
    VoicePrompt(
        "What are you making next?",
        "I'm building a weekly review template. It's the page people kept asking about, so I'm turning it into its own product. It should be ready by the end of the month.",
        intArrayOf(3, 5), listOf("What's next"),
    ),
)

internal val TotalStories = VoicePrompts.sumOf { it.tags.size }

/**
 * Simulated live session: words arrive every 300 ms, story phrases are "caught" two words after they end,
 * and the next prompt starts 3 s after the answer finishes. Driven by the screen's frame loop; frozen while paused.
 */
@Stable
internal class VoiceSession {
    var paused by mutableStateOf(false)
    var promptIndex by mutableIntStateOf(0); private set
    /** 100 ms ticks into the current prompt. */
    var ticks by mutableIntStateOf(0); private set
    /** 100 ms ticks since the session started (the recording timer). */
    var elapsed by mutableIntStateOf(0); private set

    val caught = mutableStateListOf<String>()
    val removed = mutableStateListOf<String>()
    val names = mutableStateMapOf<String, String>()

    private var acc = 0f

    val prompt: VoicePrompt get() = VoicePrompts[promptIndex]
    val wordCount: Int get() = min(ticks / 3, prompt.words.size)

    /** The story just caught (shown as "+1 story" for about a second), or null. */
    val justCaught: String?
        get() {
            val p = prompt
            for (k in 0 until p.phraseCount) {
                val at = (p.phraseEnd(k) + 2) * 3
                if (ticks >= at && ticks < at + 10) return p.tags[k]
            }
            return null
        }

    val shown: List<String> get() = caught.filter { it !in removed }

    fun nameOf(tag: String) = names[tag] ?: tag

    fun advance(dt: Float) {
        if (paused) return
        acc += min(dt, 0.25f)
        while (acc >= 0.1f) { acc -= 0.1f; tick() }
    }

    private fun tick() {
        val p = prompt
        val len = p.words.size
        val t = ticks + 1
        val n = min(t / 3, len)
        for (k in 0 until p.phraseCount) {
            val tag = p.tags[k]
            if (n >= min(p.phraseEnd(k) + 2, len) && tag !in caught) caught.add(tag)
        }
        elapsed++
        if (t > len * 3 + 30) { ticks = 0; promptIndex = (promptIndex + 1) % VoicePrompts.size } else ticks = t
    }

    fun remove(tag: String) { if (tag !in removed) removed.add(tag) }
    fun restore(tag: String) { removed.remove(tag) }
    fun rename(tag: String, name: String) { names[tag] = name }
}
