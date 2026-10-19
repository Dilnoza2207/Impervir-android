package com.impervir.agent.ui.ember

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.impervir.agent.ui.theme.Accent
import com.impervir.agent.ui.theme.Lilac
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------------------------
// Shared, immutable drawing resources (built once, in viewBox units)

private fun svgPath(d: String): Path = PathParser().parsePathString(d).toPath()

internal object EmberArt {
    val Ink = Color(0xFF1B0F0B)
    val Tongue = Color(0xFFFF7D8E)
    val Blush = Color(0xFFFF8FA3)
    val HandLine = Color(0xFFC4481B)
    val White = Color.White

    val BodyBrush = Brush.verticalGradient(
        0f to Color(0xFFFFB28C), 0.5f to Accent, 1f to Color(0xFFCF3A0A), startY = 4f, endY = 198f,
    )
    val VolumeBrush = Brush.radialGradient(
        0f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color(0x38A02800),
        center = Offset(80f, 92f), radius = 150f,
    )
    val RimBrush = Brush.linearGradient(
        0f to Color(0xA6FFFFFF), 0.45f to Color(0x2EFFFFFF), 0.75f to Color(0x00FFFFFF), 1f to Color(0x40FFD9C2),
        start = Offset(30f, 30f), end = Offset(175f, 200f),
    )
    /** Unit-radius soft blobs, drawn scaled. */
    val SpecBrush = Brush.radialGradient(
        0f to Color(0xD9FFFFFF), 0.5f to Color(0x8CFFFFFF), 1f to Color(0x00FFFFFF), center = Offset.Zero, radius = 1f,
    )
    val BlushBrush = Brush.radialGradient(
        0f to Blush, 0.55f to Blush.copy(alpha = 0.8f), 1f to Blush.copy(alpha = 0f), center = Offset.Zero, radius = 1f,
    )
    val OcclusionBrush = Brush.radialGradient(
        0f to Color(0x406E1400), 1f to Color(0x006E1400), center = Offset.Zero, radius = 1f,
    )
    val ShadowBrush = Brush.radialGradient(
        0f to Color(0x47141318), 0.55f to Color(0x24141318), 1f to Color(0x00141318), center = Offset.Zero, radius = 1f,
    )
    val GlowBrush = Brush.radialGradient(
        0f to Color(0x40FFFFFF), 1f to Color(0x00FFFFFF), center = Offset.Zero, radius = 1f,
    )
    val AccentGlow = Brush.radialGradient(
        0f to Accent.copy(alpha = 0.55f), 1f to Accent.copy(alpha = 0f), center = Offset.Zero, radius = 1f,
    )
    val HandBrush = Brush.verticalGradient(listOf(Color(0xFFFFC9AD), Color(0xFFFF7440)), startY = -20f, endY = 19f)
    val HandLineBrush = SolidColor(HandLine)
    val LidBrush = Brush.verticalGradient(listOf(Color(0xFF37333F), Color(0xFF221F28)), startY = 128f, endY = 190f)
    val LidSheen = Brush.linearGradient(
        0f to Color(0x1FFFFFFF), 0.5f to Color(0x00FFFFFF), start = Offset(50f, 128f), end = Offset(110f, 190f),
    )
    val NoteBrush = Brush.verticalGradient(listOf(Color(0xFFF8EBDA), Color(0xFFE6CDAE)), startY = -31f, endY = 31f)

    // Face (from the reference SVG)
    val BrowL = svgPath("M66 79 Q76 74 86 78")
    val BrowR = svgPath("M114 78 Q124 74 134 79")
    val HappyEyes = svgPath("M66 101 Q76 87 86 101 M114 101 Q124 87 134 101")
    val ClosedEyes = svgPath("M67 98 Q76 105 85 98 M115 98 Q124 105 133 98")
    val Smile = svgPath("M90 121 Q100 131 110 121")
    val Grin = svgPath("M84 117 Q100 141 116 117 Z")
    val Smirk = svgPath("M89 123 Q102 130 112 118")
    val Flat = svgPath("M93 124 L108 121")
    val Gloss = svgPath("M31 122 C29 86 44 60 76 47")
    val TipGleam = svgPath("M147 25 C153 19 156 13 157 8")
    val Sweat = svgPath("M40 52 C46 62 48 68 44 72 C40 76 34 73 34 68 C34 64 36 60 40 52 Z")
    /** Four-point sparkle, unit radius. */
    val Star = svgPath("M0 -1 L0.22 -0.22 L1 0 L0.22 0.22 L0 1 L-0.22 0.22 L-1 0 L-0.22 -0.22 Z")
    /** A "z", unit square. */
    val Zed = svgPath("M0 0 L1 0 L0 1 L1 1")

