package com.impervir.agent.feature.voice.house

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/*
 * Things that move with Ember: the studio headphones and the props in its hands. They are drawn in Ember's 200 x 220
 * design box (half a world unit per design unit), using the exact hand transforms the director hands to Ember.
 * Props are split into the ones the mitten wraps (drawn under the hands) and the ones held in front (over the hands).
 */

/** Ember's body space in its design box (with the same hop, squash and lean the rig applies to the body). */
internal inline fun DrawScope.inEmber(d: HouseDirector, block: DrawScope.() -> Unit) {
    val sx = 1f / kotlin.math.sqrt(d.squash)
    translate(d.ex, EMBER_TOP + d.dy + d.bodyY + 99f) {
        rotate(d.tilt, Offset.Zero) {
            scale(sx, d.squash, Offset.Zero) {
                translate(-50f, -99f) { scale(.5f, .5f, Offset.Zero) { block() } }
            }
        }
    }
}

/** Ember's design box without the body's lean and squash (the rig's hands float free of them). */
internal inline fun DrawScope.inEmberBox(d: HouseDirector, block: DrawScope.() -> Unit) {
    translate(d.ex - 50f, EMBER_TOP + d.dy + d.bodyY) { scale(.5f, .5f, Offset.Zero) { block() } }
}

/** One hand's local space (0 = left, 1 = right): pose, then the task motion about (0, 14). */
internal inline fun DrawScope.inHand(d: HouseDirector, h: Int, block: DrawScope.() -> Unit) {
    val o = h * 7
    val m = d.hands
    translate(m[o], m[o + 1]) {
        rotate(m[o + 2], Offset.Zero) {
            translate(m[o + 3], 14f + m[o + 4]) {
                rotate(m[o + 5], Offset.Zero) {
                    scale(m[o + 6], m[o + 6], Offset.Zero) { translate(0f, -14f) { block() } }
                }
            }
        }
    }
}

/** Top of Ember's box in world units (feet on the floor at y ~ 246). */
internal const val EMBER_TOP = 147f

internal object HeldProps {
    // headphones
    private val band = svg("M24 98 C24 10 176 10 176 98")
    private val bandShine = svg("M36 66 C46 34 70 25 98 23")
    private val phonesDrop = Keys(f(0f, .15f, .45f, .5f, .58f, 1f), EaseInOut, f(-30f, -30f, 0f, 2f, 0f, 0f))

    // studio notes
    private val notesInk = svg("M18 12 Q23 7 27 12 T36 12 T45 11 T54 12")
    private val inkMeasure = PathMeasure().apply { setPath(notesInk, false) }
    private val inkLen = inkMeasure.length
    private val inkPart = Path()
    private val pencilTip = svg("M-3.5 15 L0 26 L3.5 15 Z")
    private val pencilLead = svg("M-1.3 22 L0 26 L1.3 22 Z")
    private val pencil2Tip = svg("M-3.5 14 L0 26 L3.5 14 Z")
    private val pencil2Lead = svg("M-1.2 22 L0 26 L1.2 22 Z")
    // library
    private val pageL = svg("M-7 -30 Q16 -34 40 -28 V6 Q16 0 -7 4 Z")
    private val pageR = svg("M87 -30 Q64 -34 40 -28 V6 Q64 0 87 4 Z")
    private val pageLines = svg("M2 -20 Q18 -23 33 -19 M2 -12 Q18 -15 33 -11 M47 -19 Q62 -23 78 -20 M47 -11 Q62 -15 78 -12")
    private val pageFlip = svg("M40 -28 Q62 -33 85 -29 V4 Q62 0 40 6 Z")
    private val flipKeys = Keys(f(0f, .1f, .6f, .7f, 1f), EaseInOut, f(1f, .9f, -1f, -1f, -1f), f(0f, 1f, 1f, 0f, 0f))
    // workshop
    private val ballCrease = svg("M-5 -31 L-1 -27 L4 -32 M-3 -23 L2 -25")
    private val shaving1 = svg("M25 8 q3 3 0 6 q-3 3 0 6")
    private val shaving2 = svg("M30 8 q-3 3 0 6 q3 3 0 6")
    // lab
    private val strings = svg("M-44 31 L-54 56 M-44 31 L-34 56 M44 31 L34 56 M44 31 L54 56")
    private val panL = svg("M-58 56 Q-44 66 -30 56 Z")
    private val panR = svg("M30 56 Q44 66 58 56 Z")
    private val beamKeys = Keys(f(0f, .3f, .7f, 1f), EaseInOut, f(0f, 8f, -5f, 0f))
    // post office
    private val flap = svg("M-6 -38 L24 -16 L54 -38")
    private val ticks = svg("M8 -49 V-46 M8 -22 V-19 M-7 -34 H-4 M20 -34 H23")
    private val plane = svg("M-18 -24 L20 -32 L-10 -44 Z")
    private val planeFold = svg("M-18 -24 L-2 -34 L20 -32")
    private val letterFlap = svg("M-10 6 L0 16 L10 6")
    private val stampKeys = Keys(f(0f, .44f, .5f, .88f, 1f), EaseOut, f(0f, 0f, 1f, 1f, 0f), f(1.3f, 1.3f, 1f, 1f, 1f))
    private val heldPlane = Keys(f(0f, .149f, .15f, .4f, .46f, .599f, .6f, .92f, 1f), Linear, f(1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f))
    private val drops = Keys(f(0f, .52f, .62f, .85f, 1f), EaseInOut, f(1f, 1f, 0f, 0f, 1f))

