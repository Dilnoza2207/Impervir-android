package com.impervir.agent.ui.ember

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/*
 * Ember's mood vocabulary, ported from the 2D design (ember2/ember.py): every pose value below is in the design's
 * 200 x 220 viewBox units, so numbers can be compared 1:1 with the SVG / CSS reference.
 */

internal enum class EyeKind { Open, Happy, Closed }

internal enum class MouthKind { Smile, Open, O, Grin, Tongue, Smirk, Wobble, Flat, Sleep }

internal enum class HandMotion { Still, Bob, SlowBob, Gesture, Cup, Tap, Wave, Scribble, Type, Hop, Shake, Scratch, Wring }

internal enum class BodyMotion { Breathe, Lean, Bounce, Tilt, Hop, Proud, Sleep }

internal enum class GazeMode { Wander, Around, Fixed }

internal const val LAYER_BEHIND = 0 // behind the body
internal const val LAYER_MID = 1    // in front of the body, behind props (laptop lid, notepad)
internal const val LAYER_FRONT = 2  // in front of everything

/** Prop / effect slots (each fades and pops in with its own spring weight). */
internal object Prop {
    const val NOTE = 0
    const val LAPTOP = 1
    const val SPARKS = 2
    const val SWEAT = 3
    const val ZZ = 4
    const val DOTS = 5
    const val PEN = 6
    const val COUNT = 7
}

private fun props(vararg p: Int): Int = p.fold(0) { acc, i -> acc or (1 shl i) }

/**
 * One floating hand: position of the hand's origin (palm centre), rotation in degrees (0 = fingers up),
 * curl (0 open, 1 fist), its looping motion and draw layer. Left hands are drawn mirrored, like the reference.
 */
internal class HandPose(
    val x: Float,
    val y: Float,
    val rot: Float,
    val curl: Float = 0.25f,
    val motion: HandMotion = HandMotion.Bob,
    val period: Float = 3.2f,
    val phase: Float = 0f,
    val layer: Int = LAYER_FRONT,
)

internal class MoodSpec(
    val eyes: EyeKind = EyeKind.Open,
    val mouth: MouthKind = MouthKind.Smile,
    val eyeX: Float = 0f,
    val eyeY: Float = 0f,
    val eyeScale: Float = 1f,
    val browLY: Float = 0f,
    val browLR: Float = 0f,
    val browRY: Float = 0f,
    val browRR: Float = 0f,
    val cheek: Float = 0.55f,
    val left: HandPose = IdleLeft,
    val right: HandPose = IdleRight,
    val body: BodyMotion = BodyMotion.Breathe,
    val bodyPeriod: Float = 3.2f,
    val browBob: Float = 0f,
    val gaze: GazeMode = GazeMode.Wander,
    val props: Int = 0,
    val talk: Float = 1f,
) {
    fun has(prop: Int) = props and (1 shl prop) != 0
}

private val IdleLeft = HandPose(17f, 140f, -16f, 0.25f, HandMotion.Bob, 3.2f, 0f)
private val IdleRight = HandPose(183f, 140f, 16f, 0.25f, HandMotion.Bob, 3.2f, 1.6f)

