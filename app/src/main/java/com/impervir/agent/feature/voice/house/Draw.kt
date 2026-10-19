package com.impervir.agent.feature.voice.house

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.floor

/*
 * Tiny drawing toolkit for Ember's house. Everything is in "world units" (one room = 480 x 238 wall, stage = 364 x 300),
 * the same coordinates as the iOS design, so props can be copied 1:1. All helpers are allocation-free per frame:
 * paths are parsed once, strokes are cached, Offset/Size/CornerRadius/Color are value classes.
 */

// ---- shared palette (flat, soft, premium) ----
internal object P {
    val Dark = Color(0xFF2A2730)
    val Dark2 = Color(0xFF3A3640)
    val Dark3 = Color(0xFF46424D)
    val Ink = Color(0xFF141318)
    val InkFaint = Color(0x12141318)     // rgba(20,19,24,.07)
    val InkLine = Color(0x24141318)      // ~.14
    val Cream = Color(0xFFFFFDF8)
    val White = Color.White
    val Lilac = Color(0xFF8C7BFF)
    val SoftLilac = Color(0xFFC9BEFF)
    val Peach = Color(0xFFFFB59A)
    val Gold = Color(0xFFFFC94D)
    val Sage = Color(0xFFA9B79E)
    val Clay = Color(0xFFE6B49B)
    val Muted = Color(0xFF9A96A0)
    val Light = Color(0xFFEDEAF0)
    val OnAccent = Color(0xFF160A05)
}

internal fun ink(alpha: Float) = Color(0.078f, 0.075f, 0.094f, alpha)
internal fun white(alpha: Float) = Color(1f, 1f, 1f, alpha)

// ---- path + stroke caches ----
internal fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

private val roundStrokes = arrayOfNulls<Stroke>(120)
private val buttStrokes = arrayOfNulls<Stroke>(120)

/** Round-capped, round-joined stroke of width [w] (cached by tenths). */
internal fun st(w: Float): Stroke {
    val i = (w * 10f + 0.5f).toInt().coerceIn(0, 119)
    return roundStrokes[i] ?: Stroke(i / 10f, cap = StrokeCap.Round, join = StrokeJoin.Round).also { roundStrokes[i] = it }
}

/** Butt-capped stroke (outlines of boxes). */
internal fun sb(w: Float): Stroke {
    val i = (w * 10f + 0.5f).toInt().coerceIn(0, 119)
    return buttStrokes[i] ?: Stroke(i / 10f).also { buttStrokes[i] = it }
}

// ---- primitives ----
internal fun DrawScope.rect(x: Float, y: Float, w: Float, h: Float, c: Color, alpha: Float = 1f) =
    drawRect(c, Offset(x, y), Size(w, h), alpha = alpha)

internal fun DrawScope.rr(x: Float, y: Float, w: Float, h: Float, r: Float, c: Color, alpha: Float = 1f) =
    drawRoundRect(c, Offset(x, y), Size(w, h), CornerRadius(r, r), alpha = alpha)

internal fun DrawScope.rrLine(x: Float, y: Float, w: Float, h: Float, r: Float, c: Color, width: Float, alpha: Float = 1f) =
    drawRoundRect(c, Offset(x, y), Size(w, h), CornerRadius(r, r), alpha = alpha, style = sb(width))

/** Filled round rect with an outline, like an SVG rect with fill + stroke. */
internal fun DrawScope.card(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Color, line: Color, width: Float, alpha: Float = 1f) {
    rr(x, y, w, h, r, fill, alpha)
    rrLine(x, y, w, h, r, line, width, alpha)
}

internal fun DrawScope.dot(cx: Float, cy: Float, r: Float, c: Color, alpha: Float = 1f) =
    drawCircle(c, r, Offset(cx, cy), alpha = alpha)

internal fun DrawScope.ring(cx: Float, cy: Float, r: Float, c: Color, width: Float, alpha: Float = 1f) =
    drawCircle(c, r, Offset(cx, cy), alpha = alpha, style = sb(width))

internal fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, c: Color, alpha: Float = 1f) =
    drawOval(c, Offset(cx - rx, cy - ry), Size(rx * 2f, ry * 2f), alpha = alpha)

internal fun DrawScope.ovalLine(cx: Float, cy: Float, rx: Float, ry: Float, c: Color, width: Float, alpha: Float = 1f) =
    drawOval(c, Offset(cx - rx, cy - ry), Size(rx * 2f, ry * 2f), alpha = alpha, style = sb(width))

internal fun DrawScope.line(x1: Float, y1: Float, x2: Float, y2: Float, c: Color, w: Float, alpha: Float = 1f) =
    drawLine(c, Offset(x1, y1), Offset(x2, y2), w, StrokeCap.Round, alpha = alpha)

internal fun DrawScope.fill(p: Path, c: Color, alpha: Float = 1f) = drawPath(p, c, alpha)

internal fun DrawScope.stroke(p: Path, c: Color, w: Float, alpha: Float = 1f) = drawPath(p, c, alpha, st(w))

// ---- timing ----
internal val EaseInOut: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
internal val EaseOut: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
internal val EaseIn: Easing = CubicBezierEasing(0.42f, 0f, 1f, 1f)
internal val Ease: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
internal val Linear: Easing = Easing { it }

internal fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
internal fun seg(p: Float, a: Float, b: Float) = ((p - a) / (b - a)).coerceIn(0f, 1f)

/** Progress 0..1 inside a looping animation of [period] seconds that started [delay] seconds after [t]=0; -1 before it starts. */
internal fun cyc(t: Float, period: Float, delay: Float = 0f): Float {
    val x = (t - delay) / period
    return if (x < 0f) -1f else x - floor(x)
}

/** A 0 -> 1 -> 0 ease-in-out pulse over one cycle (most of the design's "breathing" props). */
internal fun pulse(p: Float): Float = if (p < 0.5f) EaseInOut.transform(p * 2f) else EaseInOut.transform((1f - p) * 2f)

/**
 * CSS-like keyframes: [times] in 0..1, one value array per channel, an easing per segment
 * (CSS applies the timing function between each pair of keyframes).
 */
internal class Keys(private val times: FloatArray, private val easing: Easing, private vararg val ch: FloatArray) {
    private var segEasing: Array<Easing>? = null

    fun eased(vararg perSegment: Easing): Keys = apply { segEasing = arrayOf(*perSegment) }

    fun at(p: Float, c: Int): Float {
        val v = ch[c]
        if (p <= times[0]) return v[0]
        val last = times.size - 1
        if (p >= times[last]) return v[last]
        var i = 0
        while (i < last && p > times[i + 1]) i++
        val span = times[i + 1] - times[i]
        if (span <= 0f) return v[i + 1]
        val e = segEasing?.getOrNull(i) ?: easing
        return lerp(v[i], v[i + 1], e.transform((p - times[i]) / span))
    }
}

internal fun f(vararg v: Float) = v