    private val bulb = Color(0xFFFFE7A0)
    private val bulbLine = Color(0xFFE9B949)
    private val glass = Color(0xFF9E94BD)
    private val wood = Color(0xFFF2D3B8)
    private val lead = Color(0xFF1B0F0B)

    /** Headphones on Ember's head in the Listening Studio (between Ember and the furniture). */
    fun DrawScope.headphones(d: HouseDirector, a: HouseArt) {
        val o = d.phones
        if (o <= 0f) return
        val drop = if (d.room == 0 && d.task == 0 && d.phase == Phase.Work) phonesDrop.at(cyc(d.workT, TASK_SECONDS), 0) else 0f
        inEmber(d) {
            translate(0f, drop) {
                stroke(band, P.Dark, 8f, o)
                stroke(bandShine, P.White, 2.5f, .22f * o)
                rr(11f, 82f, 24f, 36f, 11f, P.Dark, o); rr(165f, 82f, 24f, 36f, 11f, P.Dark, o)
                rr(16f, 89f, 9f, 22f, 4.5f, a.accent, o); rr(175f, 89f, 9f, 22f, 4.5f, a.accent, o)
            }
        }
    }

    /** Props the mitten wraps around (drawn under Ember's hands). */
    fun DrawScope.under(d: HouseDirector, a: HouseArt) {
        val o = d.propAlpha
        if (o <= 0f) return
        val t = d.workT
        inEmberBox(d) {
            when (d.propRoom * 10 + d.propTask) {
                3 -> inHand(d, 1) { jar(a, o) }
                4 -> inHand(d, 0) { notepad(t, o) }
                10 -> inHand(d, 1) {
                    rr(-9f, -60f, 18f, 54f, 2f, a.accent, o)
                    rect(-9f, -51f, 18f, 4f, P.White, .55f * o); rect(-9f, -22f, 18f, 4f, P.White, .55f * o)
                }
                12 -> inHand(d, 0) { book(a, t, o) }
                13 -> inHand(d, 1) {
                    oval(0f, 6f, 8f, 7f, P.Dark, o); rect(-3.5f, 12f, 7f, 12f, P.Dark, o)
                    rr(-11f, 24f, 22f, 7f, 2f, P.Dark3, o); rr(-10f, 31f, 20f, 3f, 1f, a.accent, o)
                }
                14 -> inHand(d, 0) {
                    val v = drops.at(cyc(t, 2.4f), 0) * o
                    rect(-10f, -1f, 20f, 8f, P.Cream, v); rr(-13f, -2f, 12f, 6f, 2f, P.Lilac, v)
                    rr(-13f, 2f, 26f, 40f, 2.5f, P.Lilac, v); line(-8f, 32f, 8f, 32f, P.White, 2f, .6f * v)
                }
                21 -> inHand(d, 1) {
                    val p = cyc(t, 2.4f)
                    val s = when { p < .4f -> 1f; p < .84f -> 0f; else -> lerp(.5f, 1f, (p - .84f) / .16f) }
                    if (s > 0f) scale(s, s, Offset(0f, -28f)) {
                        dot(0f, -28f, 9f, P.Cream, o * s); ring(0f, -28f, 9f, P.Ink, 1.5f, .2f * o * s)
                        stroke(ballCrease, P.Ink, 1.3f, .22f * o * s)
                    }
                }
                22 -> inHand(d, 0) {
                    card(-17f, -42f, 34f, 22f, 4f, P.Cream, ink(.16f), 1.5f, o)
                    rr(-11f, -34f, 22f, 5f, 2.5f, a.accent, o)
                }
                31 -> inHand(d, 1) {
                    card(-12f, -34f, 24f, 30f, 3f, white(.78f), glass, 2f, o)
                    rr(-11f, -33f, 22f, 11f, 2f, P.Lilac, .75f * o)
                    line(12f, -34f, 16f, -37f, glass, 2f, o)
                }
                32 -> inHand(d, 1) { rr(-2f, -72f, 4f, 60f, 2f, P.Dark, o); dot(0f, -71f, 3.5f, a.accent, o) }
                33 -> {
                    inHand(d, 0) {
                        dot(0f, -32f, 9f, bulb, o); ring(0f, -32f, 9f, bulbLine, 1.5f, o)
                        rr(-4f, -24f, 8f, 6f, 1.5f, P.Muted, o)
                    }
                    inHand(d, 1) { balance(t, o) }
                }
                40 -> inHand(d, 0) { envelope(a, t, o) }
                41 -> inHand(d, 1) {
                    val v = heldPlane.at(cyc(t, TASK_SECONDS), 0) * o
                    if (v > 0f) {
                        fill(plane, P.White, v)
                        drawPath(plane, ink(.28f), v, st(1.6f))
                        drawPath(planeFold, ink(.2f), v, st(1.4f))
                    }
                }
                42 -> inHand(d, 0) { clock(a, t, o) }
                43 -> inHand(d, 1) {
                    val v = drops.at(cyc(t, 2.4f), 0) * o
                    card(-12f, 4f, 24f, 34f, 2.5f, P.Cream, ink(.18f), 1.5f, v)
                    drawPath(letterFlap, ink(.18f), v, st(1.5f))
                    rr(2f, 26f, 7f, 8f, 1f, a.accent, v)
                }
            }
        }
    }

