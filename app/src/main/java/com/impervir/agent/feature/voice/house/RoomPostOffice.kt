package com.impervir.agent.feature.voice.house

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/** Room 5: the Post Office (stamps, paper planes to each platform, posting time, the mailbox, the send bell). */
internal object PostOffice : RoomArt {
    private val dashed = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)))
    private val envelope = svg("M44 70 H72 V88 H44 Z M44 70 L58 80 L72 70")
    private val bell = svg("M326 211 Q326 198 336 198 Q346 198 346 211 Z")
    private val bellShine = svg("M330 206 Q331 201 335 200")
    private val waveL = svg("M318 194 Q313 203 318 212 M312 190 Q305 203 312 216")
    private val waveR = svg("M354 194 Q359 203 354 212 M360 190 Q367 203 360 216")
    private val boxTop = svg("M402 183 V168 Q402 150 424 150 Q446 150 446 168 V183 Z")
    private val boxShine = svg("M408 166 Q410 156 420 154")
    private val plane = svg("M-9 3 L10 -1 L-5 -7 Z")
    private val planeFold = svg("M-9 3 L-1 -2 L10 -1")
    private val stand = Color(0xFFD9B9A2)

    private val launch = CubicBezierEasing(.3f, .5f, .5f, 1f)
    private val glide = CubicBezierEasing(.4f, 0f, .6f, 1f)
    // Paper planes: x, y, rotation, scale, opacity
    private val planeA = Keys(
        f(0f, .149f, .15f, .28f, .38f, .4f, 1f), Linear,
        f(231f, 231f, 231f, 268f, 296f, 300f, 300f), f(173f, 173f, 173f, 104f, 66f, 62f, 62f),
        f(-30f, -30f, -30f, -62f, -38f, -38f, -38f), f(1f, 1f, 1f, 1f, .7f, .5f, .5f), f(0f, 0f, 1f, 1f, 1f, 0f, 0f),
    ).eased(Linear, Linear, launch, glide, Linear, Linear)
    private val planeB = Keys(
        f(0f, .599f, .6f, .73f, .83f, .85f, 1f), Linear,
        f(231f, 231f, 231f, 300f, 345f, 350f, 350f), f(173f, 173f, 173f, 118f, 66f, 62f, 62f),
        f(-30f, -30f, -30f, -48f, -40f, -40f, -40f), f(1f, 1f, 1f, 1f, .7f, .5f, .5f), f(0f, 0f, 1f, 1f, 1f, 0f, 0f),
    ).eased(Linear, Linear, launch, glide, Linear, Linear)
    private val bellSwing = Keys(f(0f, .44f, .52f, .62f, .72f, .82f, 1f), EaseInOut, f(0f, 0f, 7f, -5f, 3f, 0f, 0f))
    private val flagLift = Keys(f(0f, .35f, .5f, .75f, 1f), EaseInOut, f(0f, 0f, -80f, -80f, 0f))

    override fun DrawScope.back(d: HouseDirector, a: HouseArt) {
        // Stamp poster
        rr(30f, 54f, 56f, 62f, 3f, P.Cream)
        drawRoundRect(P.InkLine, Offset(30f, 54f), Size(56f, 62f), CornerRadius(3f, 3f), style = dashed)
        rr(38f, 62f, 40f, 34f, 2f, a.accent)
        stroke(envelope, P.Cream, 2f)
        rr(38f, 102f, 24f, 4f, 2f, P.Ink, .2f)
        // The four platforms (official logos)
        for (i in 0..3) {
            val cx = 252f + 50f * i
            dot(cx, 58f, 16f, P.White)
            ring(cx, 58f, 16f, P.InkFaint, 1.5f)
            logo(a.logos[i], a.logoTints[i], cx - 9f, 49f, 18f)
        }
        // Delivery rings when a plane lands on LinkedIn, then Threads
        if (d.active(4, 1)) {
            val p = cyc(d.workT, TASK_SECONDS)
            arrival(p, .38f, 302f, Color(0xFF0A66C2))
            arrival(p, .83f, 352f, P.Ink)
        }
        // Send bell on its stand
        rr(321f, 214f, 30f, 4f, 2f, Color(0xFFE2C5B0))
        rect(332f, 218f, 8f, 25f, stand)
        rr(326f, 242f, 20f, 4f, 2f, stand)
        val ringing = d.active(4, 4)
        val bp = if (ringing) cyc(d.workT, 1.2f) else 0f
        rotate(if (ringing) bellSwing.at(bp, 0) else 0f, Offset(336f, 214f)) {
            fill(bell, P.Gold)
            stroke(bellShine, P.White, 1.5f, .7f)
            dot(336f, 196f, 2.5f, P.Dark)
            rr(323f, 210.5f, 26f, 3.5f, 1.5f, P.Dark)
        }
        if (ringing && bp >= .44f) {
            val q = EaseOut.transform((bp - .44f) / .56f)
            val o = if (bp < .5f) .9f * (bp - .44f) / .06f else .9f * (1f - (bp - .5f) / .5f)
            val s = lerp(.7f, 1.25f, q)
            scale(s, s, Offset(311.5f, 203f)) { stroke(waveL, P.Gold, 2f, o) }
            scale(s, s, Offset(360.5f, 203f)) { stroke(waveR, P.Gold, 2f, o) }
        }
        // Mailbox top + flag
        fill(boxTop, a.accent)
        stroke(boxShine, P.White, 2.5f, .4f)
        val fl = if (d.active(4, 3)) flagLift.at(cyc(d.workT, 2.4f), 0) else 0f
        rotate(fl, Offset(447f, 177f)) {
            rr(447f, 176f, 18f, 3f, 1.5f, P.Dark)
            rr(459f, 169f, 7f, 10f, 1.5f, P.Lilac)
        }
    }

    private fun DrawScope.arrival(p: Float, start: Float, cx: Float, c: Color) {
        if (p < start || p > start + .17f) return
        val q = EaseOut.transform((p - start) / .17f)
        ring(cx, 58f, 17f * lerp(.8f, 1.6f, q), c, 2f, 1f - q)
    }

    override fun DrawScope.front(d: HouseDirector, a: HouseArt) {
        if (!d.active(4, 1)) return
        val p = cyc(d.workT, TASK_SECONDS)
        flyer(planeA, p)
        flyer(planeB, p)
    }

    private fun DrawScope.flyer(k: Keys, p: Float) {
        val o = k.at(p, 4)
        if (o <= 0f) return
        translate(k.at(p, 0), k.at(p, 1)) {
            rotate(k.at(p, 2), Offset.Zero) {
                val s = k.at(p, 3)
                scale(s, s, Offset.Zero) {
                    fill(plane, P.White, o)
                    drawPath(plane, ink(.28f), o, st(.8f))
                    drawPath(planeFold, ink(.2f), o, st(.7f))
                }
            }
        }
    }

    override fun DrawScope.top(d: HouseDirector, a: HouseArt) {
        // Mailbox body (in front of the letters Ember drops)
        rr(402f, 182f, 44f, 64f, 4f, a.accent)
        rr(402f, 182f, 44f, 64f, 4f, Color(0x1F501400))
        rr(410f, 182f, 28f, 4f, 2f, Color(0xFF5A1E08))
        rr(414f, 196f, 20f, 3f, 1.5f, P.White, .35f)
    }
}
