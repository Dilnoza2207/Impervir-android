package com.impervir.agent.feature.writing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.data.Platform
import com.impervir.agent.ui.components.ImpervirCard
import com.impervir.agent.ui.components.PlatformBadge
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle
import kotlin.math.cos
import kotlin.math.sin

/** Two soft color fields drifting behind the page (radial gradients, so no blur API is needed). */
@Composable
fun DriftBackground(modifier: Modifier = Modifier) {
    val accent = ImpervirTheme.extras.accent
    val lilac = ImpervirTheme.extras.lilac
    val alpha = if (ImpervirTheme.extras.isDark) 0.22f else 0.16f
    val t = rememberInfiniteTransition(label = "drift")
    val a by t.animateFloat(0f, 6.283f, infiniteRepeatable(tween(16000), RepeatMode.Reverse), label = "a")
    Canvas(modifier.fillMaxSize()) {
        val r = size.minDimension * 0.6f
        val c1 = Offset(size.width * (0.15f + 0.08f * cos(a)), size.height * (0.12f + 0.05f * sin(a)))
        val c2 = Offset(size.width * (0.9f - 0.08f * sin(a)), size.height * (0.42f + 0.06f * cos(a)))
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = alpha), Color.Transparent), c1, r), r, c1)
        drawCircle(Brush.radialGradient(listOf(lilac.copy(alpha = alpha), Color.Transparent), c2, r), r, c2)
    }
}

/** The status phrase, morphing letter by letter from one step to the next. */
@Composable
fun StatusPhrase(text: String, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(Motion.soft()) { it / 2 } + fadeIn(tween(260))) togetherWith
                (slideOutVertically(tween(220)) { -it / 2 } + fadeOut(tween(160)))
        },
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        label = "status",
    ) { phrase ->
        Row {
            phrase.forEachIndexed { i, ch ->
                val p = remember(phrase) { Animatable(0f) }
                LaunchedEffect(phrase) { p.animateTo(1f, tween(260, delayMillis = i * 22, easing = Motion.Emphasized)) }
                Text(
                    ch.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 8.dp.toPx() },
                )
            }
        }
    }
}

enum class DraftStatus(val label: String) { Waiting("Waiting"), Queued("Queued"), Writing("Writing"), Ready("Ready") }

/** A draft being written live: the typed part shows, the rest keeps its space so the card doesn't jump. */
@Composable
fun LiveDraftCard(platform: Platform, full: String, typed: Int, caret: Boolean, status: DraftStatus, source: String?) {
    val accent = ImpervirTheme.extras.accent
    val blink = rememberInfiniteTransition(label = "caret").animateFloat(1f, 0f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "c")
    ImpervirCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformBadge(platform, size = 28.dp)
            Text(platform.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 10.dp).weight(1f))
            StatusPill(status)
        }
        val text = buildAnnotatedString {
            append(full.substring(0, typed))
            if (caret) withStyle(SpanStyle(color = accent.copy(alpha = blink.value))) { append("▍") }
            withStyle(SpanStyle(color = Color.Transparent)) { append(full.substring(typed)) }
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp).clearAndSetSemantics {
                contentDescription = "${platform.label} draft, ${status.label}. ${full.substring(0, typed)}"
            },
        )
        if (source != null && status == DraftStatus.Ready) {
            Text(source, style = MaterialTheme.typography.bodySmall, color = ImpervirTheme.extras.muted, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun StatusPill(status: DraftStatus) {
    val ready = status == DraftStatus.Ready
    val writing = status == DraftStatus.Writing
    val bg by animateColorAsState(
        when { ready -> ImpervirTheme.extras.positive.copy(alpha = 0.14f); writing -> ImpervirTheme.extras.accent.copy(alpha = 0.12f); else -> MaterialTheme.colorScheme.surfaceVariant },
        label = "pill",
    )
    val fg = when { ready -> ImpervirTheme.extras.positive; writing -> ImpervirTheme.extras.accent; else -> ImpervirTheme.extras.muted }
    AnimatedContent(targetState = status.label, label = "pillText", transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }) { label ->
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg, modifier = Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/** One of the five checks: empty ring, then a ticked dark dot. "next" marks the step waiting on the person. */
@Composable
fun CheckDot(label: String, on: Boolean, next: Boolean, pulse: Boolean, dim: Boolean, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val fill by animateColorAsState(if (on) ink else ImpervirTheme.extras.card, tween(400), label = "fill")
    val tick by animateFloatAsState(if (on) 1f else 0.3f, Motion.pop(), label = "tick")
    val ring = rememberInfiniteTransition(label = "ring")
    val r by ring.animateFloat(0f, 1f, infiniteRepeatable(tween(1800)), label = "r")
    Column(
        modifier.graphicsLayer { alpha = if (dim) 0.5f else 1f }.semantics(mergeDescendants = true) {
            contentDescription = label + when { on -> ", done"; next -> ", next"; else -> ", waiting" }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            if (pulse) Box(
                Modifier.size(28.dp).scale(1f + r * 0.6f).graphicsLayer { alpha = 1f - r }
                    .border(1.5.dp, ImpervirTheme.extras.accent, CircleShape),
            )
            Box(
                Modifier.size(28.dp).background(fill, CircleShape)
                    .border(1.5.dp, if (on || next) ink else ink.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (on) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(16.dp).scale(tick))
                else if (next) Text("next", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.SemiBold))
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (on) ink else ImpervirTheme.extras.muted,
        )
    }
}

/** Platform progress ring with its official logo inside. */
@Composable
fun PlatformRing(platform: Platform, fraction: Float, count: String, modifier: Modifier = Modifier) {
    val accent = ImpervirTheme.extras.accent
    val track = MaterialTheme.colorScheme.outlineVariant
    val st = when { fraction <= 0f -> "Waiting"; fraction < 1f -> "Drafting"; else -> "Ready" }
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = "${platform.label}, $count, $st" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = 3.dp.toPx()
                drawCircle(track, style = Stroke(w), radius = size.minDimension / 2 - w)
                drawArc(
                    accent, -90f, 360f * fraction, false,
                    topLeft = Offset(w, w), size = androidx.compose.ui.geometry.Size(size.width - 2 * w, size.height - 2 * w),
                    style = Stroke(w, cap = StrokeCap.Round),
                )
            }
            PlatformBadge(platform, size = 32.dp)
        }
        Text(platform.label, style = MaterialTheme.typography.labelMedium)
        Text("$count · $st", style = NumberStyle.copy(fontSize = 11.sp), color = ImpervirTheme.extras.muted)
    }
}

/** Activity ticker: newest line on top, three visible, older ones slide down and fade. */
@Composable
fun ActivityTicker(lines: List<String>) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(36.dp * 3)) {
        val seen = lines.size
        lines.forEachIndexed { i, text ->
            val slot = seen - 1 - i
            val y by animateFloatAsState(slot * 36f, Motion.soft(), label = "y")
            val a by animateFloatAsState(if (slot < 3) 1f else 0f, tween(300), label = "a")
            val enter = remember(text) { Animatable(0f) }
            LaunchedEffect(text) { enter.animateTo(1f, tween(320, easing = Motion.Emphasized)) }
            Row(
                Modifier.height(36.dp).graphicsLayer {
                    translationY = (y - (1f - enter.value) * 18f).dp.toPx()
                    alpha = a * enter.value
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).background(if (slot == 0) ImpervirTheme.extras.accent else ImpervirTheme.extras.muted, CircleShape))
                Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}
