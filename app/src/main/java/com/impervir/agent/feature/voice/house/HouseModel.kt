package com.impervir.agent.feature.voice.house

import androidx.compose.ui.graphics.Color
import com.impervir.agent.ui.ember.EmberMood

/*
 * Ember's house: 5 rooms side by side (480 world units each), 5 tasks per room (same as the iOS design).
 * Hand poses are in Ember's own 200 x 220 design box (x, y, degrees), like the iOS rig.
 */

internal const val ROOM_W = 480f
internal const val STAGE_W = 364f
internal const val STAGE_H = 300f
internal const val WALL_H = 238f
internal const val TASK_SECONDS = 4.8f

internal class Room(val name: String, val wall: Color, val base: Color)

internal val Rooms = listOf(
    Room("Listening Studio", Color(0xFFF7E3D5), Color(0xFFEFD2BF)),
    Room("Memory Library", Color(0xFFECE6F8), Color(0xFFDDD4F0)),
    Room("Writing Workshop", Color(0xFFF4ECDD), Color(0xFFE7DBC6)),
    Room("Insight Lab", Color(0xFFE6E1F4), Color(0xFFD8D0EA)),
    Room("Post Office", Color(0xFFF9E5D8), Color(0xFFEFD3C1)),
)

internal class Pose(val x: Float, val y: Float, val deg: Float)

/** Hand motion loops (the iOS keyframes); channels: translateX, translateY, rotate, scale. Origin (0, 14) in hand space. */
internal enum class Hm(val keys: Keys?) {
    None(null),
    Idle(k(f(0f, .5f, 1f), ty = f(0f, -4f, 0f))),
    Swing(k(f(0f, .25f, .75f, 1f), rot = f(0f, -14f, 14f, 0f))),
    SwingB(k(f(0f, .25f, .75f, 1f), rot = f(0f, 14f, -14f, 0f))),
    HpOn(k(f(0f, .15f, .45f, .5f, .58f, 1f), ty = f(0f, -30f, 0f, 2f, 0f, 0f))),
    Twist(k(f(0f, .5f, 1f), rot = f(0f, -18f, 0f))),
    FadA(k(f(0f, .5f, 1f), ty = f(0f, -6f, 0f))),
    FadB(k(f(0f, .5f, 1f), ty = f(0f, 5f, 0f))),
    Beckon(k(f(0f, .5f, 1f), ty = f(0f, -4f, 0f), rot = f(0f, -22f, 0f))),
    Scrib(k(f(0f, .25f, .5f, .75f, 1f), tx = f(0f, -4f, -8f, -4f, 0f), ty = f(0f, 2f, -1f, 3f, 0f))),
    Pull(k(f(0f, .4f, .6f, 1f), ty = f(0f, -16f, -16f, 0f))),
    ClA(k(f(0f, .5f, 1f), ty = f(0f, -12f, 0f))),
    ClB(k(f(0f, .5f, 1f), ty = f(0f, 12f, 0f))),
    Stamp(k(f(0f, .3f, .45f, .55f, .75f, 1f), ty = f(0f, -6f, 14f, 12f, 0f, 0f))),
    Drop(k(f(0f, .4f, .6f, 1f), ty = f(0f, 18f, 18f, 0f))),
    Type(k(f(0f, .5f, 1f), ty = f(0f, -6f, 0f))),
    TypeB(k(f(0f, .25f, .5f, .75f, 1f), ty = f(0f, -5f, 0f, -5f, 0f))),
    Throw(k(f(0f, .2f, .4f, .55f, 1f), tx = f(0f, -6f, 14f, 0f, 0f), ty = f(0f, 8f, -14f, 0f, 0f))),
    Dip(k(f(0f, .3f, .6f, .8f, 1f), ty = f(0f, 14f, -12f, 0f, 0f))),
    Shake(k(f(0f, .5f, 1f), ty = f(0f, 2f, 0f))),
    Press(k(f(0f, .55f, .7f, 1f), ty = f(0f, 2f, 0f, 0f), sc = f(1f, .9f, 1f, 1f))),
    Focus(k(f(0f, .5f, 1f), rot = f(0f, -6f, 0f))),
    Tap(k(f(0f, .5f, 1f), rot = f(0f, -5f, 0f))),
    Till(k(f(0f, .3f, .42f, .52f, .7f, 1f), ty = f(0f, 0f, 8f, 8f, 0f, 0f))),
    Stick(k(f(0f, .25f, .45f, .55f, .75f, 1f), tx = f(0f, 0f, -20f, -20f, 0f, 0f), ty = f(0f, 0f, 10f, 10f, 0f, 0f))),
    Launch(k(f(0f, .08f, .15f, .25f, .52f, .6f, .7f, 1f), tx = f(0f, -8f, 8f, 0f, -8f, 8f, 0f, 0f), ty = f(0f, 6f, -10f, 0f, 6f, -10f, 0f, 0f))),
    Knob(k(f(0f, .5f, 1f), rot = f(0f, -12f, 0f))),
    Slap(k(f(0f, .3f, .45f, .58f, .75f, 1f), ty = f(0f, -8f, 12f, 10f, 0f, 0f))),
    Cheer(k(f(0f, .5f, 1f), ty = f(0f, -10f, 0f), rot = f(0f, -10f, 0f))),
}

