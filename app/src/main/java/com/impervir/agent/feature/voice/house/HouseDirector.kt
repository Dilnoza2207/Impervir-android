package com.impervir.agent.feature.voice.house

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.impervir.agent.ui.ember.EmberMood
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

// Ember rig springs (ui/ember/EmberRig): hands k=170 zeta=.55, tilt/squash k=260 zeta=.7.
private const val RIG_HAND_K = 170f
private const val RIG_BODY_K = 260f
private val RIG_HAND_C = 2f * 0.55f * sqrt(RIG_HAND_K)
private val RIG_BODY_C = 2f * 0.7f * sqrt(RIG_BODY_K)

internal enum class Phase { Work, Prep, Move, Land }

@Immutable
internal data class Caption(val room: String, val task: String)

private val MoveEase = CubicBezierEasing(0.45f, 0.05f, 0.3f, 1f)
private val CamEase = CubicBezierEasing(0.4f, 0.05f, 0.25f, 1f)
private val SettleEase = CubicBezierEasing(0.3f, 0.7f, 0.4f, 1f)
private val HandSpring = CubicBezierEasing(0.3f, 1.25f, 0.5f, 1f)

// Hop while walking: [y, squash] over one 0.6 s step (ease-out up, ease-in down).
private val Hop = Keys(f(0f, .15f, .5f, .85f, 1f), EaseOut, f(0f, 0f, -9f, 0f, 0f), f(1f, .95f, 1.04f, .96f, 1f))
    .eased(EaseOut, EaseOut, EaseIn, EaseOut)
// Landing after a walk: [tiltFactor, squash].
private val LandMoved = Keys(f(0f, .35f, .68f, 1f), SettleEase, f(1f, 0f, 0f, 0f), f(1f, .87f, 1.04f, 1f))
// Little hop in place when the next task is right here: [y, squash].
private val LandHere = Keys(f(0f, .3f, .62f, 1f), SettleEase, f(0f, -7f, 0f, 0f), f(.9f, 1.05f, .93f, 1f))

/**
 * Runs Ember's day in the house: work ~4.8 s per task, crouch, hop (with lean and squash-and-stretch) to the next spot
 * while the camera pans, land, repeat; room 5 loops back to room 1 through a twin of room 1 (slot 5), so nothing jumps.
 * Plain mutable fields are read by the canvases every frame; only low-frequency values are snapshot state.
 */
@Stable
internal class HouseDirector {
    /** Bumped every frame; canvases and Ember read it so they redraw. */
    var frame by mutableLongStateOf(0L); private set
    var targetRoom by mutableIntStateOf(0); private set
    var caption by mutableStateOf(Caption(Rooms[0].name, Tasks[0][0].caption)); private set
    var working by mutableStateOf(true); private set
    var paused by mutableStateOf(false)

    // --- simulation ---
    var time = 0f; private set
    var room = 0; private set
    var task = 0; private set
    var slot = 0; private set
    var phase = Phase.Work; private set
    var phaseT = 0f; private set
    private var workStart = 0f
    /** Seconds since the current (or last) task started; task props loop on it, in sync with the hands. */
    val workT: Float get() = time - workStart

    private var fromX = 100f; private var toX = 100f
    private var fromDy = 0f; private var toDy = 0f
    private var fromCam = 0f; private var toCam = 0f
    private var mdur = 0f; private var camDur = 0f; private var departAt = -10f
    private var dir = 0f
    private var moved = false

    // --- outputs (world units) ---
    var ex = 100f; private set
    var dy = 0f; private set
    var cam = 0f; private set
    var tilt = 0f; private set
    var squash = 1f; private set
    var bodyY = 0f; private set
    var gazeX = 0f; private set
    var gazeY = 0f; private set

    /** Per hand (left at 0, right at 7): poseX, poseY, poseDeg, motionX, motionY, motionRot, motionScale (design box units). */
    val hands = FloatArray(14)
    /** Hand centres in the design box: lx, ly, rx, ry. */
    val handCenter = FloatArray(4)

    /**
     * What Ember's rig gets: lx, ly, lRot, rx, ry, rRot (design box), tilt, squash. The rig eases its hands and body
     * through springs; these targets are pre-compensated (feed-forward through the spring) so the mittens land exactly
     * where the props are drawn, even on fast loops like typing.
     */
    val rigOut = FloatArray(8)
    private val rigRaw = FloatArray(8)
    private val ffPrev = FloatArray(8)
    private val ffVel = FloatArray(8)
    private var ffPrimed = false

    // --- props state ---
    var phones = 0f; private set
    var lamp = 0.3f; private set
    var trend = 0f; private set
    var words3 = 0f; private set
    var stream31 = 0f; private set
    var propAlpha = 1f; private set
    var propRoom = 0; private set
    var propTask = 0; private set
    var pins = 0; private set
    val pinAt = FloatArray(3)

