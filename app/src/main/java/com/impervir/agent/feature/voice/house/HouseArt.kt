package com.impervir.agent.feature.voice.house

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size
import com.impervir.agent.data.Platform
import com.impervir.agent.ui.components.BrandLogos
import com.impervir.agent.ui.components.brandColor
import com.impervir.agent.ui.theme.Geist

/** Things the house draws that need Compose resources: the accent, pre-measured labels and the official platform logos. */
@Stable
internal class HouseArt(
    val accent: Color,
    val deadlines: TextLayoutResult,
    val elevenDays: TextLayoutResult,
    val almostCut: TextLayoutResult,
    val initials: TextLayoutResult,
    val sale: TextLayoutResult,
    val logos: List<VectorPainter>,
    val logoTints: List<ColorFilter>,
)

/** Official logo order on the Post Office wall. */
internal val WallPlatforms = listOf(Platform.X, Platform.LinkedIn, Platform.Threads, Platform.Telegram)

@Composable
internal fun rememberHouseArt(accent: Color): HouseArt {
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val painters = WallPlatforms.map { rememberVectorPainter(BrandLogos.of(it)) }
    return remember(accent, measurer, painters) {
        HouseArt(
            accent = accent,
            deadlines = measurer.label("deadlines", 9f, P.Ink),
            elevenDays = measurer.label("eleven days", 9f, P.OnAccent),
            almostCut = measurer.label("almost cut", 9f, P.Ink),
            initials = measurer.label("MC", 10f, Color.White),
            sale = measurer.label("+\$29", 8f, Color(0xFFC2410C)),
            logos = painters,
            logoTints = WallPlatforms.map { ColorFilter.tint(it.brandColor()) },
        )
    }
}

/** Labels are measured at density 1 so one text pixel == one world unit; the stage scale makes them crisp. */
private fun TextMeasurer.label(text: String, size: Float, color: Color): TextLayoutResult =
    measure(
        text,
        TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = size.sp, color = color),
        density = Density(1f, 1f),
    )

/** Draws a pre-measured label centred on (cx, cy). */
internal fun DrawScope.label(t: TextLayoutResult, cx: Float, cy: Float, alpha: Float = 1f) =
    drawText(t, topLeft = androidx.compose.ui.geometry.Offset(cx - t.size.width / 2f, cy - t.size.height / 2f), alpha = alpha)

/** Draws an official platform logo (24-unit viewport) at (x, y) with the given world size. */
internal fun DrawScope.logo(painter: VectorPainter, tint: ColorFilter, x: Float, y: Float, size: Float) {
    translate(x, y) { with(painter) { draw(Size(size, size), colorFilter = tint) } }
}
