package com.impervir.agent.feature.report

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.impervir.agent.ui.theme.Accent
import com.impervir.agent.ui.theme.Lilac
import com.impervir.agent.ui.theme.Night
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Three blob colors per card id (0..5). Warm for money, cool for voice, mixed for plans. */
private fun paletteFor(card: Int): Triple<Color, Color, Color> = when (card) {
    0 -> Triple(Accent, Color(0xFFFF9A5B), Lilac)
    1 -> Triple(Color(0xFF0A66C2), Accent, Color(0xFF3B2A8C))
    2 -> Triple(Accent, Color(0xFFFFC233), Color(0xFF26A5E4))
    3 -> Triple(Color(0xFFFF8A5B), Color(0xFFFF4D6D), Lilac)
    4 -> Triple(Lilac, Color(0xFF5BD3C7), Color(0xFF3B2A8C))
    else -> Triple(Lilac, Accent, Color(0xFF26A5E4))
}

/**
 * The report's living background, drawn in one Canvas: drifting blurred color blobs (radial gradients,
 * so it looks blurred on every API level), film grain, and a slow light sweep.
 */
@Composable
fun ReportBackdrop(card: Int, modifier: Modifier = Modifier) {
    val (a, b, c) = paletteFor(card)
    val ca by animateColorAsState(a, tween(900), label = "blobA")
    val cb by animateColorAsState(b, tween(900), label = "blobB")
    val cc by animateColorAsState(c, tween(900), label = "blobC")
    val t = rememberInfiniteTransition(label = "backdrop")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(22_000, easing = LinearEasing)), label = "drift")
    val sweep by t.animateFloat(0f, 1f, infiniteRepeatable(tween(7_000, easing = LinearEasing), RepeatMode.Restart), label = "sweep")
    val grainFrame by t.animateFloat(0f, 8f, infiniteRepeatable(tween(640, easing = LinearEasing)), label = "grain")
    val grain = remember {
        val r = Random(42)
        List(1600) { Offset(r.nextFloat(), r.nextFloat()) to (0.03f + r.nextFloat() * 0.07f) }
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(Night)
        val w = size.width
        val h = size.height
        val ang = (drift * 2 * PI).toFloat()
        blob(ca, Offset(w * (0.25f + 0.18f * sin(ang)), h * (0.22f + 0.08f * cos(ang * 1.3f))), w * 0.85f, 0.55f)
        blob(cb, Offset(w * (0.82f + 0.12f * cos(ang * 0.8f)), h * (0.55f + 0.1f * sin(ang * 1.1f))), w * 0.75f, 0.42f)
        blob(cc, Offset(w * (0.35f + 0.2f * sin(ang * 0.6f + 1f)), h * (0.88f + 0.06f * cos(ang))), w * 0.9f, 0.40f)

        // Dark vignette keeps text readable.
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.55f))))

        // Light sweep: a soft diagonal band crossing every few seconds (visible for the first 40% of the loop).
        val s = (sweep / 0.4f).coerceAtMost(1.2f)
        if (s < 1.2f) {
            val x = -w * 0.6f + s * w * 2.2f
            drawRect(
                Brush.linearGradient(
                    0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.07f), 1f to Color.Transparent,
                    start = Offset(x - w * 0.35f, 0f), end = Offset(x + w * 0.35f, h * 0.6f),
                ),
            )
        }

        // Grain: a fixed scatter that shifts a little every frame step.
        val f = grainFrame.toInt()
        val dx = (f * 37 % 11).toFloat()
        val dy = (f * 53 % 13).toFloat()
        val px = 1.4f
        grain.forEach { (p, alpha) ->
            drawRect(Color.White.copy(alpha = alpha), Offset((p.x * w + dx) % w, (p.y * h + dy) % h), Size(px, px))
        }
    }
}

private fun DrawScope.blob(color: Color, center: Offset, radius: Float, alpha: Float) {
    drawCircle(
        Brush.radialGradient(
            0f to color.copy(alpha = alpha),
            0.45f to color.copy(alpha = alpha * 0.45f),
            1f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