    /** Props held in front of the mitten (pencils, the sharpener, a push pin). */
    fun DrawScope.over(d: HouseDirector, a: HouseArt) {
        val o = d.propAlpha
        if (o <= 0f) return
        val t = d.workT
        inEmberBox(d) {
            when (d.propRoom * 10 + d.propTask) {
                4 -> inHand(d, 1) {
                    rr(-3.5f, -30f, 7f, 46f, 3.5f, P.Dark, o); rr(-3.5f, -30f, 7f, 9f, 3.5f, P.Lilac, o)
                    fill(pencilTip, wood, o); fill(pencilLead, lead, o)
                }
                23 -> {
                    inHand(d, 0) {
                        rr(8f, -8f, 24f, 16f, 4f, P.Lilac, o); rr(10f, -6f, 9f, 12f, 2f, P.SoftLilac, o)
                        dot(28f, 0f, 3.2f, P.Dark, o)
                        shaving(shaving1, Color(0xFFE7C9A8), 2.2f, cyc(t, 1.2f), o)
                        shaving(shaving2, P.Gold, 2f, cyc(t, 1.2f, .6f), o)
                    }
                    inHand(d, 1) {
                        rr(-3.5f, -30f, 7f, 44f, 1.5f, P.Gold, o); rr(-3.5f, -30f, 7f, 6f, 2f, Color(0xFFFF8FA3), o)
                        fill(pencil2Tip, wood, o); fill(pencil2Lead, P.Dark, o)
                    }
                }
                24 -> inHand(d, 1) { line(0f, -19f, 0f, -14f, P.Muted, 1.6f, o); dot(0f, -23f, 4.5f, a.accent, o) }
            }
        }
    }

    private fun DrawScope.jar(a: HouseArt, o: Float) {
        rr(-20f, -62f, 40f, 48f, 10f, P.White, .55f * o)
        rrLine(-20f, -62f, 40f, 48f, 10f, P.Ink, 2.5f, .22f * o)
        rr(-12f, -30f, 13f, 8f, 3f, P.Peach, o); rr(2f, -27f, 12f, 8f, 3f, a.accent, o); rr(-8f, -40f, 12f, 8f, 3f, P.SoftLilac, o)
        line(-13f, -54f, -13f, -36f, P.White, 3f, .9f * o)
        rr(-16f, -68f, 32f, 8f, 3f, P.Lilac, o)
    }