private fun k(times: FloatArray, tx: FloatArray? = null, ty: FloatArray? = null, rot: FloatArray? = null, sc: FloatArray? = null): Keys {
    val zero = FloatArray(times.size)
    val one = FloatArray(times.size) { 1f }
    return Keys(times, EaseInOut, tx ?: zero, ty ?: zero, rot ?: zero, sc ?: one)
}

/** Whole-body motion layered on Ember while working. */
internal enum class BodyMotion { None, Nod, Climb }

internal class Task(
    val x: Float,
    val caption: String,
    val mood: EmberMood,
    val gaze: Pose,          // x, y in px of the design (about -7..7); deg unused
    val l: Pose,
    val r: Pose,
    val al: Hm = Hm.None, val alDur: Float = 1.2f,
    val ar: Hm = Hm.None, val arDur: Float = 1.2f,
    val dy: Float = 0f,
    val cam: Float = -1f,
    val body: BodyMotion = BodyMotion.None,
    val pins: List<Pose>? = null,
)

private val HIP = Pose(22f, 158f, 72f)
private fun p(x: Float, y: Float, d: Float = 0f) = Pose(x, y, d)

internal val Tasks: List<List<Task>> = listOf(
    // Listening Studio
    listOf(
        Task(100f, "Putting on the headphones", EmberMood.Listening, p(0f, -3f), p(14f, 104f, 10f), p(186f, 104f, -10f), Hm.HpOn, 4.8f, Hm.HpOn, 4.8f),
        Task(250f, "Setting your mic level", EmberMood.Listening, p(-5f, 5f), p(28f, 162f, -12f), p(170f, 166f, 20f), Hm.Twist, 1.2f),
        Task(250f, "Nodding along at the desk", EmberMood.Happy, p(0f, 2f), p(80f, 164f, -6f), p(176f, 164f, 6f), Hm.FadA, 1.2f, Hm.FadB, 1.2f, body = BodyMotion.Nod),
        Task(380f, "Catching words in a jar", EmberMood.Wow, p(5f, -5f), p(30f, 110f, -30f), p(184f, 150f, 0f), Hm.Beckon, 1.2f),
        Task(380f, "Scribbling quick notes", EmberMood.Curious, p(-6f, 6f), p(40f, 178f, -10f), p(92f, 162f, 30f), ar = Hm.Scrib, arDur = 0.6f),
    ),
    // Memory Library
    listOf(
        Task(130f, "Pulling your best post", EmberMood.Listening, p(6f, -4f), p(17f, 140f, -16f), p(186f, 82f, -6f), Hm.Idle, 2.4f, Hm.Pull, 2.4f),
        Task(66f, "Climbing to older posts", EmberMood.Curious, p(0f, -6f), p(16f, 100f, -6f), p(184f, 100f, 6f), Hm.ClA, 1.2f, Hm.ClB, 1.2f, dy = -62f, body = BodyMotion.Climb),
        Task(252f, "Reading by the lamp", EmberMood.Listening, p(0f, 7f), p(60f, 170f), p(140f, 170f)),
        Task(378f, "Stamping it: your voice", EmberMood.Proud, p(6f, 4f), p(84f, 156f, -60f), p(192f, 86f), ar = Hm.Stamp),
        Task(378f, "Filing it under your style", EmberMood.Happy, p(-6f, 4f), p(36f, 70f), p(150f, 160f, 60f), Hm.Drop, 2.4f),
    ),
    // Writing Workshop
    listOf(
        Task(116f, "Typing the first draft", EmberMood.Curious, p(0f, 6f), p(72f, 198f, -14f), p(128f, 198f, 14f), Hm.Type, 0.4f, Hm.TypeB, 0.4f, dy = -34f),
        Task(116f, "Tossing a weak draft", EmberMood.Concerned, p(6f, 4f), p(66f, 190f, -30f), p(170f, 150f, -10f), ar = Hm.Throw, arDur = 2.4f, dy = -34f),
        Task(300f, "Picking the right words", EmberMood.Thinking, p(-6f, -3f), p(-12f, 58f, 10f), p(176f, 150f, 14f), Hm.Dip, 2.4f, Hm.Idle, 2.4f),
        Task(300f, "Sharpening a pencil", EmberMood.Curious, p(0f, 6f), p(82f, 154f), p(140f, 150f, 90f), ar = Hm.Shake, arDur = 0.4f),
        Task(
            340f, "Pinning drafts to the board", EmberMood.Happy, p(7f, -5f), HIP, p(196f, 10f, -10f), ar = Hm.Press, arDur = 1.6f,
            pins = listOf(p(196f, 10f, -10f), p(236f, -16f, -10f), p(206f, 46f, -10f)),
        ),
    ),
    // Insight Lab
    listOf(
        Task(100f, "Watching the trend stars", EmberMood.Wow, p(4f, -4f), HIP, p(182f, 54f, -43f), ar = Hm.Focus, arDur = 2.4f),
        Task(200f, "Mixing a test potion", EmberMood.Curious, p(6f, 2f), HIP, p(200f, 60f, 110f)),
        Task(260f, "Checking what sold", EmberMood.Proud, p(7f, -3f), HIP, p(192f, 31f, 50f), ar = Hm.Tap),
        Task(260f, "Weighing two ideas", EmberMood.Thinking, p(5f, 6f), p(22f, 150f), p(176f, 110f), Hm.Idle, 2.4f),
        Task(380f, "Ringing up a \$29 sale", EmberMood.Happy, p(6f, 3f), p(60f, 166f, -60f), p(188f, 98f, 20f), ar = Hm.Till),
    ),
    // Post Office
    listOf(
        Task(110f, "Sticking on stamps", EmberMood.Curious, p(0f, 7f), p(64f, 196f), p(136f, 148f, -30f), ar = Hm.Stick),
        Task(200f, "Flying posts to your platforms", EmberMood.Happy, p(6f, -6f), p(30f, 130f, -20f), p(176f, 90f, -30f), Hm.Idle, 2.4f, Hm.Launch, 4.8f, cam = 66f),
        Task(200f, "Setting the posting time", EmberMood.Curious, p(-4f, 6f), p(66f, 196f), p(112f, 168f, -80f), ar = Hm.Knob, arDur = 0.6f, cam = 66f),
        Task(360f, "Dropping letters in the box", EmberMood.Listening, p(7f, 3f), HIP, p(228f, 40f), ar = Hm.Drop, arDur = 2.4f),
        Task(280f, "Ringing the send bell", EmberMood.Happy, p(6f, 2f), p(16f, 70f, -26f), p(212f, 70f), Hm.Cheer, 1.2f, Hm.Slap, 1.2f),
    ),
)

internal val IdlePose = floatArrayOf(16f, 120f, -18f, 184f, 120f, 18f)
internal val WalkPose = floatArrayOf(16f, 120f, -22f, 184f, 120f, 22f)
