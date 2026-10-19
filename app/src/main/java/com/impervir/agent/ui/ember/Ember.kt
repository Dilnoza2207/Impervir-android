package com.impervir.agent.ui.ember

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Ember's 15 context-driven emotions. */
enum class EmberMood { Idle, Waving, Listening, Talking, Excited, Curious, Concerned, Thinking, Writing, Typing, Happy, Wow, Proud, Oops, Sleeping }

/**
 * Optional hand placement that overrides the mood's own hand pose (used by scenes such as Voice mode,
 * where Ember holds the scene's props). Positions are in Ember's box, as fractions: (0,0) top-left,
 * (1,1) bottom-right; hands may go outside 0..1. Rotation in degrees; curl 0 = open hand, 1 = fist.
 */
@Immutable
data class EmberHands(
    val left: Offset,
    val right: Offset,
    val leftRotation: Float = 0f,
    val rightRotation: Float = 0f,
    val leftCurl: Float = 0.3f,
    val rightCurl: Float = 0.3f,
)

/**
 * The mascot. [tilt] leans the body in degrees, [squash] < 1 squashes / > 1 stretches (for hops and landings),
 * [lookAt] moves the pupils (-1..1 on each axis).
 *
 * Drawn natively (no bitmaps): a glossy wisp body with a swaying swoosh tip, a face whose eyes blink at random
 * intervals, and two floating hands posed per mood. Everything runs on one frame clock and springs between moods;
 * animation only invalidates the draw phase (no recomposition per frame). Below 48dp fine details are skipped.
 */
@Composable
fun Ember(
    mood: EmberMood,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    hands: EmberHands? = null,
    tilt: Float = 0f,
    squash: Float = 1f,
    lookAt: Offset? = null,
) {
    val rig = remember { EmberRig().also { it.mood = mood; it.hands = hands; it.lookAt = lookAt } }
    SideEffect {
        rig.mood = mood
        rig.hands = hands
        rig.tilt = tilt
        rig.squash = squash
        rig.lookAt = lookAt
    }
    LaunchedEffect(rig) {
        while (true) withFrameNanos { rig.tick(it) }
    }
    val detailed = size >= 48.dp
    Spacer(
        modifier
            .size(size)
            .drawBehind {
                rig.frame.intValue // subscribe: redraw on every clock tick
                rig.draw(this, detailed)
            },
    )
}
