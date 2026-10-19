package com.impervir.agent.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.impervir.agent.ui.theme.ImpervirTheme
import com.impervir.agent.ui.theme.Motion
import com.impervir.agent.ui.theme.NumberStyle
import java.text.NumberFormat
import java.util.Locale

/** Bold section title ("Stories it heard", "Live drafts"...). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Standard surface card: white on paper in light theme, raised night surface in dark. */
@Composable
fun ImpervirCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = ImpervirTheme.extras.card)
    val shape = MaterialTheme.shapes.large
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = CardDefaults.cardElevation(0.dp)) {
            Column(Modifier.padding(20.dp), content = content)
        }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = CardDefaults.cardElevation(0.dp)) {
            Column(Modifier.padding(20.dp), content = content)
        }
    }
}

private val intFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)
fun Int.grouped(): String = intFormat.format(this)
fun Int.money(): String = "$" + intFormat.format(this)

/** A number in Geist tabular figures that counts up on first show. */
@Composable
fun CountUpText(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = NumberStyle.copy(fontSize = 34.sp),
    format: (Int) -> String = { it.grouped() },
    durationMs: Int = 900,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) { anim.animateTo(value.toFloat(), tween(durationMs, easing = FastOutSlowInEasing)) }
    Text(format(anim.value.toInt()), modifier = modifier, style = style)
}

/**
 * Headline whose words rise in from a blur, one after another (the brand's signature reveal).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlurRiseText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.displaySmall) {
    val words = remember(text) { text.split(" ") }
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        words.forEachIndexed { i, w ->
            val p = remember(text) { Animatable(0f) }
            LaunchedEffect(text) { p.animateTo(1f, Motion.reveal(delay = i * Motion.Stagger)) }
            Text(
                text = if (i < words.lastIndex) "$w " else w,
                style = style,
                modifier = Modifier
                    .graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 10.dp.toPx() }
                    .blur(((1f - p.value) * 10f).dp),
            )
        }
    }
}

/**
 * The "+ Add" pill: optional features stay hidden until the person adds them.
 */
@Composable
fun AddPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 6.dp))
    }
}

/**
 * An optional block the person added. Expands in, and has a close button to hide it again.
 */
@Composable
fun AddedBlock(
    visible: Boolean,
    title: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(280)) + fadeIn(tween(280)),
        exit = shrinkVertically(tween(220)) + fadeOut(tween(160)),
        modifier = modifier,
    ) {
        ImpervirCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove $title", modifier = Modifier.size(18.dp))
                }
            }
            Box(Modifier.padding(top = 8.dp)) { Column(content = content) }
        }
    }
}

/** Initials avatar (ink circle). */
@Composable
fun Avatar(initials: String, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.inverseSurface),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelMedium)
    }
}