    // --- transitions ---
    private var sqFrom = 1f; private var sqTo = 1f
    private var tiltFrom = 0f; private var tiltTo = 0f
    private var bobFrom = 0f; private var trAt = -10f
    private val poseFrom = FloatArray(6)
    private val poseTo = FloatArray(6)
    private val poseCur = FloatArray(6)
    private var poseAt = -10f
    private var pinIdx = -1
    private val motionFrom = FloatArray(8)
    private val motionCur = FloatArray(8)
    private var motionAt = -10f

    init {
        motionCur[3] = 1f; motionCur[7] = 1f; motionFrom[3] = 1f; motionFrom[7] = 1f
        val t = Tasks[0][0]
        setPose(t.l, t.r, snap = true)
        compute(0f)
    }

    /** True while Ember works on (room, task): its room props animate. */
    fun active(r: Int, k: Int) = phase == Phase.Work && room == r && task == k

    val mood: EmberMood
        get() = when {
            paused -> EmberMood.Sleeping
            phase == Phase.Work -> Tasks[room][task].mood
            else -> EmberMood.Idle
        }

    val sceneLabel: String
        get() = when {
            paused -> "Ember is napping in the ${Rooms[room].name}"
            working -> "Ember in the ${Rooms[room].name}: ${Tasks[room][task].caption}"
            else -> "Ember is walking over to the ${Rooms[room].name}"
        }

    fun advance(dtIn: Float) {
        if (paused) return
        val dt = min(dtIn, 0.1f)
        time += dt
        phaseT += dt
        when (phase) {
            Phase.Work -> {
                if (room == 2 && task == 4) {
                    val pt = floor(phaseT * 10f).toInt()
                    val n = min(3, (pt + 7) / 16)
                    while (pins < n) { pinAt[pins] = time; pins++ }
                    val idx = min(2, pt / 16)
                    if (idx != pinIdx) {
                        pinIdx = idx
                        val t = Tasks[2][4]
                        setPose(t.l, t.pins!![idx])
                    }
                }
                if (phaseT >= TASK_SECONDS) nextTask()
            }
            Phase.Prep -> if (phaseT >= 0.3f) depart()
            Phase.Move -> if (phaseT >= mdur) startPhase(Phase.Land)
            Phase.Land -> if (phaseT >= 0.5f) startWork()
        }
        compute(dt)
        frame++
    }

    /** Send Ember to a room (the room dots). Returns false when it is already working there. */
    fun goRoom(i: Int): Boolean {
        if (i == room && phase == Phase.Work) return false
        freeze()
        // Room 1 has a twin at slot 5: hop to whichever copy is nearer, shifting the world invisibly when the view allows.
        if (slot == 5 && i != 0 && cam >= 5 * ROOM_W - 0.5f && ex >= 5 * ROOM_W) shift(-5 * ROOM_W)
        if (i == 4 && ex < ROOM_W && cam <= 116.5f) shift(5 * ROOM_W)
        val sl = if (i == 0) (if (abs(ex - 5 * ROOM_W) < abs(ex - ROOM_W * 0.3f)) 5 else 0) else i
        plan(i, 0, sl)
        return true
    }

    // ---------------------------------------------------------------------------------------------

    private fun nextTask() {
        var r = room
        var k = task + 1
        var sl = slot
        if (k > 4) { k = 0; r = (r + 1) % 5; sl = if (r == 0) 5 else r }
        plan(r, k, sl)
    }

    private fun plan(r: Int, k: Int, sl: Int) {
        if (r != room) caption = Caption(Rooms[r].name, "Heading over")
        room = r; task = k; slot = sl
        targetRoom = r
        working = false
        sqFrom = squash; sqTo = 0.9f
        tiltFrom = tilt; tiltTo = 0f
        bobFrom = bodyY
        trAt = time
        setWalkPose()
        startPhase(Phase.Prep)
    }

    private fun depart() {
        val t = Tasks[room][task]
        fromX = ex; fromDy = dy; fromCam = cam
        toX = slot * ROOM_W + t.x
        toDy = t.dy
        toCam = camFor(slot, t)
        val dx = toX - fromX
        val ddy = toDy - fromDy
        val dist = sqrt(dx * dx + ddy * ddy * 2.25f)
        dir = sign(dx)
        departAt = time
        sqFrom = squash; tiltFrom = tilt; trAt = time
        if (dist >= 1f) {
            mdur = (((0.5f + dist / 260f) / 0.6f).roundToInt() * 0.6f).coerceIn(0.6f, 4.2f)
            camDur = mdur * 1.08f
            moved = true
            sqTo = 1f; tiltTo = 4f * dir
            snapMotion()
            startPhase(Phase.Move)
        } else {
            mdur = 0f
            camDur = if (abs(toCam - fromCam) > 0.5f) 0.9f else 0f
            moved = false
            startPhase(Phase.Land)
        }
    }

