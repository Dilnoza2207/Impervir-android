package com.impervir.agent.feature.voice.house

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/** Room 4: the Insight Lab (trend stars, a test potion, what sold, weighing ideas, the $29 sale). */
internal object InsightLab : RoomArt {
    private val shelf = Color(0xFFCFC5E4)
    private val night = Color(0xFF25213B)
    private val trendC = Color(0xFFB9A8FF)
    private val glass = Color(0xFF9E94BD)
    private val flask = svg("M56 60 H70 L68 48 Q63 40 58 48 Z")
    private val star = svg("M262 56 L264.5 61.5 L270 64 L264.5 66.5 L262 72 L259.5 66.5 L254 64 L259.5 61.5 Z")
    private val liquid = svg("M251.5 216 A13 13 0 0 0 276.5 216 Z")
    private val tube = svg("M6 -4.5 L66 -7.5 L66 7.5 L6 4.5 Z")
    private val tripod = svg("M134 176 L118 248 M134 176 L150 248 M134 176 L136 250")
    private val register = svg("M410 222 V204 Q410 194 420 194 H446 Q456 194 456 204 V222 Z")
    private val trendPath = svg("M194 112 L210 100 L224 104 L240 84 L258 68")
    private val measure = PathMeasure().apply { setPath(trendPath, false) }
    private val trendLen = measure.length
    private val trendPart = Path()
    private val drawIn = CubicBezierEasing(.3f, .7f, .3f, 1f)

    // Stars: cx, cy, r, period
    private val stars = f(204f, 64f, 1.6f, 2.4f, 214f, 106f, 1.3f, 1.6f, 240f, 58f, 1.4f, 3.2f, 196f, 88f, 1.1f, 1.6f, 252f, 112f, 1.2f, 2.4f, 226f, 76f, 1f, 3.2f)
    // Coin from the till: y, scaleX, opacity
    private val coin = Keys(f(0f, .44f, .46f, .62f, .78f, .84f, 1f), EaseOut, f(220f, 220f, 218f, 190f, 214f, 224f, 224f), f(1f, 1f, 1f, .15f, 1f, 1f, 1f), f(0f, 0f, 1f, 1f, 1f, 0f, 0f))
    private val display = Keys(f(0f, .44f, .5f, .88f, 1f), EaseOut, f(0f, 0f, 1f, 1f, 0f))
    private val drawer = Keys(f(0f, .4f, .5f, .8f, 1f), EaseInOut, f(0f, 0f, 5f, 5f, 0f))

    override fun DrawScope.back(d: HouseDirector, a: HouseArt) {
        // Shelf of bottles
        rr(24f, 60f, 96f, 4f, 2f, shelf)
        rr(34f, 42f, 10f, 18f, 3f, P.SoftLilac)
        fill(flask, P.Peach)
        rr(84f, 46f, 12f, 14f, 3f, P.Lilac)
        rr(102f, 50f, 8f, 10f, 2f, P.Cream)
        // Night window: twinkling stars, the trend line drawing itself
        dot(230f, 86f, 48f, night)
        var i = 0
        while (i < stars.size) {
            val k = pulse(cyc(d.time, stars[i + 3]))
            dot(stars[i], stars[i + 1], stars[i + 2] * lerp(.7f, 1f, k), P.White, lerp(.35f, 1f, k))
            i += 4
        }
        if (d.trend > 0f) {
            trendPart.reset()
            measure.getSegment(0f, trendLen * drawIn.transform(d.trend), trendPart, true)
            stroke(trendPart, trendC, 2.5f)
        }
        val sp = if (d.active(3, 0)) pulse(cyc(d.workT, 1.2f)) else 0f
        val ss = 1f + .35f * sp
        rotate(45f * sp, Offset(262f, 64f)) { scale(ss, ss, Offset(262f, 64f)) { fill(star, P.Gold) } }
        ring(230f, 86f, 48f, Color(0xFFF6F3FC), 7f)
        // Sales chart board
        card(318f, 84f, 100f, 90f, 7f, Color(0xFFFBFAF7), Color(0xFFD6CFE6), 3f)
        rr(328f, 94f, 40f, 4f, 2f, P.Ink, .3f)
        val bar = if (d.active(3, 2)) 1f + .07f * pulse(cyc(d.workT, 1.2f)) else 1f
        rr(326f, 164f - 52f * bar, 14f, 52f * bar, 2f, a.accent)
        rr(344f, 132f, 12f, 32f, 2f, P.Lilac)
        rr(362f, 142f, 12f, 22f, 2f, P.SoftLilac)
        rr(380f, 152f, 12f, 12f, 2f, Color(0xFFDCD6EA))
        line(322f, 164.5f, 410f, 164.5f, P.Ink, 1.5f, .25f)
        // Lab table + test potion
        rr(232f, 227f, 68f, 6f, 2f, Color(0xFFCFC6E0))
        rect(236f, 233f, 3f, 13f, Color(0xFFBDB3D3))
        rect(293f, 233f, 3f, 13f, Color(0xFFBDB3D3))
        if (d.stream31 > 0f) {
            val o = d.stream31
            val ph = cyc(d.workT, .4f) * 9f
            var s = -9f + ph
            while (s < 23f) {
                val a0 = s.coerceAtLeast(0f); val a1 = (s + 4f).coerceAtMost(23f)
                if (a1 > a0) line(264f, 189f + a0, 264f, 189f + a1, P.Lilac, 2.4f, o)
                s += 9f
            }
            bubble(261f, 220f, 1.6f, cyc(d.workT, 1.2f), o)
            bubble(267f, 222f, 1.2f, cyc(d.workT, 1.6f), o)
        }
        card(259f, 194f, 10f, 12f, 2f, white(.55f), glass, 1.5f)
        dot(264f, 214f, 13f, P.White, .55f)
        ring(264f, 214f, 13f, glass, 1.5f)
        fill(liquid, P.Lilac, .75f)
    }

