package com.impervir.agent.feature.talk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.impervir.agent.data.SampleData
import com.impervir.agent.ui.ember.EmberMood

/** What kind of row a chat item is. */
enum class MsgKind { Agent, Final, User, Voice, Saved }

/** One row of the check-in conversation. [highlight] is the story phrase the agent picked up (shown in accent). */
data class TalkMsg(
    val id: Int,
    val kind: MsgKind,
    val text: String,
    val time: String = "",
    val highlight: String? = null,
    val tag: String = "",
    val storyIndex: Int = -1,
    val duration: String = "",
    val wave: List<Float> = emptyList(),
) {
    val isAgent get() = kind == MsgKind.Agent || kind == MsgKind.Final
    val isMine get() = kind == MsgKind.User || kind == MsgKind.Voice
}

/** A scripted question: the believable voice answer the recorder "hears", and the answer ideas. */
data class ScriptedQuestion(
    val question: String,
    val alt: String,
    val tag: String,
    val storyIndex: Int,
    val spoken: String,
    val highlight: String,
    val ideas: List<String>,
)

/** The bar above the composer when replying to or editing a message. */
data class ComposeBar(val label: String, val excerpt: String, val editId: Int?)

/** Ember's mood from the tone of an agent message. */
fun moodFor(m: TalkMsg): EmberMood = when {
    m.kind == MsgKind.Final -> EmberMood.Proud
    Regex("^(Morning|Hi|Hey)\\b").containsMatchIn(m.text) -> EmberMood.Waving
    Regex("\\$|sold|sale", RegexOption.IGNORE_CASE).containsMatchIn(m.text) -> EmberMood.Excited
    m.text.trimEnd().endsWith("?") -> EmberMood.Curious
    m.text.startsWith("Noted") -> EmberMood.Happy
    else -> EmberMood.Idle
}

/**
 * Plain state holder for the weekly check-in. The scripted agent lives here; the screen only renders.
 * Saved across rotation via [TalkState.Saver] (conversation, question index, edits).
 */