    private fun startWork() {
        val t = Tasks[room][task]
        if (slot == 5) { slot = 0; shift(-5 * ROOM_W) }
        if (room != 2) pins = 0
        propRoom = room; propTask = task
        workStart = time
        pinIdx = -1
        caption = Caption(Rooms[room].name, t.caption)
        working = true
        setPose(t.l, t.r)
        startPhase(Phase.Work)
    }

    private fun startPhase(p: Phase) {
        if (phase == Phase.Work || p == Phase.Work) snapMotion()
        phase = p
        phaseT = 0f
    }

    /** Stop any walk where it is (used when a room dot interrupts). */
    private fun freeze() {
        fromX = ex; toX = ex; fromDy = dy; toDy = dy; fromCam = cam; toCam = cam
        mdur = 0f; camDur = 0f
    }

    private fun shift(by: Float) {
        ex += by; fromX += by; toX += by
        cam += by; fromCam += by; toCam += by
        slot = if (by < 0) 0 else 5
    }

    private fun camFor(sl: Int, t: Task): Float =
        sl * ROOM_W + if (t.cam >= 0f) t.cam else (t.x - 182f).coerceIn(0f, 116f)

    private fun setPose(l: Pose, r: Pose, snap: Boolean = false) {
        for (i in 0 until 6) poseFrom[i] = poseCur[i]
        poseTo[0] = l.x; poseTo[1] = l.y; poseTo[2] = l.deg
        poseTo[3] = r.x; poseTo[4] = r.y; poseTo[5] = r.deg
        poseAt = if (snap) -10f else time
        if (snap) for (i in 0 until 6) poseCur[i] = poseTo[i]
    }

    private fun setWalkPose() {
        for (i in 0 until 6) { poseFrom[i] = poseCur[i]; poseTo[i] = WalkPose[i] }
        poseAt = time
    }

    private fun snapMotion() {
        for (i in 0 until 8) motionFrom[i] = motionCur[i]
        motionAt = time
    }

    // ---------------------------------------------------------------------------------------------

