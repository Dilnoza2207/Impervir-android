package com.impervir.agent.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory source of truth for the prototype. Screens observe [drafts] and call the
 * mutators; a real implementation would back these with the API and Room.
 */
class ImpervirRepository {
    private val _drafts = MutableStateFlow(SampleData.week43Drafts)
    val drafts: StateFlow<List<Post>> = _drafts.asStateFlow()

    private val _comments = MutableStateFlow(SampleData.comments)
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _rules = MutableStateFlow(SampleData.voiceRules)
    val rules: StateFlow<List<VoiceRule>> = _rules.asStateFlow()

    fun approve(id: String) = setStatus(id, PostStatus.Scheduled)
    fun skip(id: String) = setStatus(id, PostStatus.Skipped)
    fun undo(id: String, previous: PostStatus) = setStatus(id, previous)

    fun updateBody(id: String, body: String) = _drafts.update { list -> list.map { if (it.id == id) it.copy(body = body) else it } }
    fun reschedule(id: String, day: String, time: String) = _drafts.update { list -> list.map { if (it.id == id) it.copy(day = day, time = time) else it } }

    fun replied(commentId: String) = _comments.update { list -> list.filterNot { it.id == commentId } }

    fun toggleRule(id: String) = _rules.update { list -> list.map { if (it.id == id) it.copy(enabled = !it.enabled) else it } }
    fun addRule(text: String) = _rules.update { it + VoiceRule("r${it.size + 1}", text) }

    fun post(id: String): Post? = (_drafts.value + SampleData.week42Posts).firstOrNull { it.id == id }

    private fun setStatus(id: String, status: PostStatus) = _drafts.update { list -> list.map { if (it.id == id) it.copy(status = status) else it } }
}
