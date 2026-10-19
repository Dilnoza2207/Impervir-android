package com.impervir.agent.navigation

import kotlinx.serialization.Serializable

/** Type-safe routes (Navigation Compose 2.8). */
sealed interface Destination {
    @Serializable data object Onboarding : Destination
    @Serializable data object Home : Destination
    @Serializable data object Talk : Destination
    @Serializable data object Voice : Destination
    @Serializable data object Writing : Destination
    @Serializable data object Posts : Destination
    @Serializable data class PostEditor(val postId: String) : Destination
    @Serializable data object Calendar : Destination
    @Serializable data object Inbox : Destination
    @Serializable data object Analytics : Destination
    @Serializable data class PostDetail(val postId: String) : Destination
    @Serializable data object Report : Destination
    @Serializable data object YourVoice : Destination
    @Serializable data object Settings : Destination
}

/** What every screen gets to move around. Keeps screens free of NavController. */
interface Navigator {
    fun open(destination: Destination)
    fun back()
    /** Jump to a top-level tab, keeping one copy of each tab on the back stack. */
    fun switchTab(destination: Destination)
}