/** Indexed by [EmberMood.ordinal]. */
internal val MoodSpecs: Array<MoodSpec> = Array(EmberMood.entries.size) { i ->
    when (EmberMood.entries[i]) {
        EmberMood.Idle -> MoodSpec()
        EmberMood.Waving -> MoodSpec(
            mouth = MouthKind.Grin, browLY = -3f, browRY = -3f, cheek = 0.7f,
            right = HandPose(190f, 76f, 14f, 0.02f, HandMotion.Wave, 1.0f),
        )
        EmberMood.Listening -> MoodSpec(
            browLY = -2f, browRY = -2f, gaze = GazeMode.Around,
            body = BodyMotion.Lean, bodyPeriod = 3f,
            right = HandPose(182f, 94f, -14f, 0.55f, HandMotion.Cup, 2.4f),
        )
        EmberMood.Talking -> MoodSpec(
            mouth = MouthKind.Open, body = BodyMotion.Bounce, bodyPeriod = 0.6f, browBob = 3f,
            left = HandPose(14f, 128f, -28f, 0.2f, HandMotion.Gesture, 1.1f, 0f),
            right = HandPose(186f, 128f, 28f, 0.2f, HandMotion.Gesture, 1.1f, 0.55f),
        )
        EmberMood.Excited -> MoodSpec(
            mouth = MouthKind.Open, browLY = -6f, browRY = -6f, cheek = 0.8f, eyeScale = 1.04f,
            body = BodyMotion.Bounce, bodyPeriod = 0.5f, props = props(Prop.SPARKS), talk = 1.35f,
            left = HandPose(12f, 92f, -30f, 0.08f, HandMotion.Gesture, 0.6f, 0f),
            right = HandPose(188f, 92f, 30f, 0.08f, HandMotion.Gesture, 0.6f, 0.3f),
        )
        EmberMood.Curious -> MoodSpec(
            mouth = MouthKind.Open, eyeX = 2f, eyeY = -1f, browLY = 1f, browLR = 6f, browRY = -7f, browRR = -6f,
            body = BodyMotion.Tilt, bodyPeriod = 2.4f, talk = 0.85f,
            left = HandPose(30f, 150f, 40f, 0.3f, HandMotion.Bob, 2f, 0f),
            right = HandPose(186f, 128f, 28f, 0.2f, HandMotion.Gesture, 1.1f, 0.55f),
        )
        EmberMood.Concerned -> MoodSpec(
            mouth = MouthKind.Open, eyeY = 1.5f, browLY = -2f, browLR = -14f, browRY = -2f, browRR = 14f, cheek = 0.4f,
            body = BodyMotion.Bounce, bodyPeriod = 1.2f, talk = 0.7f,
            left = HandPose(84f, 186f, 75f, 0.45f, HandMotion.Wring, 1.4f, 0f),
            right = HandPose(116f, 186f, -75f, 0.45f, HandMotion.Wring, 1.4f, 0.7f),
        )
        EmberMood.Thinking -> MoodSpec(
            mouth = MouthKind.Flat, eyeX = -5f, eyeY = -6f, browLY = -6f, browLR = -6f, browRY = 1f, browRR = 4f,
            gaze = GazeMode.Fixed, props = props(Prop.DOTS),
            left = HandPose(44f, 76f, 40f, 0.55f, HandMotion.Tap, 0.9f),
        )
        EmberMood.Writing -> MoodSpec(
            mouth = MouthKind.Tongue, eyeX = -6f, eyeY = 5f, browLY = 3f, browLR = 10f, browRY = 3f, browRR = -10f,
            gaze = GazeMode.Fixed, props = props(Prop.NOTE, Prop.PEN),
            left = HandPose(36f, 193f, 30f, 0.55f, HandMotion.Still),
            right = HandPose(80f, 156f, 20f, 0.5f, HandMotion.Scribble, 0.5f, layer = LAYER_MID),
        )
        EmberMood.Typing -> MoodSpec(
            mouth = MouthKind.Flat, eyeY = 5f, browLY = 2f, browLR = 8f, browRY = 2f, browRR = -8f,
            gaze = GazeMode.Fixed, props = props(Prop.LAPTOP),
            left = HandPose(47f, 180f, 62f, 0.35f, HandMotion.Type, 0.52f, 0f, LAYER_MID),
            right = HandPose(153f, 180f, -62f, 0.35f, HandMotion.Type, 0.52f, 0.26f, LAYER_MID),
        )
        EmberMood.Happy -> MoodSpec(
            eyes = EyeKind.Happy, mouth = MouthKind.Grin, browLY = -5f, browRY = -5f, cheek = 0.85f,
            body = BodyMotion.Hop, bodyPeriod = 1.2f,
            left = HandPose(16f, 70f, -26f, 0.08f, HandMotion.Hop, 1.2f),
            right = HandPose(190f, 84f, 26f, 0.08f, HandMotion.Hop, 1.2f),
        )
        EmberMood.Wow -> MoodSpec(
            mouth = MouthKind.O, eyeY = -2f, eyeScale = 1.08f, browLY = -9f, browRY = -9f, cheek = 0.75f,
            gaze = GazeMode.Fixed, props = props(Prop.SPARKS),
            left = HandPose(36f, 124f, 14f, 0.12f, HandMotion.Shake, 0.36f),
            right = HandPose(164f, 124f, -14f, 0.12f, HandMotion.Shake, 0.36f, 0.18f),
        )
        EmberMood.Proud -> MoodSpec(
            mouth = MouthKind.Smirk, eyeY = -3f, browLY = -4f, browLR = -4f, browRY = -6f, browRR = 6f,
            body = BodyMotion.Proud, bodyPeriod = 2.6f, gaze = GazeMode.Fixed,
            left = HandPose(25f, 158f, 72f, 1f, HandMotion.Still, layer = LAYER_BEHIND),
            right = HandPose(175f, 158f, -72f, 1f, HandMotion.Still, layer = LAYER_BEHIND),
        )
        EmberMood.Oops -> MoodSpec(
            mouth = MouthKind.Wobble, eyeX = 3f, browLY = -3f, browLR = -14f, browRY = -3f, browRR = 14f, cheek = 0.65f,
            props = props(Prop.SWEAT), gaze = GazeMode.Fixed,
            right = HandPose(176f, 76f, -30f, 0.4f, HandMotion.Scratch, 0.6f),
        )
        EmberMood.Sleeping -> MoodSpec(
            eyes = EyeKind.Closed, mouth = MouthKind.Sleep, browLY = 4f, browRY = 4f, cheek = 0.45f,
            body = BodyMotion.Sleep, bodyPeriod = 4.8f, gaze = GazeMode.Fixed, props = props(Prop.ZZ),
            left = HandPose(82f, 186f, 78f, 0.5f, HandMotion.SlowBob, 4.8f),
            right = HandPose(118f, 186f, -78f, 0.5f, HandMotion.SlowBob, 4.8f, 0.3f),
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Small math helpers (no allocation)

internal const val TAU = (2 * PI).toFloat()

internal fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

internal fun frac(x: Float) = x - floor(x)

internal fun smooth(t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

/** 0 -> 1 -> 0 over one period, eased like CSS ease-in-out between 0% and 50%. */
internal fun wave01(t: Float, period: Float) = 0.5f - 0.5f * cos(TAU * t / period)

/** Piecewise keyframes with smoothstep easing between keys; [ts] ascending in 0..1, first 0, last 1. */
internal fun keyframes(p: Float, ts: FloatArray, vs: FloatArray): Float {
    for (i in 1 until ts.size) {
        if (p <= ts[i]) {
            val span = ts[i] - ts[i - 1]
            val k = if (span <= 0f) 1f else (p - ts[i - 1]) / span
            return lerp(vs[i - 1], vs[i], smooth(k))
        }
    }
    return vs[vs.size - 1]
}

private val HopT = floatArrayOf(0f, 0.3f, 0.55f, 0.72f, 1f)
private val HopY = floatArrayOf(0f, -18f, 0f, 0f, 0f)
private val HopSX = floatArrayOf(1.07f, 0.95f, 1.09f, 1f, 1.07f)
private val HopSY = floatArrayOf(0.93f, 1.06f, 0.91f, 1f, 0.93f)
private val HandHopT = floatArrayOf(0f, 0.3f, 0.55f, 1f)
private val HandHopY = floatArrayOf(0f, -24f, 0f, 0f)
private val HandHopR = floatArrayOf(0f, -10f, 4f, 0f)
private val LookT = floatArrayOf(0f, 0.4f, 0.5f, 0.7f, 0.8f, 0.9f, 1f)
private val LookX = floatArrayOf(0f, 0f, -4f, -4f, 4f, 4f, 0f)
private val LookY = floatArrayOf(0f, 0f, 1f, 1f, 1f, 1f, 0f)

/** Writes the hand's loop offsets (dx, dy, rotation) in its own (mirrored) frame into [out]. */
internal fun handMotion(pose: HandPose, t: Float, amp: Float, out: FloatArray) {
    val tt = t + pose.phase
    var dx = 0f
    var dy = 0f
    var r = 0f
    when (pose.motion) {
        HandMotion.Still -> {}
        HandMotion.Bob -> dy = -4f * wave01(tt, pose.period)
        HandMotion.SlowBob -> dy = -2.5f * wave01(tt, pose.period)
        HandMotion.Gesture -> {
            val w = wave01(tt, pose.period); dy = -10f * w; r = -14f * w
        }
        HandMotion.Cup -> {
            val w = wave01(tt, pose.period); dy = -2f * w; r = -4f * w
        }
        HandMotion.Tap -> {
            val p = frac(tt / pose.period)
            val w = if (p < 0.4f) smooth(p / 0.4f) else 1f - smooth((p - 0.4f) / 0.6f)
            r = -12f * w; dy = -2f * w
        }
        HandMotion.Wave -> r = 3f + 19f * sin(TAU * tt / pose.period)
        HandMotion.Scribble -> {
            val p = TAU * frac(tt / pose.period)
            dx = -4f * (1f - cos(p)); dy = 1f + 1.6f * sin(2f * p)
        }
        HandMotion.Type -> dy = -6f * wave01(tt, pose.period)
        HandMotion.Hop -> {
            val p = frac(tt / pose.period)
            dy = keyframes(p, HandHopT, HandHopY); r = keyframes(p, HandHopT, HandHopR)
        }
        HandMotion.Shake -> dx = 1.5f * sin(TAU * tt / pose.period)
        HandMotion.Scratch -> {
            val s = sin(TAU * tt / pose.period); r = 8f * s; dy = -1.5f * s
        }
        HandMotion.Wring -> r = 10f * wave01(tt, pose.period)
    }
    out[0] = dx * amp; out[1] = dy * amp; out[2] = r * amp
}

/** Writes body offsets: translateY, scaleX, scaleY, rotation, shadow scale, shadow alpha. */
internal fun bodyMotion(kind: BodyMotion, period: Float, t: Float, amp: Float, out: FloatArray) {
    var ty = 0f
    var sx = 1f
    var sy = 1f
    var rot = 0f
    var sh = 1f
    var sa = 1f
    val b = wave01(t, period)
    when (kind) {
        BodyMotion.Breathe -> { sx = 1f + 0.03f * b; sy = 1f - 0.03f * b; sh = 1f + 0.05f * b }
        BodyMotion.Sleep -> { sx = 1f + 0.035f * b; sy = 1f - 0.035f * b; sh = 1f + 0.05f * b }
        BodyMotion.Lean -> { rot = -3f * b; sx = 1f + 0.02f * b; sy = 1f - 0.02f * b; sh = 1f + 0.03f * b }
        BodyMotion.Bounce -> { ty = b; sx = 1f + 0.025f * b; sy = 1f - 0.025f * b; sh = 1f + 0.03f * b }
        BodyMotion.Tilt -> { rot = -5f - 2f * b; sx = 1f + 0.01f * b; sy = 1f - 0.01f * b }
        BodyMotion.Proud -> { sx = 1f - 0.02f * b; sy = 1f + 0.04f * b; sh = 1f - 0.02f * b }
        BodyMotion.Hop -> {
            val p = frac(t / period)
            ty = keyframes(p, HopT, HopY); sx = keyframes(p, HopT, HopSX); sy = keyframes(p, HopT, HopSY)
            sh = 1f + ty / 18f * 0.3f + (sx - 1f) * 0.6f
            sa = 1f + ty / 18f * 0.4f
        }
    }
    out[0] = ty * amp
    out[1] = 1f + (sx - 1f) * amp
    out[2] = 1f + (sy - 1f) * amp
    out[3] = rot * amp
    out[4] = 1f + (sh - 1f) * amp
    out[5] = 1f + (sa - 1f) * amp
}

/** The listening look-around (mLook keyframes): writes x, y. */
internal fun lookAround(t: Float, out: FloatArray) {
    val p = frac(t / 6f)
    out[0] = keyframes(p, LookT, LookX)
    out[1] = keyframes(p, LookT, LookY)
}
