package com.impervir.agent.feature.voice.house

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/** Room 2: the Memory Library (best post, ladder to older posts, reading lamp, the "your voice" stamp, filing). */
internal object MemoryLibrary : RoomArt {
    private val caseC = Color(0xFFD2B79C)
    private val caseIn = Color(0xFFBC9F83)
    private val ladder = Color(0xFFB48A66)
    private val shade = svg("M296 148 L338 148 L330 124 L304 124 Z")
    private val glow = Brush.radialGradient(listOf(Color(0xBFFFD9A8), Color(0x00FFD9A8)), Offset.Zero, 1f)

    // Books: x, y, w, h, colour index (bottoms sit on the shelves).
    private val books = intArrayOf(
        34, 62, 9, 26, 0, 44, 63, 7, 25, 1, 52, 55, 9, 33, 2, 62, 61, 11, 27, 2, 74, 58, 7, 30, 0, 82, 61, 7, 27, 1, 90, 58, 11, 30, 2,
        102, 63, 11, 25, 3, 114, 64, 11, 24, 0, 126, 61, 7, 27, 2, 134, 62, 11, 26, 4, 146, 62, 10, 26, 1, 157, 60, 11, 28, 5, 169, 55, 7, 33, 3,
        177, 63, 9, 25, 1, 34, 103, 7, 35, 3, 42, 102, 10, 36, 0, 53, 105, 9, 33, 6, 63, 108, 9, 30, 3, 73, 101, 8, 37, 3, 82, 103, 7, 35, 4,
        90, 105, 11, 33, 7, 102, 108, 10, 30, 1, 113, 104, 7, 34, 0, 121, 100, 8, 38, 7, 130, 105, 8, 33, 0, 139, 102, 7, 36, 1, 147, 103, 11, 35, 7,
        159, 101, 9, 37, 7, 169, 105, 11, 33, 6, 181, 111, 7, 27, 4, 34, 162, 7, 26, 4, 42, 152, 11, 36, 6, 54, 151, 9, 37, 0, 64, 162, 9, 26, 6,
        74, 160, 9, 28, 1, 84, 162, 10, 26, 3, 95, 160, 9, 28, 3, 105, 156, 10, 32, 6, 116, 160, 7, 28, 6, 124, 154, 10, 34, 4, 135, 156, 8, 32, 4,
        144, 157, 10, 31, 0, 155, 152, 8, 36, 2, 184, 161, 8, 27, 5, 34, 200, 8, 34, 3, 43, 203, 7, 31, 5, 51, 206, 9, 28, 2, 61, 204, 8, 30, 7,
        70, 201, 11, 33, 7, 82, 199, 8, 35, 2, 91, 198, 10, 36, 0, 102, 204, 10, 30, 0, 113, 203, 7, 31, 0, 121, 207, 7, 27, 1, 129, 203, 8, 31, 5,
        138, 205, 7, 29, 2, 146, 210, 7, 24, 5, 154, 209, 11, 25, 7, 166, 210, 11, 24, 1, 178, 201, 8, 33, 0, 187, 200, 8, 34, 4,
    )
    private val bookColors = arrayOf(
        Color(0xFFE9D6C3), P.Lilac, P.Dark, P.Cream, P.SoftLilac, P.Peach, Color(0xFF5E5A63), Color.Unspecified, // 7 = accent
    )
    private val deskTop = Color(0xFFD8BA9C)
    private val deskBody = Color(0xFFC9A584)

    private val inkKeys = Keys(f(0f, .44f, .48f, .56f, .85f, 1f), EaseOut, f(0f, 0f, 1f, 1f, 1f, 0f), f(.6f, .6f, 1.1f, 1f, 1f, 1f))

    override fun DrawScope.back(d: HouseDirector, a: HouseArt) {
        // Bookcase
        rr(24f, 34f, 182f, 204f, 6f, caseC)
        rect(31f, 41f, 168f, 193f, caseIn)
        var i = 0
        while (i < books.size) {
            val c = books[i + 4]
            rr(books[i].toFloat(), books[i + 1].toFloat(), books[i + 2].toFloat(), books[i + 3].toFloat(), 1.5f, if (c == 7) a.accent else bookColors[c])
            i += 5
        }
        for (y in 0..2) rect(31f, 88f + 50f * y, 168f, 5f, caseC)
        // Ladder
        rr(22f, 30f, 5f, 216f, 2f, ladder)
        rr(106f, 30f, 5f, 216f, 2f, ladder)
        for (r in 0..8) rect(27f, 52f + 22f * r, 79f, 4f, ladder)
        // Reading lamp glow (brighter while Ember reads)
        translate(286f, 196f) {
            scale(86f, 64f, Offset.Zero) { drawCircle(glow, 1f, Offset.Zero, alpha = d.lamp) }
        }
        rect(314f, 146f, 4f, 96f, P.Dark)
        oval(316f, 243f, 15f, 3.5f, P.Dark)
        fill(shade, P.Peach)
        oval(317f, 150f, 10f, 3f, Color(0xFFFFE7C2))
        // Portrait of Maya
        card(372f, 64f, 56f, 48f, 8f, P.Cream, P.InkFaint, 2f)
        dot(400f, 88f, 13f, P.Ink)
        label(a.initials, 400f, 88f)
    }

    override fun DrawScope.front(d: HouseDirector, a: HouseArt) {
        rr(334f, 194f, 24f, 4f, 1.5f, Color(0xFFCDB89F))
        rr(334f, 220f, 118f, 8f, 3f, deskTop)
        rr(338f, 228f, 110f, 24f, 4f, deskBody)
        line(380f, 238f, 406f, 238f, P.Ink, 2f, .15f)
        card(410f, 214f, 28f, 7f, 1.5f, P.White, ink(.12f), 1f)
        // The "your voice" stamp mark appears each time the stamp lands.
        if (d.active(1, 3)) {
            val p = cyc(d.workT, 1.2f)
            val o = inkKeys.at(p, 0)
            if (o > 0f) {
                val s = inkKeys.at(p, 1)
                scale(s, s, Offset(424f, 217.5f)) {
                    ovalLine(424f, 217.5f, 8f, 2.6f, a.accent, 1.5f, o)
                    line(419.5f, 217.5f, 428.5f, 217.5f, a.accent, 1.2f, o)
                }
            }
        }
    }

    override fun DrawScope.top(d: HouseDirector, a: HouseArt) {
        // Card file box Ember drops cards into
        card(334f, 197f, 24f, 25f, 3f, Color(0xFFEFE6D8), ink(.1f), 1f)
        rr(340f, 204f, 12f, 5f, 1.5f, P.Lilac, .6f)
    }
}