    val Stroke45 = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val Stroke54 = Stroke(width = 5.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val Stroke5 = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val Stroke4 = Stroke(width = 4f, cap = StrokeCap.Round)
    val Stroke3 = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val Stroke15 = Stroke(width = 1.5f)
    val GlossStroke = Stroke(width = 3.5f, cap = StrokeCap.Round)
    val GleamStroke = Stroke(width = 2.6f, cap = StrokeCap.Round)
    val RimStroke = Stroke(width = 5f)
    val ZedStroke = Stroke(width = 0.17f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val CreaseStroke = Stroke(width = 1.4f, cap = StrokeCap.Round)

    val Pen = Color(0xFF2A2730)
    val Nib = Color(0xFFF2D3B8)
    val Spark = Color(0xFFFFC94D)
    val Dots = Color(0xFFB9A8FF)
    val Zz = Color(0xFF8E8A93)
    val SweatColor = Color(0xFF8FD3FF)
    val Ring = Color(0xFF4A4550)
    val Page = Color(0xFFFFFDF8)
    val Base = Color(0xFF46424D)
}

// ---------------------------------------------------------------------------------------------------------------
// Animated state: one spring system driven by a single frame clock

private object I {
    const val EYE_X = 0
    const val EYE_Y = 1
    const val EYE_S = 2
    const val BLY = 3
    const val BLR = 4
    const val BRY = 5
    const val BRR = 6
    const val CHEEK = 7
    const val HL = 8   // x, y, rot, curl
    const val HR = 12
    const val HLM = 16 // motion dx, dy, rot
    const val HRM = 19
    const val B_TY = 22
    const val B_SX = 23
    const val B_SY = 24
    const val B_ROT = 25
    const val SH_S = 26
    const val SH_A = 27
    const val TILT = 28
    const val SQUASH = 29
    const val EYEW = 30
    const val MOUTHW = 33
    const val PROPW = 42
    const val N = 49
}

internal class EmberRig {
    // Inputs (set from composition)
    var mood: EmberMood = EmberMood.Idle
    var hands: EmberHands? = null
    var tilt = 0f
    var squash = 1f
    var lookAt: Offset? = null
    /** Motion amplitude multiplier (smaller when the system asks for reduced motion). */
    var motionAmp = 1f

    /** Bumped every frame; reading it in a draw lambda schedules a redraw without recomposition. */
    val frame = mutableIntStateOf(0)

    private var time = 0f
    private var lastNanos = -1L
    private val x = FloatArray(I.N)
    private val v = FloatArray(I.N)
    private val target = FloatArray(I.N)
    private val k = FloatArray(I.N)
    private val c = FloatArray(I.N)
    private val tmp = FloatArray(6)
    private val rnd = Random(System.nanoTime())

    private var nextBlink = 1.2f + rnd.nextFloat() * 2f
    private var blinkStart = -10f
    private var gazeX = 0f
    private var gazeY = 0f
    private var nextGaze = 2f
    private var leftLayer = LAYER_FRONT
    private var rightLayer = LAYER_FRONT

    // last known geometry (for EmberHands fractions)
    private var boxW = 0f
    private var boxH = 0f

    private val bodyPath = Path()
    private val wobblePath = Path()

    init {
        fun group(from: Int, to: Int, stiffness: Float, damping: Float) {
            for (i in from until to) {
                k[i] = stiffness; c[i] = 2f * damping * sqrt(stiffness)
            }
        }
        group(I.EYE_X, I.HL, 240f, 0.62f)            // face: lively, slight overshoot
        group(I.HL, I.HLM, 170f, 0.55f)              // hand poses: springy like the CSS cubic-bezier(.3,1.35,.5,1)
        group(I.HLM, I.B_TY, 1400f, 0.9f)            // hand loop motion: tracks the loop, smooths switches
        group(I.B_TY, I.TILT, 1200f, 0.85f)          // body loop motion
        group(I.TILT, I.EYEW, 260f, 0.7f)            // tilt / squash from the caller
        group(I.EYEW, I.MOUTHW, 300f, 0.9f)          // eye kinds
        group(I.MOUTHW, I.N, 320f, 0.72f)            // mouths and props pop in
        x[I.EYE_S] = 1f; x[I.B_SX] = 1f; x[I.B_SY] = 1f; x[I.SH_S] = 1f; x[I.SH_A] = 1f; x[I.SQUASH] = 1f
    }

    private var initialized = false

    /** Snaps every spring to the current mood (first frame shows the pose, no fly-in). */
    private fun ensureInit() {
        if (initialized) return
        initialized = true
        computeTargets()
        for (i in 0 until I.N) { x[i] = target[i]; v[i] = 0f }
    }

    fun tick(nanos: Long) {
        ensureInit()
        if (lastNanos < 0L) {
            lastNanos = nanos
            frame.intValue++
            return
        }
        val dt = ((nanos - lastNanos) / 1e9f).coerceIn(0f, 0.05f)
        lastNanos = nanos
        time += dt
        computeTargets()
        // semi-implicit Euler in small fixed substeps (stable for the stiff loop springs)
        var remaining = dt
        while (remaining > 0f) {
            val h = min(remaining, 1f / 240f)
            remaining -= h
            for (i in 0 until I.N) {
                v[i] += (k[i] * (target[i] - x[i]) - c[i] * v[i]) * h
                x[i] += v[i] * h
            }
        }
        frame.intValue++
    }

    private fun computeTargets() {
        val spec = MoodSpecs[mood.ordinal]
        val t = time
        val amp = motionAmp

        // gaze
        val look = lookAt
        if (look != null) {
            target[I.EYE_X] = look.x.coerceIn(-1f, 1f) * 6f
            target[I.EYE_Y] = look.y.coerceIn(-1f, 1f) * 5f
        } else {
            when (spec.gaze) {
                GazeMode.Wander -> {
                    if (t >= nextGaze) {
                        if (rnd.nextFloat() < 0.4f) { gazeX = 0f; gazeY = 0f } else {
                            gazeX = (rnd.nextFloat() * 2f - 1f) * 2.6f
                            gazeY = (rnd.nextFloat() * 2f - 1f) * 1.4f
                        }
                        nextGaze = t + 1.6f + rnd.nextFloat() * 2.8f
                    }
                }
                GazeMode.Around -> { lookAround(t, tmp); gazeX = tmp[0]; gazeY = tmp[1] }
                GazeMode.Fixed -> { gazeX = 0f; gazeY = 0f }
            }
            target[I.EYE_X] = spec.eyeX + gazeX * amp
            target[I.EYE_Y] = spec.eyeY + gazeY * amp
        }
        target[I.EYE_S] = spec.eyeScale

        // blink scheduling (natural random intervals, sometimes a double blink)
        if (t >= nextBlink) {
            blinkStart = t
            nextBlink = if (rnd.nextFloat() < 0.18f) t + 0.24f else t + 2.2f + rnd.nextFloat() * 3.6f
        }

        // brows (talking moods bob them with the hands)
        val bob = if (spec.browBob != 0f) -spec.browBob * wave01(t, 1.1f) * amp else 0f
        target[I.BLY] = spec.browLY + bob
        target[I.BLR] = spec.browLR
        target[I.BRY] = spec.browRY + bob
        target[I.BRR] = spec.browRR
        target[I.CHEEK] = spec.cheek

        // hands
        val over = hands
        if (over != null && boxW > 0f) {
            val s = scaleFor(boxW, boxH)
            val ox = boxW / 2f - 100f * s
            val oy = boxH / 2f - 105f * s
            target[I.HL] = (over.left.x * boxW - ox) / s
            target[I.HL + 1] = (over.left.y * boxH - oy) / s
            target[I.HL + 2] = over.leftRotation
            target[I.HL + 3] = over.leftCurl
            target[I.HR] = (over.right.x * boxW - ox) / s
            target[I.HR + 1] = (over.right.y * boxH - oy) / s
            target[I.HR + 2] = over.rightRotation
            target[I.HR + 3] = over.rightCurl
            for (i in 0 until 3) { target[I.HLM + i] = 0f; target[I.HRM + i] = 0f }
            leftLayer = LAYER_FRONT; rightLayer = LAYER_FRONT
        } else {
            setHand(I.HL, I.HLM, spec.left, t, amp)
            setHand(I.HR, I.HRM, spec.right, t, amp)
            leftLayer = spec.left.layer; rightLayer = spec.right.layer
        }

        // body
        bodyMotion(spec.body, spec.bodyPeriod, t, amp, tmp)
        target[I.B_TY] = tmp[0]; target[I.B_SX] = tmp[1]; target[I.B_SY] = tmp[2]
        target[I.B_ROT] = tmp[3]; target[I.SH_S] = tmp[4]; target[I.SH_A] = tmp[5]
        target[I.TILT] = tilt
        target[I.SQUASH] = squash.coerceIn(0.4f, 1.8f)

        // discrete parts as spring weights
        for (i in 0 until 3) target[I.EYEW + i] = if (spec.eyes.ordinal == i) 1f else 0f
        for (i in 0 until 9) target[I.MOUTHW + i] = if (spec.mouth.ordinal == i) 1f else 0f
        for (i in 0 until Prop.COUNT) target[I.PROPW + i] = if (spec.has(i) && (i != Prop.PEN || over == null)) 1f else 0f
    }

    private fun setHand(base: Int, mo: Int, pose: HandPose, t: Float, amp: Float) {
        target[base] = pose.x; target[base + 1] = pose.y; target[base + 2] = pose.rot; target[base + 3] = pose.curl
        handMotion(pose, t, amp, tmp)
        target[mo] = tmp[0]; target[mo + 1] = tmp[1]; target[mo + 2] = tmp[2]
    }

    private fun w(i: Int) = x[i].coerceIn(0f, 1f)

    // -----------------------------------------------------------------------------------------------------------
    // Drawing

    fun draw(scope: DrawScope, detailed: Boolean) = with(scope) {
        val firstSize = boxW == 0f && hands != null
        boxW = size.width; boxH = size.height
        if (firstSize) initialized = false // hand fractions need the box size: re-snap once it is known
        ensureInit()
        val s = scaleFor(boxW, boxH)
        val ox = boxW / 2f - 100f * s
        val oy = boxH / 2f - 105f * s
        withTransform({ translate(ox, oy); scale(s, s, Offset.Zero) }) {
            drawShadow()
            drawHands(LAYER_BEHIND, detailed)
            drawBody(detailed)
            drawHands(LAYER_MID, detailed)
            drawNotepad(w(I.PROPW + Prop.NOTE))
            drawLaptop(w(I.PROPW + Prop.LAPTOP))
            drawHands(LAYER_FRONT, detailed)
            drawEffects(detailed)
        }
    }

    private fun DrawScope.drawShadow() {
        val hop = x[I.SH_S] * x[I.SQUASH].let { 1f + (1f - it) * 0.4f }
        withTransform({ translate(100f, 212f); scale(56f * hop, 8f * hop, Offset.Zero) }) {
            drawCircle(EmberArt.ShadowBrush, radius = 1f, center = Offset.Zero, alpha = x[I.SH_A].coerceIn(0.2f, 1f))
        }
    }

    private fun DrawScope.drawBody(detailed: Boolean) {
        val t = time
        val sq = x[I.SQUASH]
        val qx = 1f / sqrt(sq)
        // tip sway (always on) with a little whip from vertical body speed
        val tipDx = (4.5f * sin(TAU * t / 2.7f) + 1.5f * sin(TAU * t / 1.3f + 1f)) * motionAmp
        val tipDy = (1.2f * sin(TAU * t / 2.7f + 0.8f)) * motionAmp + (-v[I.B_TY] * 0.04f).coerceIn(-7f, 7f)
        buildBody(bodyPath, tipDx, tipDy)
        val pivot = Offset(100f, 198f)
        withTransform({
            translate(0f, x[I.B_TY])
            rotate(x[I.B_ROT] + x[I.TILT], pivot)
            scale(x[I.B_SX] * qx, x[I.B_SY] * sq, pivot)
        }) {
            drawPath(bodyPath, EmberArt.BodyBrush)
            clipPath(bodyPath) {
                drawRect(EmberArt.VolumeBrush, topLeft = Offset(0f, 0f), size = Size(200f, 200f))
                withTransform({ translate(100f, 190f); scale(70f, 16f, Offset.Zero) }) {
                    drawCircle(EmberArt.OcclusionBrush, radius = 1f, center = Offset.Zero)
                }
                val lap = w(I.PROPW + Prop.LAPTOP)
                if (lap > 0.01f) {
                    withTransform({ translate(100f, 124f); scale(62f, 34f, Offset.Zero) }) {
                        drawCircle(EmberArt.GlowBrush, radius = 1f, center = Offset.Zero, alpha = lap)
                    }
                }
                drawPath(bodyPath, EmberArt.RimBrush, style = EmberArt.RimStroke)
            }
            // specular highlight + gloss
            withTransform({ translate(68f, 64f); rotate(-32f, Offset.Zero); scale(24f, 11f, Offset.Zero) }) {
                drawCircle(EmberArt.SpecBrush, radius = 1f, center = Offset.Zero)
            }
            drawCircle(EmberArt.White, radius = 2.6f, center = Offset(57f, 70f), alpha = 0.75f)
            if (detailed) {
                drawPath(EmberArt.Gloss, EmberArt.White, alpha = 0.28f, style = EmberArt.GlossStroke)
                withTransform({ translate(tipDx * 0.8f, tipDy * 0.8f) }) {
                    drawPath(EmberArt.TipGleam, EmberArt.White, alpha = 0.45f, style = EmberArt.GleamStroke)
                }
            }
            drawFace(detailed)
        }
    }

    private fun buildBody(p: Path, dx: Float, dy: Float) {
        p.reset()
        p.moveTo(100f, 198f)
        p.cubicTo(30f, 198f, 18f, 160f, 20f, 118f)
        p.cubicTo(22f, 70f, 52f, 40f, 96f, 36f)
        p.cubicTo(130f + dx * 0.2f, 33f + dy * 0.1f, 150f + dx * 0.6f, 20f + dy * 0.6f, 156f + dx, 2f + dy)
        p.cubicTo(176f + dx * 0.6f, 22f + dy * 0.6f, 178f + dx * 0.2f, 52f + dy * 0.1f, 168f, 72f)
        p.cubicTo(180f, 90f, 182f, 106f, 180f, 124f)
        p.cubicTo(178f, 170f, 160f, 198f, 100f, 198f)
        p.close()
    }

    private fun DrawScope.drawFace(detailed: Boolean) {
        val t = time
        val ex = x[I.EYE_X]
        val ey = x[I.EYE_Y]

        // cheeks (soft blush, slight parallax)
        val cheekA = (x[I.CHEEK] * 1.2f).coerceIn(0f, 1f)
        for (cx in CheekX) {
            withTransform({ translate(cx + ex * 0.2f, 121f + ey * 0.1f); scale(12.5f, 7.5f, Offset.Zero) }) {
                drawCircle(EmberArt.BlushBrush, radius = 1f, center = Offset.Zero, alpha = cheekA)
            }
        }

        // brows
        if (detailed) {
            val bx = ex * 0.4f
            withTransform({ translate(bx, x[I.BLY] + ey * 0.2f); rotate(x[I.BLR], Offset(76f, 77f)) }) {
                drawPath(EmberArt.BrowL, EmberArt.Ink, style = EmberArt.Stroke4)
            }
            withTransform({ translate(bx, x[I.BRY] + ey * 0.2f); rotate(x[I.BRR], Offset(124f, 77f)) }) {
                drawPath(EmberArt.BrowR, EmberArt.Ink, style = EmberArt.Stroke4)
            }
        }

        // eyes
        val es = x[I.EYE_S]
        val open = x[I.EYEW].coerceIn(0f, 1.15f)
        if (open > 0.02f) {
            for (e in 0..1) {
                val cx = if (e == 0) 76f else 124f
                val sy = es * open * blink(t - e * 0.04f)
                if (sy <= 0.02f) continue
                withTransform({ translate(ex, ey); scale(es, sy, Offset(cx, 98f)) }) {
                    drawOval(EmberArt.Ink, topLeft = Offset(cx - 9.5f, 84.5f), size = Size(19f, 27f))
                    drawCircle(EmberArt.White, radius = 3.4f, center = Offset(cx + 3.5f - ex * 0.3f, 92f - ey * 0.25f))
                    if (detailed) {
                        drawCircle(EmberArt.White, radius = 1.5f, center = Offset(cx - 2.5f - ex * 0.3f, 104f - ey * 0.25f), alpha = 0.7f)
                    }
                }
            }
        }
        val happy = w(I.EYEW + 1)
        if (happy > 0.01f) {
            withTransform({ translate(ex, ey); scale(1f, 0.6f + 0.4f * happy, Offset(100f, 96f)) }) {
                drawPath(EmberArt.HappyEyes, EmberArt.Ink, alpha = happy, style = EmberArt.Stroke5)
            }
        }
        val closed = w(I.EYEW + 2)
        if (closed > 0.01f) {
            withTransform({ translate(ex, ey); scale(1f, 0.6f + 0.4f * closed, Offset(100f, 100f)) }) {
                drawPath(EmberArt.ClosedEyes, EmberArt.Ink, alpha = closed, style = EmberArt.Stroke45)
            }
        }

        // mouths (cross-faded with a little pop)
        val mx = ex * 0.35f
        val my = ey * 0.15f
        val mouthStroke = if (detailed) EmberArt.Stroke45 else EmberArt.Stroke54
        for (m in 0 until 9) {
            val wm = x[I.MOUTHW + m]
            if (wm <= 0.01f) continue
            val a = wm.coerceIn(0f, 1f)
            val sc = (0.55f + 0.45f * wm).coerceAtLeast(0f)
            withTransform({ translate(mx, my); scale(sc, sc, Offset(100f, 124f)) }) {
                when (m) {
                    0 -> drawPath(EmberArt.Smile, EmberArt.Ink, alpha = a, style = mouthStroke)
                    1 -> {
                        val talk = MoodSpecs[mood.ordinal].talk
                        val open01 = talkOpen(t, talk)
                        withTransform({ scale(1f, open01, Offset(100f, 120f)) }) {
                            drawOval(EmberArt.Ink, topLeft = Offset(91.5f, 117f), size = Size(17f, 16f), alpha = a)
                            drawOval(EmberArt.Tongue, topLeft = Offset(95f, 126.5f), size = Size(10f, 6f), alpha = a)
                        }
                    }
                    2 -> {
                        val pulse = 1f + 0.06f * sin(TAU * t / 1.6f)
                        withTransform({ scale(1f / pulse, pulse, Offset(100f, 127f)) }) {
                            drawOval(EmberArt.Ink, topLeft = Offset(93.5f, 118.5f), size = Size(13f, 17f), alpha = a)
                            drawOval(EmberArt.Tongue, topLeft = Offset(96f, 129.5f), size = Size(8f, 4.5f), alpha = a * 0.9f)
                        }
                    }
                    3 -> {
                        drawPath(EmberArt.Grin, EmberArt.Ink, alpha = a)
                        drawPath(EmberArt.Grin, EmberArt.Ink, alpha = a, style = EmberArt.Stroke3)
                        clipPath(EmberArt.Grin) {
                            drawOval(EmberArt.Tongue, topLeft = Offset(92.5f, 123f), size = Size(15f, 10f), alpha = a)
                        }
                    }
                    4 -> {
                        val wig = 0.8f * sin(TAU * t / 0.8f)
                        drawOval(EmberArt.Tongue, topLeft = Offset(103f + wig, 123f), size = Size(8f, 9.5f), alpha = a)
                        drawPath(EmberArt.Smile, EmberArt.Ink, alpha = a, style = mouthStroke)
                    }
                    5 -> drawPath(EmberArt.Smirk, EmberArt.Ink, alpha = a, style = mouthStroke)
                    6 -> {
                        val wb = 1.6f * sin(TAU * t / 0.9f)
                        wobblePath.reset()
                        wobblePath.moveTo(87f, 125f)
                        wobblePath.quadraticTo(93f, 119f + wb, 100f, 125f)
                        wobblePath.quadraticTo(107f, 131f - wb, 113f, 125f)
                        drawPath(wobblePath, EmberArt.Ink, alpha = a, style = mouthStroke)
                    }
                    7 -> drawPath(EmberArt.Flat, EmberArt.Ink, alpha = a, style = mouthStroke)
                    else -> {
                        val br = 1f + 0.15f * sin(TAU * t / 4.8f)
                        withTransform({ scale(br, br, Offset(100f, 124f)) }) {
                            drawOval(EmberArt.Ink, topLeft = Offset(96f, 120.5f), size = Size(8f, 7f), alpha = a)
                        }
                    }
                }
            }
        }
    }

    /** Eye openness for a blink at time [t]: quick close, slightly slower open. */
    private fun blink(t: Float): Float {
        val dt = t - blinkStart
        if (dt < 0f || dt > 0.17f) return 1f
        val p = dt / 0.17f
        val closeAmt = if (p < 0.4f) smooth(p / 0.4f) else 1f - smooth((p - 0.4f) / 0.6f)
        return 1f - 0.92f * closeAmt
    }

    /** Syllable-like talking: fast open/close with a slower phrase envelope. */
    private fun talkOpen(t: Float, speed: Float): Float {
        val syl = kotlin.math.abs(sin(TAU * 2.6f * speed * t))
        val phrase = 0.65f + 0.35f * sin(TAU * 0.55f * t + 1.3f)
        val jitter = 0.85f + 0.15f * sin(TAU * 4.1f * speed * t + 0.7f)
        return (0.28f + 0.72f * syl * phrase * jitter).coerceIn(0.2f, 1f)
    }

    // -----------------------------------------------------------------------------------------------------------
    // Hands

    private fun DrawScope.drawHands(layer: Int, detailed: Boolean) {
        if (leftLayer == layer) drawHandAt(left = true, detailed = detailed)
        if (rightLayer == layer) drawHandAt(left = false, detailed = detailed)
    }

    private fun DrawScope.drawHandAt(left: Boolean, detailed: Boolean) {
        val b = if (left) I.HL else I.HR
        val m = if (left) I.HLM else I.HRM
        withTransform({
            translate(x[b], x[b + 1])
            rotate(x[b + 2], Offset.Zero)
            if (left) scale(-1f, 1f, Offset.Zero)
            translate(x[m], x[m + 1] + 14f)
            rotate(x[m + 2], Offset.Zero)
            translate(0f, -14f)
        }) {
            drawHand(x[b + 3], detailed)
            if (!left) {
                val pen = w(I.PROPW + Prop.PEN)
                if (pen > 0.01f) drawPen(pen)
            }
        }
    }

    /** A soft cartoon hand in its own frame: palm centre at the origin, fingers up, thumb toward -x. */
    private fun DrawScope.drawHand(curlRaw: Float, detailed: Boolean) {
        val curl = curlRaw.coerceIn(0f, 1f)
        val baseY = -4f + 3f * curl
        val spread = 10f * (1f - 0.55f * curl)
        val thumbA = lerp(-58f, 66f, curl.pow(1.3f))
        val thumbL = lerp(12.5f, 10.5f, curl)
        val thumbX = -9f + 1.5f * curl
        val thumbOver = curl > 0.5f
        val palmTop = baseY - 2f
        val palmH = 19f - palmTop
        val o = 1.5f

        // pass 0: one merged silhouette outline
        capsule(thumbX, 9f, 8.6f, thumbL, thumbA, o, EmberArt.HandLineBrush)
        for (i in -1..1) capsule(i * 6.8f, baseY + 3f, 7.4f, fingerLen(i, curl), i * spread, o, EmberArt.HandLineBrush)
        drawOval(EmberArt.HandLine, topLeft = Offset(-12.5f - o, palmTop - o), size = Size(25f + 2 * o, palmH + 2 * o))

        // pass 1: fills (fingers keep a thin outline of their own so they read as separate fingers)
        if (!thumbOver) capsule(thumbX, 9f, 8.6f, thumbL, thumbA, 0f, EmberArt.HandBrush)
        for (i in FingerOrder) {
            val len = fingerLen(i, curl)
            if (detailed) capsule(i * 6.8f, baseY + 3f, 7.4f, len, i * spread, 0.9f, EmberArt.HandLineBrush)
            capsule(i * 6.8f, baseY + 3f, 7.4f, len, i * spread, 0f, EmberArt.HandBrush)
        }
        drawOval(EmberArt.HandBrush, topLeft = Offset(-12.5f, palmTop), size = Size(25f, palmH))
        if (thumbOver) {
            capsule(thumbX, 9f, 8.6f, thumbL, thumbA, 1.1f, EmberArt.HandLineBrush)
            capsule(thumbX, 9f, 8.6f, thumbL, thumbA, 0f, EmberArt.HandBrush)
        }
        // soft highlight on the palm
        drawOval(EmberArt.White, topLeft = Offset(-8.5f, palmTop + 3f), size = Size(7f, 10f), alpha = 0.32f)
        if (detailed) {
            // nails while the fingers are open
            val nailA = (1f - curl / 0.6f).coerceIn(0f, 1f) * 0.4f
            if (nailA > 0.01f) {
                for (i in -1..1) {
                    val len = fingerLen(i, curl)
                    withTransform({ rotate(i * spread, Offset(i * 6.8f, baseY + 3f)) }) {
                        drawOval(
                            EmberArt.White, topLeft = Offset(i * 6.8f - 1.8f, baseY + 3f - len + 1.2f),
                            size = Size(3.6f, 3.2f), alpha = nailA,
                        )
                    }
                }
            }
            // knuckle creases when curled
            if (curl > 0.55f) {
                val ca = ((curl - 0.55f) / 0.45f).coerceIn(0f, 1f) * 0.35f
                for (i in -1..0) {
                    val cx = i * 6.8f + 3.4f
                    drawLine(EmberArt.HandLine, Offset(cx, palmTop + 0.5f), Offset(cx, palmTop + 4.5f), strokeWidth = 1.4f, cap = StrokeCap.Round, alpha = ca * 2f)
                }
            }
        }
    }

    private fun fingerLen(i: Int, curl: Float) = (if (i == 0) 15f else 12.5f) * (1f - 0.7f * curl) + 3f

    /** A rounded finger: base at (cx, by), extends [len] up (rotated by [angle]), inflated by [inf]. */
    private fun DrawScope.capsule(cx: Float, by: Float, w: Float, len: Float, angle: Float, inf: Float, brush: Brush) {
        withTransform({ rotate(angle, Offset(cx, by)) }) {
            drawRoundRect(
                brush,
                topLeft = Offset(cx - w / 2f - inf, by - len - inf),
                size = Size(w + 2f * inf, len + w / 2f + 2f * inf),
                cornerRadius = CornerRadius(w / 2f + inf),
            )
        }
    }

    private fun DrawScope.drawPen(a: Float) {
        withTransform({ scale(0.6f + 0.4f * a, 0.6f + 0.4f * a, Offset(0f, 0f)) }) {
            drawRoundRect(EmberArt.Pen, topLeft = Offset(-3.5f, -32f), size = Size(7f, 46f), cornerRadius = CornerRadius(3.5f), alpha = a)
            drawRoundRect(Lilac, topLeft = Offset(-3.5f, -32f), size = Size(7f, 10f), cornerRadius = CornerRadius(3.5f), alpha = a)
            drawRoundRect(EmberArt.White, topLeft = Offset(-2f, -29f), size = Size(1.6f, 36f), cornerRadius = CornerRadius(0.8f), alpha = a * 0.25f)
        }
    }

    // -----------------------------------------------------------------------------------------------------------
    // Props

    /** Notepad held up with its back to us (we never see the text), spiral on top, elastic band. */
    private fun DrawScope.drawNotepad(a: Float) {
        if (a <= 0.01f) return
        val sc = 0.6f + 0.4f * a
        withTransform({ translate(60f, 167f); rotate(-9f, Offset.Zero); scale(sc, sc, Offset.Zero) }) {
            drawRoundRect(Color.Black, topLeft = Offset(-24f, -28f), size = Size(52f, 62f), cornerRadius = CornerRadius(7f), alpha = 0.12f * a)
            drawRoundRect(EmberArt.Page, topLeft = Offset(-25f, -34f), size = Size(50f, 10f), cornerRadius = CornerRadius(3f), alpha = a)
            drawRoundRect(EmberArt.NoteBrush, topLeft = Offset(-26f, -31f), size = Size(52f, 62f), cornerRadius = CornerRadius(7f), alpha = a)
            drawRoundRect(Color(0xFF141318), topLeft = Offset(-26f, -31f), size = Size(52f, 62f), cornerRadius = CornerRadius(7f), alpha = 0.14f * a, style = EmberArt.Stroke15)
            drawRect(Lilac, topLeft = Offset(15f, -31f), size = Size(4.5f, 62f), alpha = a)
            drawRoundRect(EmberArt.White, topLeft = Offset(-21f, -26f), size = Size(3f, 40f), cornerRadius = CornerRadius(1.5f), alpha = 0.35f * a)
            for (r in 0 until 6) {
                drawRoundRect(EmberArt.Ring, topLeft = Offset(-21.6f + r * 8f, -36f), size = Size(3.2f, 8.5f), cornerRadius = CornerRadius(1.6f), alpha = a)
            }
        }
    }

    /** A small laptop seen from behind its lid. */
    private fun DrawScope.drawLaptop(a: Float) {
        if (a <= 0.01f) return
        val sc = 0.6f + 0.4f * a
        withTransform({ scale(sc, sc, Offset(100f, 196f)) }) {
            val r = CornerRadius(9f)
            drawRoundRect(EmberArt.LidBrush, topLeft = Offset(50f, 128f), size = Size(100f, 62f), cornerRadius = r, alpha = a)
            drawRoundRect(EmberArt.LidSheen, topLeft = Offset(50f, 128f), size = Size(100f, 62f), cornerRadius = r, alpha = a)
            drawRoundRect(EmberArt.White, topLeft = Offset(50f, 128f), size = Size(100f, 62f), cornerRadius = r, alpha = 0.12f * a, style = EmberArt.Stroke15)
            val pulse = 0.5f + 0.5f * sin(TAU * time / 2f)
            withTransform({ translate(100f, 158f); scale(12f + 3f * pulse, 12f + 3f * pulse, Offset.Zero) }) {
                drawCircle(EmberArt.AccentGlow, radius = 1f, center = Offset.Zero, alpha = a * (0.5f + 0.5f * pulse))
            }
            drawCircle(Accent, radius = 6f, center = Offset(100f, 158f), alpha = a * (0.75f + 0.25f * pulse))
            drawRoundRect(EmberArt.Base, topLeft = Offset(36f, 188f), size = Size(128f, 9f), cornerRadius = CornerRadius(4.5f), alpha = a)
            drawRoundRect(EmberArt.White, topLeft = Offset(44f, 189f), size = Size(112f, 1.6f), cornerRadius = CornerRadius(0.8f), alpha = 0.18f * a)
        }
    }

    private fun DrawScope.drawEffects(detailed: Boolean) {
        val t = time
        // sparkles
        val sp = w(I.PROPW + Prop.SPARKS)
        if (sp > 0.01f) {
            for (i in 0 until 3) {
                val p = frac((t + SparkPhase[i]) / 1.4f)
                val tw = 0.5f - 0.5f * kotlin.math.cos(TAU * p)
                val r = SparkR[i] * (0.4f + 0.6f * tw) * (0.5f + 0.5f * sp)
                withTransform({ translate(SparkX[i], SparkY[i]); rotate(45f * tw, Offset.Zero); scale(r, r, Offset.Zero) }) {
                    drawPath(EmberArt.Star, EmberArt.Spark, alpha = sp * (0.3f + 0.7f * tw))
                }
            }
        }
        // sweat drop
        val sw = w(I.PROPW + Prop.SWEAT)
        if (sw > 0.01f) {
            val p = frac(t / 1.8f)
            val ty = -4f + 14f * p.pow(1.6f)
            val fa = if (p < 0.2f) p / 0.2f else 1f - (p - 0.2f) / 0.8f
            withTransform({ translate(0f, ty) }) {
                drawPath(EmberArt.Sweat, EmberArt.SweatColor, alpha = sw * fa)
                drawOval(EmberArt.White, topLeft = Offset(37f, 63f), size = Size(3f, 5f), alpha = sw * fa * 0.8f)
            }
        }
        // floating z's
        val zz = w(I.PROPW + Prop.ZZ)
        if (zz > 0.01f) {
            for (i in 0 until 3) {
                val p = frac((t + i * 0.87f) / 2.6f)
                val fa = if (p < 0.4f) p / 0.4f else 1f - (p - 0.4f) / 0.6f
                val sz = (9f + 6f * p) * (if (detailed) 1f else 1.15f)
                withTransform({ translate(26f - 14f * p, 30f - 26f * p); scale(sz, sz, Offset.Zero) }) {
                    drawPath(EmberArt.Zed, EmberArt.Zz, alpha = zz * fa, style = EmberArt.ZedStroke)
                }
            }
        }
        // thought dots
        val dots = w(I.PROPW + Prop.DOTS)
        if (dots > 0.01f) {
            for (i in 0 until 3) {
                val pa = 0.25f + 0.75f * wave01(t - i * 0.25f, 1.8f)
                val r = DotR[i] * (0.5f + 0.5f * dots)
                drawCircle(EmberArt.Dots, radius = r, center = Offset(DotX[i], DotY[i]), alpha = dots * pa)
                if (detailed) {
                    drawCircle(EmberArt.White, radius = r * 0.32f, center = Offset(DotX[i] - r * 0.35f, DotY[i] - r * 0.35f), alpha = dots * pa * 0.6f)
                }
            }
        }
    }

    private companion object {
        val CheekX = floatArrayOf(57f, 143f)
        val FingerOrder = intArrayOf(-1, 1, 0)
        val SparkX = floatArrayOf(30f, 188f, 12f)
        val SparkY = floatArrayOf(42f, 106f, 99f)
        val SparkR = floatArrayOf(12f, 8f, 7f)
        val SparkPhase = floatArrayOf(0f, -0.5f, -0.9f)
        val DotX = floatArrayOf(70f, 58f, 40f)
        val DotY = floatArrayOf(22f, 6f, -8f)
        val DotR = floatArrayOf(5f, 6.5f, 8f)
    }
}

/** viewBox units -> px: the design spans x -8..208, y -10..222 (hands, sparkles and thought dots included). */
internal fun scaleFor(w: Float, h: Float) = min(w / 216f, h / 232f)