    private fun compute(dt: Float) {
        // Position + camera (eased from the last departure; no teleports).
        val pm = if (mdur > 0f) MoveEase.transform(((time - departAt) / mdur).coerceIn(0f, 1f)) else 1f
        ex = lerp(fromX, toX, pm)
        dy = lerp(fromDy, toDy, pm)
        val pc = if (camDur > 0f) CamEase.transform(((time - departAt) / camDur).coerceIn(0f, 1f)) else 1f
        cam = lerp(fromCam, toCam, pc)

        // Body: crouch, lean, hop, land, task motion.
        val tr = SettleEase.transform(((time - trAt) / 0.3f).coerceIn(0f, 1f))
        when (phase) {
            Phase.Prep -> {
                squash = lerp(sqFrom, sqTo, tr); tilt = lerp(tiltFrom, 0f, tr); bodyY = lerp(bobFrom, 0f, tr)
            }
            Phase.Move -> {
                val hp = cyc(time - departAt, 0.6f)
                squash = lerp(sqFrom, 1f, tr) * Hop.at(hp, 1)
                tilt = lerp(tiltFrom, tiltTo, tr)
                bodyY = Hop.at(hp, 0)
            }
            Phase.Land -> {
                val lp = (phaseT / 0.5f).coerceIn(0f, 1f)
                if (moved) {
                    squash = LandMoved.at(lp, 1); tilt = 4f * dir * LandMoved.at(lp, 0); bodyY = 0f
                } else {
                    squash = LandHere.at(lp, 1); tilt = 0f; bodyY = LandHere.at(lp, 0)
                }
            }
            Phase.Work -> {
                squash = 1f; tilt = 0f; bodyY = 0f
                when (Tasks[room][task].body) {
                    BodyMotion.Nod -> { val k = pulse(cyc(phaseT, 0.6f)); tilt = -3f * k; bodyY = 2f * k; squash = 1f - 0.02f * k }
                    BodyMotion.Climb -> bodyY = -4f * pulse(cyc(phaseT, 1.2f))
                    BodyMotion.None -> Unit
                }
            }
        }

        // Gaze.
        var gx = 0f; var gy = 0f
        if (phase == Phase.Work) { val g = Tasks[room][task].gaze; gx = g.x / 7f; gy = g.y / 7f }
        else if (phase == Phase.Move) gx = dir * 5f / 7f
        val gk = min(1f, dt * 9f)
        gazeX += (gx - gazeX) * gk; gazeY += (gy - gazeY) * gk
        if (dt == 0f) { gazeX = gx; gazeY = gy }

        // Hands: spring between poses, task motion on top (cross-faded on every phase change).
        val hp = HandSpring.transform(((time - poseAt) / 0.55f).coerceIn(0f, 1f))
        for (i in 0 until 6) poseCur[i] = lerp(poseFrom[i], poseTo[i], hp)
        var lm: Hm = Hm.None; var rm: Hm = Hm.None
        var lt = 0f; var rt = 0f
        when (phase) {
            Phase.Work -> { val t = Tasks[room][task]; lm = t.al; rm = t.ar; lt = cyc(phaseT, t.alDur); rt = cyc(phaseT, t.arDur) }
            Phase.Move -> { lm = Hm.Swing; rm = Hm.SwingB; lt = cyc(time - departAt, 0.6f); rt = lt }
            else -> Unit
        }
        val mk = EaseInOut.transform(((time - motionAt) / 0.3f).coerceIn(0f, 1f))
        motion(lm, lt, 0, mk)
        motion(rm, rt, 4, mk)
        for (h in 0..1) {
            val o = h * 7
            hands[o] = poseCur[h * 3]; hands[o + 1] = poseCur[h * 3 + 1]; hands[o + 2] = poseCur[h * 3 + 2]
            hands[o + 3] = motionCur[h * 4]; hands[o + 4] = motionCur[h * 4 + 1]
            hands[o + 5] = motionCur[h * 4 + 2]; hands[o + 6] = motionCur[h * 4 + 3]
            // Centre of the mitten: pose * motion(about (0, 14)) applied to (0, 0).
            val mr = Math.toRadians(hands[o + 5].toDouble())
            val sc = hands[o + 6]
            val lx = hands[o + 3] + 14f * sc * sin(mr).toFloat()
            val ly = hands[o + 4] + 14f - 14f * sc * cos(mr).toFloat()
            val d = Math.toRadians(hands[o + 2].toDouble())
            handCenter[h * 2] = hands[o] + (lx * cos(d) - ly * sin(d)).toFloat()
            handCenter[h * 2 + 1] = hands[o + 1] + (lx * sin(d) + ly * cos(d)).toFloat()
        }

        feedForward(dt)

        // Props that fade or draw in.
        phones = approach(phones, if (room == 0 && (phase == Phase.Work || task > 0)) 1f else 0f, dt / 0.35f)
        lamp = approach(lamp, if (active(1, 2)) 1f else 0.3f, dt * 0.7f / 0.8f)
        trend = approach(trend, if (active(3, 0)) 1f else 0f, dt / 1.4f)
        words3 = approach(words3, if (active(0, 3)) 1f else 0f, dt / 0.45f)
        stream31 = approach(stream31, if (active(3, 1)) 1f else 0f, dt / 0.45f)
        propAlpha = approach(propAlpha, if (phase == Phase.Work) 1f else 0f, dt / 0.3f)
    }

    private fun feedForward(dt: Float) {
        rigRaw[0] = handCenter[0]; rigRaw[1] = handCenter[1]; rigRaw[2] = hands[2] + hands[5]
        rigRaw[3] = handCenter[2]; rigRaw[4] = handCenter[3]; rigRaw[5] = hands[9] + hands[12]
        rigRaw[6] = tilt; rigRaw[7] = squash
        if (!ffPrimed || dt <= 0f) {
            for (i in 0 until 8) { ffPrev[i] = rigRaw[i]; ffVel[i] = 0f; rigOut[i] = rigRaw[i] }
            ffPrimed = true
            return
        }
        val h = maxOf(dt, 1f / 240f)
        for (i in 0 until 8) {
            val k = if (i < 6) RIG_HAND_K else RIG_BODY_K
            val c = if (i < 6) RIG_HAND_C else RIG_BODY_C
            val v = (rigRaw[i] - ffPrev[i]) / h
            val a = (v - ffVel[i]) / h
            ffPrev[i] = rigRaw[i]; ffVel[i] = v
            // x'' = k (target - x) - c x'  =>  target = x + (x'' + c x') / k, one frame ahead.
            rigOut[i] = rigRaw[i] + v * h + (a + c * v) / k
        }
    }

    /** Raw (uncompensated) rig values, used while paused so nothing drifts. */
    fun rigRest(i: Int) = rigRaw[i]

    private fun motion(m: Hm, p: Float, o: Int, blend: Float) {
        val k = m.keys
        for (c in 0 until 4) {
            val target = if (k == null || p < 0f) (if (c == 3) 1f else 0f) else k.at(p, c)
            motionCur[o + c] = lerp(motionFrom[o + c], target, blend)
        }
    }

    private fun approach(v: Float, target: Float, step: Float): Float =
        if (v < target) min(target, v + step) else maxOf(target, v - step)
}
