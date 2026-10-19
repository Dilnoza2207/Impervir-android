package com.impervir.agent.feature.voice.house

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.impervir.agent.ui.ember.Ember
import com.impervir.agent.ui.ember.EmberHands
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import kotlin.math.floor

/** One room's drawing, split in the three depth layers around Ember (back wall, furniture in front, things in front of the hands). */
internal interface RoomArt {
    fun DrawScope.back(d: HouseDirector, a: HouseArt)
    fun DrawScope.front(d: HouseDirector, a: HouseArt) {}
    fun DrawScope.top(d: HouseDirector, a: HouseArt) {}
}

private val RoomArts = arrayOf(ListeningStudio, MemoryLibrary, WritingWorkshop, InsightLab, PostOffice)

private val Ground = Color(0xFFEADBC8)
private val FloorLine = Color(0xFFDCC8B2)
private val DoorPost = Color(0xFFE4D3C3)
private val DoorSill = Color(0xFFD6C1AC)
private val DoorArch = svg("M-9 99 Q0 91 9 99")
private val BottomShade = Brush.verticalGradient(listOf(Color(0x00141318), Color(0x1F141318)), startY = 270f, endY = 300f)

/** World transform: stage scale + camera. */
private inline fun DrawScope.world(d: HouseDirector, block: DrawScope.() -> Unit) {
    val k = size.width / STAGE_W
    scale(k, k, Offset.Zero) { translate(-d.cam, 0f) { block() } }
}

/** Runs [block] in each room slot the camera can see (slot 5 is the twin of room 1 that makes the loop seamless). */
private inline fun DrawScope.visibleRooms(d: HouseDirector, block: DrawScope.(room: Int) -> Unit) {
    val first = floor(d.cam / ROOM_W).toInt().coerceIn(0, 5)
    val last = floor((d.cam + STAGE_W) / ROOM_W).toInt().coerceIn(0, 5)
    for (s in first..last) translate(s * ROOM_W, 0f) { block(s % 5) }
}

private fun DrawScope.drawBackLayer(d: HouseDirector, a: HouseArt) {
    drawRect(Ground)
    world(d) {
        visibleRooms(d) { r ->
            val room = Rooms[r]
            rect(0f, 0f, ROOM_W, WALL_H, room.wall)
            rect(0f, 0f, ROOM_W, 12f, P.Ink, .05f)
            rect(0f, 229f, ROOM_W, 9f, room.base)
            with(RoomArts[r]) { back(d, a) }
        }
        // Doorways between rooms (one at every seam, including both ends so the twin room matches exactly).
        val first = floor(d.cam / ROOM_W).toInt()
        val last = floor((d.cam + STAGE_W) / ROOM_W).toInt() + 1
        for (k in first..last) translate(k * ROOM_W, 0f) {
            rect(-9f, 0f, 18f, 98f, DoorPost)
            rect(-9f, 98f, 18f, 140f, P.Ink, .07f)
            stroke(DoorArch, P.Ink, 2f, .12f)
            rr(-12f, 235f, 24f, 5f, 2f, DoorSill)
        }
        rect(d.cam, 238f, STAGE_W, 2f, FloorLine)
        line(d.cam, 260f, d.cam + STAGE_W, 260f, P.Ink, 1.5f, .035f)
        line(d.cam, 282f, d.cam + STAGE_W, 282f, P.Ink, 1.5f, .035f)
        // (Ember draws its own contact shadow.)
    }
}

private fun DrawScope.drawFrontLayer(d: HouseDirector, a: HouseArt) {
    world(d) {
        with(HeldProps) { headphones(d, a) }
        visibleRooms(d) { r -> with(RoomArts[r]) { front(d, a) } }
        with(HeldProps) { under(d, a) }
    }
}

private fun DrawScope.drawTopLayer(d: HouseDirector, a: HouseArt) {
    world(d) {
        with(HeldProps) { over(d, a) }
        visibleRooms(d) { r -> with(RoomArts[r]) { top(d, a) } }
    }
    scale(size.width / STAGE_W, size.width / STAGE_W, Offset.Zero) {
        drawRect(BottomShade, Offset(0f, 270f), androidx.compose.ui.geometry.Size(STAGE_W, 30f))
    }
}

/**
 * Ember's house: a horizontally scrolling cross-section drawn in three canvases, with Ember between them
 * (a second, hands-only copy of Ember sits above the furniture so its mittens stay on the props).
 */
