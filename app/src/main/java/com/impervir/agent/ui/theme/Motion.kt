package com.impervir.agent.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/** Motion vocabulary shared by every screen (mirrors the design's CSS curves). */
object Motion {
    val Emphasized = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)      // blur-rise reveals
    val Springy = CubicBezierEasing(0.3f, 1.6f, 0.5f, 1f)         // pops
    const val Reveal = 800
    const val Stagger = 60
    fun <T> pop() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
    fun <T> soft() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    fun <T> reveal(delay: Int = 0) = tween<T>(Reveal, delay, Emphasized)
}