    private fun DrawScope.bubble(x: Float, y: Float, r: Float, p: Float, alpha: Float) {
        val e = EaseIn.transform(p)
        val o = if (p < .3f) p / .3f else 1f - (p - .3f) / .7f
        dot(x, y - 12f * e, r, P.White, o * alpha)
    }

    override fun DrawScope.front(d: HouseDirector, a: HouseArt) {
        // Telescope on a tripod
        translate(112f, 196f) {
            rotate(-43f, Offset.Zero) {
                fill(tube, P.Dark)
                rr(-4f, -3.5f, 11f, 7f, 2f, P.Dark3)
                rr(62f, -8.5f, 8f, 17f, 3f, a.accent)
                line(14f, -2.8f, 58f, -4.8f, P.White, 1.5f, .22f)
            }
        }
        stroke(tripod, P.Dark2, 3f)
        dot(134f, 176f, 3.5f, P.Dark3)
        // Counter + till
        rr(334f, 222f, 136f, 8f, 3f, Color(0xFFD7D0E6))
        rr(338f, 230f, 128f, 22f, 4f, Color(0xFFC6BCDB))
        rect(446f, 186f, 4f, 9f, P.Dark3)
        card(436f, 174f, 22f, 14f, 3f, Color(0xFFFBFAF7), ink(.15f), 1f)
        val sale = d.active(3, 4)
        val p = if (sale) cyc(d.workT, 1.2f) else 0f
        if (sale) label(a.sale, 447f, 181.5f, display.at(p, 0))
        fill(register, P.Dark)
        for (r in 0..1) for (c in 0..2) rr(414f + 8f * c, 198f + 7f * r, 6f, 4f, 1f, P.Light, .8f)
        rr(440f, 199f, 11f, 10f, 2f, a.accent)
        if (sale) {
            val o = coin.at(p, 2)
            if (o > 0f) {
                val cy = coin.at(p, 0)
                scale(coin.at(p, 1), 1f, Offset(433f, cy)) {
                    dot(433f, cy, 5f, P.Gold, o)
                    ring(433f, cy, 5f, Color(0xFFE0A82E), 1f, o)
                    line(433f, cy - 2.6f, 433f, cy + 2.6f, Color(0xFFB9801A), 1.2f, o)
                }
            }
        }
        val dr = if (sale) drawer.at(p, 0) else 0f
        rr(414f, 216f + dr, 38f, 8f, 2f, P.Dark3)
        line(428f, 220f + dr, 438f, 220f + dr, P.White, 1.5f, .35f)
    }
}
