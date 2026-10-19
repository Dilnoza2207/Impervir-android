package com.impervir.agent.feature.voice.house

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/** Room 3: the Writing Workshop (typewriter, the bin, the word jar, sharpening, the draft board). */
internal object WritingWorkshop : RoomArt {
    private val wood = Color(0xFFD8BA9C)
    private val woodDark = Color(0xFFC9A584)
    private val pencils = svg("M62 78 L60 66 M66 78 L67 64 M69 78 L72 68")
    private val pot = svg("M142 96 H158 L156 84 H144 Z")
    private val leaf = svg("M150 84 C144 76 145 68 150 62 C155 69 156 77 150 84 Z")
    private val typewriter = svg("M86 222 L90 205 Q92 202 96 202 H136 Q140 202 142 205 L146 222 Z")
    private val bin = svg("M200 218 L203 246 H221 L224 218 Q212 222 200 218 Z")
    private val ballCrease = svg("M-2.5 -1.5 L-.5 .5 L2 -2 M-1.5 2.5 L1 1.5")
    private val pop = CubicBezierEasing(0.3f, 1.6f, 0.5f, 1f)

    // Carriage return every task cycle; paper ball arc into the bin (x, y, rotation).
    private val carriage = Keys(f(0f, .84f, .91f, 1f), EaseInOut, f(0f, -10f, 1f, 0f), f(0f, -2f, -2f, 0f))
    private val ball = Keys(f(.4f, .55f, .7f), Linear, f(154f, 184f, 212f), f(166f, 136f, 228f), f(0f, 160f, 320f))
        .eased(CubicBezierEasing(.2f, .6f, .4f, 1f), CubicBezierEasing(.6f, 0f, .8f, .5f))

    override fun DrawScope.back(d: HouseDirector, a: HouseArt) {
        // Shelf with pencil cup, books and a plant
        rr(40f, 96f, 136f, 5f, 2f, wood)
        rr(58f, 78f, 14f, 18f, 3f, P.SoftLilac)
        stroke(pencils, P.Dark, 2f)
        rr(96f, 88f, 30f, 8f, 2f, P.Peach)
        rr(99f, 81f, 24f, 7f, 2f, P.Lilac)
        fill(pot, P.Clay); fill(leaf, P.Sage)
        rr(88f, 211f, 56f, 6f, 3f, Color(0xFFCDAE8F))
        // Side table for the word jar
        rr(226f, 214f, 36f, 5f, 2f, wood)
        rect(230f, 219f, 3f, 27f, woodDark)
        rect(255f, 219f, 3f, 27f, woodDark)
        // Draft board
        rr(372f, 96f, 80f, 80f, 7f, Color(0xFFC69C74))
        rr(377f, 101f, 70f, 70f, 4f, Color(0xFFDDB98F))
        rotate(4f, Offset(429f, 115f)) {
            rr(420f, 106f, 18f, 18f, 2f, P.Cream)
            line(423f, 113f, 435f, 113f, P.Ink, 1.5f, .18f); line(423f, 118f, 432f, 118f, P.Ink, 1.5f, .18f)
        }
        dot(429f, 108f, 2.2f, a.accent)
        rotate(-5f, Offset(435f, 141f)) { rr(426f, 133f, 18f, 16f, 2f, P.SoftLilac) }
        dot(435f, 135f, 2.2f, P.Dark)
        rotate(6f, Offset(423f, 159f)) { rr(414f, 151f, 18f, 16f, 2f, P.Peach) }
        dot(423f, 153f, 2.2f, a.accent)
        // Drafts Ember pins up (pop in one by one)
        pinned(d, a, 0, 379f, 134f)
        pinned(d, a, 1, 399f, 122f)
        pinned(d, a, 2, 384f, 153f)
    }

    private fun DrawScope.pinned(d: HouseDirector, a: HouseArt, i: Int, x: Float, y: Float) {
        if (d.pins <= i) return
        val t = ((d.time - d.pinAt[i]) / 0.5f).coerceIn(0f, 1f)
        val s = lerp(.3f, 1f, pop.transform(t))
        val o = (t / 0.5f).coerceAtMost(1f)
        val c = Offset(x + 7f, y + 7f)
        rotate(-10f, c) {
            scale(s, s, c) {
                rr(x, y, 14f, 14f, 2f, P.Cream, o)
                line(x + 3f, y + 7f, x + 11f, y + 7f, P.Ink, 1.2f, .2f * o)
                line(x + 3f, y + 10.5f, x + 9f, y + 10.5f, P.Ink, 1.2f, .2f * o)
                dot(x + 7f, y + 2.5f, 2f, a.accent, o)
            }
        }
    }

    override fun DrawScope.front(d: HouseDirector, a: HouseArt) {
        // Desk
        rr(34f, 222f, 162f, 8f, 3f, wood)
        rr(38f, 230f, 154f, 22f, 4f, woodDark)
        line(100f, 240f, 130f, 240f, P.Ink, 2f, .15f)
        // Typewriter carriage (returns at the end of every line while typing)
        val cp = if (d.active(2, 0)) cyc(d.workT, TASK_SECONDS) else 0f
        translate(carriage.at(cp, 0), carriage.at(cp, 1)) {
            card(98f, 184f, 36f, 15f, 1f, P.White, ink(.1f), 1f)
            line(103f, 188.5f, 128f, 188.5f, P.Ink, 1.2f, .28f)
            line(103f, 192.5f, 122f, 192.5f, P.Ink, 1.2f, .28f)
            rr(80f, 196f, 72f, 7f, 3.5f, P.Dark2)
            dot(78f, 199.5f, 4f, P.Dark); dot(154f, 199.5f, 4f, P.Dark)
        }
        fill(typewriter, P.Dark)
        line(92f, 205f, 140f, 205f, P.White, 1.2f, .12f)
        for (k in 0..5) dot(97f + 8f * k, 209f, 1.8f, P.Light, .75f)
        for (k in 0..4) dot(101f + 8f * k, 215f, 1.8f, P.Light, .75f)
        // Bin and the tossed draft
        oval(212f, 218f, 12f, 3f, Color(0xFF8F80C9))
        if (d.active(2, 1)) {
            val p = cyc(d.workT, 2.4f)
            if (p >= .4f && p <= .7f) {
                translate(ball.at(p, 0), ball.at(p, 1)) {
                    rotate(ball.at(p, 2), Offset.Zero) {
                        dot(0f, 0f, 4.5f, P.Cream)
                        ring(0f, 0f, 4.5f, P.Ink, .8f, .2f)
                        drawPath(ballCrease, ink(.22f), style = st(.7f))
                    }
                }
            }
        }
        fill(bin, P.SoftLilac)
        for (k in 0..2) line(206f + 6f * k, 226f + (k % 2), 206f + 6f * k, 240f + (k % 2), P.Ink, 1.5f, .07f)
    }

    override fun DrawScope.top(d: HouseDirector, a: HouseArt) {
        // Word jar on the side table
        rr(236f, 196f, 8f, 6f, 2f, P.Peach)
        rr(245f, 203f, 8f, 6f, 2f, P.SoftLilac)
        rr(237f, 205f, 7f, 5f, 2f, a.accent)
        card(232f, 186f, 24f, 28f, 6f, white(.42f), ink(.2f), 1.5f)
        line(236f, 191f, 236f, 205f, P.White, 2f, .85f)
        rr(230f, 182f, 28f, 5f, 2.5f, P.Lilac)
    }
}