    private fun DrawScope.notepad(t: Float, o: Float) {
        card(0f, -30f, 60f, 48f, 6f, P.Cream, ink(.16f), 2f, o)
        line(20f, -20f, 52f, -20f, P.Ink, 2.5f, .12f * o); line(20f, -10f, 52f, -10f, P.Ink, 2.5f, .12f * o)
        line(20f, 0f, 44f, 0f, P.Ink, 2.5f, .12f * o)
        val p = (cyc(t, 2.4f) / .7f).coerceAtMost(1f)
        if (p > 0f) {
            inkPart.reset()
            inkMeasure.getSegment(0f, inkLen * p, inkPart, true)
            stroke(inkPart, P.Lilac, 2.5f, o)
        }
    }

    private fun DrawScope.book(a: HouseArt, t: Float, o: Float) {
        rr(-12f, -34f, 104f, 44f, 5f, a.accent, o)
        fill(pageL, P.Cream, o); fill(pageR, Color(0xFFFFF6EA), o)
        stroke(pageLines, P.Ink, 2f, .14f * o)
        line(40f, -28f, 40f, 6f, P.Ink, 1.5f, .15f * o)
        val p = cyc(t, 2.4f)
        val v = flipKeys.at(p, 1) * o
        if (v > 0f) {
            val sx = flipKeys.at(p, 0)
            scale(sx, 1f, Offset(40f, 0f)) {
                fill(pageFlip, P.White, v)
                drawPath(pageFlip, ink(.08f), v, st(1f))
            }
        }
    }

    private fun DrawScope.balance(t: Float, o: Float) {
        line(0f, -6f, 0f, 28f, P.Dark, 2.5f, o)
        rotate(beamKeys.at(cyc(t, 2.4f), 0), Offset(0f, 26.5f)) {
            dot(0f, 30f, 3.5f, P.Dark, o)
            rr(-46f, 29f, 92f, 4f, 2f, P.Dark, o)
            stroke(strings, P.Dark, 1.4f, .6f * o)
            dot(-44f, 49f, 6f, bulb, o); ring(-44f, 49f, 6f, bulbLine, 1.2f, o)
            dot(44f, 47f, 8f, bulb, o); ring(44f, 47f, 8f, bulbLine, 1.2f, o)
            fill(panL, P.Muted, o); fill(panR, P.Muted, o)
        }
    }

    private fun DrawScope.envelope(a: HouseArt, t: Float, o: Float) {
        card(-8f, -40f, 64f, 40f, 4f, P.Cream, ink(.16f), 2f, o)
        drawPath(flap, ink(.18f), o, st(2f))
        val p = cyc(t, 1.2f)
        val v = stampKeys.at(p, 0) * o
        if (v > 0f) {
            val s = stampKeys.at(p, 1)
            scale(s, s, Offset(46f, -30f)) {
                rr(40f, -36f, 12f, 12f, 1.5f, a.accent, v)
                rrLine(42.5f, -33.5f, 7f, 7f, 1f, P.Cream, 1.2f, v)
            }
        }
    }

    private fun DrawScope.clock(a: HouseArt, t: Float, o: Float) {
        dot(-6f, -56f, 6f, P.Lilac, o); dot(22f, -56f, 6f, P.Lilac, o)
        dot(8f, -34f, 22f, P.Lilac, o)
        rr(29f, -37f, 6f, 6f, 1.5f, Color(0xFF6B5BE0), o)
        dot(8f, -34f, 17f, P.Cream, o)
        stroke(ticks, P.Ink, 2f, .3f * o)
        val c = Offset(8f, -34f)
        rotate(360f * cyc(t, TASK_SECONDS), c) { line(8f, -34f, 13f, -40f, P.Dark, 3f, o) }
        rotate(360f * cyc(t, 1.2f), c) { line(8f, -34f, 8f, -46f, P.Dark, 2.2f, o) }
        dot(8f, -34f, 2.2f, a.accent, o)
    }

    private fun DrawScope.shaving(p: Path, c: Color, w: Float, k: Float, o: Float) {
        if (k < 0f) return
        val e = EaseIn.transform(k)
        val v = (if (k < .2f) k / .2f else 1f - (k - .2f) / .8f) * o
        translate(-4f * e, 22f * e) { rotate(90f * e, Offset.Zero) { drawPath(p, c, v, st(w)) } }
    }
}