@Stable
class TalkState(
    initial: List<TalkMsg>? = null,
    qi: Int = 1,
    questionEdits: List<String>? = null,
    voiceByDefault: Boolean = false,
) {
    val script: List<ScriptedQuestion> = listOf(
        ScriptedQuestion(
            SampleData.checkInQuestions[0],
            "Who was your best customer this week, and what did they say?",
            "Customer win", 0,
            "So a customer emailed me on Tuesday. She said she finally stopped missing client deadlines. She'd used my Notion planner for eleven days, and the part she liked most was the one I almost cut.",
            "finally stopped missing client deadlines",
            listOf("A customer emailed me", "A post that did well", "Nothing big this week"),
        ),
        ScriptedQuestion(
            SampleData.checkInQuestions[1],
            "Was anything broken this week? Tell me what you learned.",
            "Lesson learned", 3,
            "Honestly, the launch email went out with a broken link. Forty people replied to tell me. I fixed it in ten minutes and sent a sorry note, and that note got more replies than the launch.",
            "that note got more replies than the launch",
            listOf("Nothing went wrong", "A launch problem", "A page I cut"),
        ),
        ScriptedQuestion(
            SampleData.checkInQuestions[2],
            "What is the next thing you want to ship?",
            "What's next", 4,
            "I'm building a weekly review template. It's the page people kept asking about, so I'm turning it into its own product. It should be ready by the end of the month.",
            "building a weekly review template",
            listOf("A weekly review template", "A bundle deal", "Nothing yet"),
        ),
    )

    /** Messages whose entrance already played (so scrolling back doesn't replay it). */
    val revealed = mutableSetOf<Int>()
    val questions = mutableStateListOf<String>().apply { addAll(questionEdits ?: script.map { it.question }) }
    val messages = mutableStateListOf<TalkMsg>().apply { addAll(initial ?: seed()) }
    /** Index of the open question (0..2); 3 = all answered. */
    var qi by mutableIntStateOf(qi)
    var typing by mutableStateOf(false)
    var voiceByDefault by mutableStateOf(voiceByDefault)
    private var seq = (initial?.maxOfOrNull { it.id } ?: 10) + 1
    private var minute = 4 + (initial?.size ?: 0)

    val questionOpen get() = qi < 3 && !typing
    val currentScript get() = script[qi.coerceAtMost(2)]

    private fun seed(): List<TalkMsg> {
        val q = script
        return listOf(
            TalkMsg(1, MsgKind.Agent, "Morning. Two minutes and your week is done.", "9:02 AM"),
            TalkMsg(2, MsgKind.Agent, questions.getOrElse(0) { q[0].question }, "9:02 AM"),
            TalkMsg(3, MsgKind.User, "A customer emailed me. She finally stopped missing client deadlines after 11 days with my Notion planner.", "9:03 AM",
                highlight = "finally stopped missing client deadlines"),
            TalkMsg(4, MsgKind.Saved, SampleData.stories[0].text, tag = q[0].tag, storyIndex = 0),
            TalkMsg(5, MsgKind.Agent, questions.getOrElse(1) { q[1].question }, "9:04 AM"),
        )
    }

    fun nextTime(): String {
        minute = (minute + 1).coerceAtMost(59)
        return "9:" + minute.toString().padStart(2, '0') + " AM"
    }

    fun newMsg(kind: MsgKind, text: String, highlight: String? = null, tag: String = "", storyIndex: Int = -1, duration: String = "", wave: List<Float> = emptyList()) =
        TalkMsg(seq++, kind, text, if (kind == MsgKind.Saved) "" else nextTime(), highlight, tag, storyIndex, duration, wave)

    /** Adds the person's answer (null = skip) and returns the agent's scripted follow-ups, to post after a typing pause. */
    fun answer(msg: TalkMsg?): List<TalkMsg> {
        msg?.let { messages.add(it) }
        typing = true
        val i = qi
        val out = mutableListOf<TalkMsg>()
        if (msg != null && i < 3) {
            val s = script[i]
            out += newMsg(MsgKind.Saved, SampleData.stories[s.storyIndex].text, tag = s.tag, storyIndex = s.storyIndex)
        }
        out += when {
            i < 2 -> newMsg(MsgKind.Agent, questions[i + 1])
            i == 2 -> newMsg(MsgKind.Final, "That's plenty. I'll turn it into 7 posts that sound like you.")
            else -> newMsg(MsgKind.Agent, "Noted. I'll work that into this week's drafts.")
        }
        return out
    }

    fun deliver(replies: List<TalkMsg>) {
        messages.addAll(replies)
        typing = false
        qi = (qi + 1).coerceAtMost(3)
    }

    fun askDifferently(): TalkMsg {
        typing = true
        return newMsg(MsgKind.Agent, if (qi < 3) script[qi].alt else "Anything else you want in this week?")
    }

    fun deliverOne(m: TalkMsg) { messages.add(m); typing = false }

    fun replace(id: Int, text: String) {
        val i = messages.indexOfFirst { it.id == id }
        if (i >= 0) messages[i] = messages[i].copy(text = text, highlight = null)
    }

    fun restart() {
        messages.clear()
        qi = 0
        typing = false
        messages.add(newMsg(MsgKind.Agent, "Morning. Two minutes and your week is done."))
        messages.add(newMsg(MsgKind.Agent, questions[0]))
    }

    companion object {
        val Saver: Saver<TalkState, Any> = Saver(
            save = { s ->
                arrayListOf<Any>(
                    s.qi, s.voiceByDefault, ArrayList(s.questions),
                    ArrayList(s.messages.map { m ->
                        arrayListOf<Any>(m.id, m.kind.ordinal, m.text, m.time, m.highlight ?: "", m.tag, m.storyIndex, m.duration, ArrayList(m.wave))
                    }),
                )
            },
            restore = { v ->
                @Suppress("UNCHECKED_CAST")
                val l = v as List<Any>
                @Suppress("UNCHECKED_CAST")
                val msgs = (l[3] as List<List<Any>>).map { r ->
                    @Suppress("UNCHECKED_CAST")
                    TalkMsg(r[0] as Int, MsgKind.entries[r[1] as Int], r[2] as String, r[3] as String, (r[4] as String).ifEmpty { null },
                        r[5] as String, r[6] as Int, r[7] as String, r[8] as List<Float>)
                }
                @Suppress("UNCHECKED_CAST")
                TalkState(msgs, l[0] as Int, l[2] as List<String>, l[1] as Boolean).also { st -> msgs.forEach { st.revealed.add(it.id) } }
            },
        )
    }
}

@Composable
fun rememberTalkState(): TalkState = rememberSaveable(saver = TalkState.Saver) { TalkState() }

/** The seeded morning conversation (ids 1..5) staggers in on first show; new messages appear at once. */
fun seedDelay(id: Int): Long = if (id in 1..5) id * 140L else 0L