@Composable
internal fun HouseStage(director: HouseDirector, modifier: Modifier = Modifier) {
    val art = rememberHouseArt(ImpervirTheme.extras.accent)
    val label = director.sceneLabel.also { director.caption }
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(STAGE_W / STAGE_H)
            .shadow(24.dp, RoundedCornerShape(30.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(30.dp))
            .background(Rooms[0].wall)
            .semantics { contentDescription = label },
    ) {
        val unitPx = constraints.maxWidth / STAGE_W
        val unitDp = maxWidth / STAGE_W
        val quiet = Modifier.fillMaxSize().clearAndSetSemantics { }
        Canvas(quiet) { director.frame; drawBackLayer(director, art) }
        EmberInHouse(director, unitPx, unitDp, handsOnly = false)
        Canvas(quiet) { director.frame; drawFrontLayer(director, art) }
        EmberInHouse(director, unitPx, unitDp, handsOnly = true)
        Canvas(quiet) { director.frame; drawTopLayer(director, art) }
    }
}

/*
 * Ember's rig maps its 200 x 220 design onto a box via scaleFor (216 x 232 span, centred). A 116-unit box makes one design
 * unit exactly half a world unit, like the iOS mockup, with the design origin at (box + 8, box + 5.5).
 */
private const val EMBER_BOX = 116f
private const val DESIGN_OX = 8f
private const val DESIGN_OY = 5.5f

@Composable
private fun EmberInHouse(d: HouseDirector, unitPx: Float, unitDp: Dp, handsOnly: Boolean) {
    d.frame // follow the director every frame
    val still = d.paused
    val o = d.rigOut
    fun v(i: Int) = if (still) d.rigRest(i) else o[i]
    val curl = if (d.phase == Phase.Work) 0.65f else 0.3f
    val hands = EmberHands(
        left = Offset((DESIGN_OX + v(0) / 2f) / EMBER_BOX, (DESIGN_OY + v(1) / 2f) / EMBER_BOX),
        right = Offset((DESIGN_OX + v(3) / 2f) / EMBER_BOX, (DESIGN_OY + v(4) / 2f) / EMBER_BOX),
        leftRotation = v(2),
        rightRotation = v(5),
        leftCurl = curl,
        rightCurl = curl,
    )
    val tx = (d.ex - 50f - DESIGN_OX - d.cam) * unitPx
    val ty = (EMBER_TOP - DESIGN_OY + d.dy + d.bodyY) * unitPx
    val hc = d.handCenter
    val clip = remember { Path() }
    val mask = if (!handsOnly) Modifier else Modifier.drawWithContent {
        // Only the mittens of this copy show: an oval around each hand.
        val r = 12.5f * unitPx
        clip.reset()
        clip.addOval(Rect(Offset((DESIGN_OX + hc[0] / 2f) * unitPx, (DESIGN_OY + hc[1] / 2f) * unitPx), r))
        clip.addOval(Rect(Offset((DESIGN_OX + hc[2] / 2f) * unitPx, (DESIGN_OY + hc[3] / 2f) * unitPx), r))
        clipPath(clip) { this@drawWithContent.drawContent() }
    }
    Ember(
        mood = d.mood,
        modifier = Modifier
            .clearAndSetSemantics { }
            .graphicsLayer { translationX = tx; translationY = ty }
            .then(mask),
        size = unitDp * EMBER_BOX,
        hands = hands,
        tilt = v(6),
        squash = v(7),
        lookAt = Offset(d.gazeX.coerceIn(-1f, 1f), d.gazeY.coerceIn(-1f, 1f)),
    )
}

/** "Writing Workshop · Picking the right words", cross-fading on every change. */
@Composable
internal fun HouseCaption(director: HouseDirector, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    AnimatedContent(
        targetState = director.caption,
        transitionSpec = {
            (fadeIn(tween(450)) + slideInVertically(tween(500, easing = Motion.Emphasized)) { it / 3 }) togetherWith
                (fadeOut(tween(300)) + slideOutVertically(tween(400)) { -it / 3 })
        },
        modifier = modifier.fillMaxWidth(),
        label = "caption",
    ) { c ->
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(c.room) }
                withStyle(SpanStyle(color = muted)) { append(" · "); append(c.task) }
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Five room dots; tapping one sends Ember there. */
@Composable
internal fun RoomDots(current: Int, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Rooms.forEachIndexed { i, room ->
            val on = i == current
            val w by animateDpAsState(if (on) 22.dp else 7.dp, Motion.pop(), label = "dot")
            val c by animateColorAsState(
                if (on) ImpervirTheme.extras.accent else MaterialTheme.colorScheme.onSurface.copy(alpha = .26f),
                tween(300), label = "dotColor",
            )
            Box(
                Modifier
                    .size(48.dp)
                    .selectable(
                        selected = on,
                        onClick = { onPick(i) },
                        role = Role.Tab,
                        indication = ripple(bounded = false, radius = 20.dp),
                        interactionSource = null,
                    )
                    .semantics { contentDescription = "Send Ember to the ${room.name}" },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.width(w).height(7.dp).clip(RoundedCornerShape(4.dp)).background(c))
            }
        }
    }
}
