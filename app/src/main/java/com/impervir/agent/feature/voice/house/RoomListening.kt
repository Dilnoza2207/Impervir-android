package com.impervir.agent.feature.voice.house

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.geometry.Offset

/** Room 1: the Listening Studio (headphones, mic level, faders, the word jar, quick notes). */
internal object ListeningStudio : RoomArt {
    private val wave = svg("M48 75 H55 L59 65 L64 87 L69 58 L74 92 L79 67 L83 81 L87 75 H92")
    private val pot = svg("M18 214 H40 L37 238 H21 Z")
    private val leaves = svg("M29 214 C21 202 19 190 24 180 C31 191 32 203 29 214 Z M29 214 C34 204 41 199 46 198 C44 207 37 213 29 214 Z")
    private val micStand = svg("M186 216 Q178 206 186 200")

    private val panel = Color(0xFFF0CDBB)
    private val sky = Color(0xFFD9D0FA)
    private val frame = Color(0xFFFCF1E8)
    private val sill = Color(0xFFEFD2BF)
    private val meter = Color(0xFF1E1C22)
    private val deskTop = Color(0xFF46424D)
    private val deskBody = Color(0xFF2F2C35)
    private val knob = Color(0xFF5F5A68)
    private val slotC = Color(0xFF1A181E)
    private val words2 = Color(0xFFC9BEFF)

    // Words float from the window down into the jar: transform keys [x, y, scale] and opacity keys.
    private val wordMove = Keys(f(0f, .5f, .85f, .95f, 1f), EaseInOut, f(446f, 442f, 424f, 424f, 424f), f(92f, 146f, 184f, 190f, 190f), f(.8f, .9f, .35f, .2f, .2f))
    private val wordFade = Keys(f(0f, .15f, .85f, .95f, 1f), EaseInOut, f(0f, 1f, 1f, 0f, 0f))
    private val ledPeriods = f(.4f, .6f, .8f, .6f, .4f)

    override fun DrawScope.back(d: HouseDirector, a: HouseArt) {
        // Waveform print
        card(40f, 50f, 60f, 50f, 8f, P.Cream, P.InkFaint, 2f)
        stroke(wave, a.accent, 2.4f)
        // Plant
        fill(pot, P.Clay); fill(leaves, P.Sage)
        // Acoustic panel
        rr(196f, 64f, 140f, 86f, 16f, panel)
        for (i in 0..4) { dot(216f + 25f * i, 88f, 9f, P.White, .3f); dot(216f + 25f * i, 125f, 9f, P.White, .3f) }
        // Round window
        rr(400f, 56f, 64f, 96f, 32f, sky)
        rrLine(400f, 56f, 64f, 96f, 32f, frame, 6f)
        line(432f, 59f, 432f, 149f, frame, 3f)
        oval(418f, 96f, 10f, 5f, P.White, .75f)
        rr(394f, 150f, 76f, 6f, 3f, sill)
        // Words drifting into the jar while Ember catches them
        if (d.words3 > 0f) {
            word(d, a.deadlines, P.White, 46f, 0f, d.words3)
            word(d, a.elevenDays, a.accent, 52f, .8f, d.words3)
            word(d, a.almostCut, words2, 50f, 1.6f, d.words3)
        }
    }

    private fun DrawScope.word(d: HouseDirector, t: TextLayoutResult, bg: Color, w: Float, delay: Float, alpha: Float) {
        val p = cyc(d.workT, 2.4f, delay)
        if (p < 0f) return
        val o = wordFade.at(p, 0) * alpha
        if (o <= 0.01f) return
        translate(wordMove.at(p, 0), wordMove.at(p, 1)) {
            val s = wordMove.at(p, 2)
            scale(s, s, Offset.Zero) {
                rr(-w / 2f, -8f, w, 16f, 8f, bg, o)
                label(t, 0f, 0f, o)
            }
        }
    }

    override fun DrawScope.front(d: HouseDirector, a: HouseArt) {
        // Mic
        stroke(micStand, P.Dark2, 3f)
        rr(180f, 184f, 12f, 18f, 6f, P.Dark)
        line(182.5f, 190f, 189.5f, 190f, P.White, 1.2f, .28f)
        line(182.5f, 194f, 189.5f, 194f, P.White, 1.2f, .28f)
        // Level meter
        rr(304f, 192f, 30f, 24f, 4f, meter)
        val live = d.active(0, 1) || d.active(0, 2)
        for (i in 0..4) {
            val sy = if (live) .3f + .7f * pulse(cyc(d.workT, ledPeriods[i])) else .3f
            val h = 16f * sy
            rr(309f + 5f * i, 212f - h, 3f, h, 1f, a.accent)
        }
        // Mixing desk
        rr(166f, 216f, 176f, 10f, 3f, deskTop)
        line(168f, 216.6f, 340f, 216.6f, P.White, 1.2f, .14f)
        rr(170f, 226f, 168f, 26f, 5f, deskBody)
        val turn = if (d.active(0, 1)) -50f * pulse(cyc(d.workT, 1.2f)) else 0f
        dot(214f, 220f, 4.5f, knob)
        rotate(turn, Offset(214f, 220f)) { line(214f, 220f, 214f, 216.5f, P.White, 1.5f) }
        dot(227f, 220f, 4.5f, knob)
        line(227f, 220f, 229.5f, 217.5f, P.White, 1.5f)
        for (i in 0..3) rr(239f + 16f * i, 217f, 2.5f, 8f, 1f, slotC)
        val fad = if (d.active(0, 2)) pulse(cyc(d.workT, 1.2f)) else 0f
        rr(234.25f, 219f - 3f * fad, 12f, 4f, 1.5f, P.Light)
        rr(250.25f, 217.5f, 12f, 4f, 1.5f, P.Light)
        rr(266.25f, 221f, 12f, 4f, 1.5f, P.Light)
        rr(282.25f, 219f + 2.5f * fad, 12f, 4f, 1.5f, P.Light)
    }
}
