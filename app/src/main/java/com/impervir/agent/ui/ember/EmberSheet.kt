package com.impervir.agent.ui.ember

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.impervir.agent.ui.theme.ImpervirTheme

/** Name and "when it shows" line for each mood (from the character sheet). */
fun EmberMood.label(): String = when (this) {
    EmberMood.Idle -> "Idle"
    EmberMood.Waving -> "Hello"
    EmberMood.Listening -> "Listening"
    EmberMood.Talking -> "Talking"
    EmberMood.Excited -> "Excited"
    EmberMood.Curious -> "Curious"
    EmberMood.Concerned -> "Concerned"
    EmberMood.Thinking -> "Thinking"
    EmberMood.Writing -> "Writing"
    EmberMood.Typing -> "Typing"
    EmberMood.Happy -> "Happy"
    EmberMood.Wow -> "Wow"
    EmberMood.Proud -> "Proud"
    EmberMood.Oops -> "Oops"
    EmberMood.Sleeping -> "Sleeping"
}

fun EmberMood.whenShown(): String = when (this) {
    EmberMood.Idle -> "Resting on Home. Hands bob, body breathes softly."
    EmberMood.Waving -> "First open, and every Monday check-in."
    EmberMood.Listening -> "While you record. Hand cupped to listen."
    EmberMood.Talking -> "Asks or answers. Its hands talk too."
    EmberMood.Excited -> "Good news: a sale, a post that took off."
    EmberMood.Curious -> "Asks you a follow-up question."
    EmberMood.Concerned -> "Comments waiting, or sales dropped."
    EmberMood.Thinking -> "Planning your week. Taps its head."
    EmberMood.Writing -> "Drafting a post with pen and paper."
    EmberMood.Typing -> "Reading your old posts on its laptop."
    EmberMood.Happy -> "Story caught, post approved. It hops."
    EmberMood.Wow -> "A sales spike. Hands on cheeks."
    EmberMood.Proud -> "Best week yet. Hands on hips."
    EmberMood.Oops -> "A post failed or a line sounds like AI."
    EmberMood.Sleeping -> "Paused, or all done for the week."
}

/** All 15 moods in a grid with labels, plus the small sizes Ember is used at (debug / character screen). */
@Composable
fun EmberGallery(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val extras = ImpervirTheme.extras
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    "Ember",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "15 moods, picked from context.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extras.muted,
                )
            }
        }
        items(EmberMood.entries, key = { it.name }) { mood ->
            Surface(
                color = extras.card,
                shape = MaterialTheme.shapes.large,
                tonalElevation = 0.dp,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .semantics(mergeDescendants = true) {},
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Ember(mood, size = 112.dp)
                    Text(
                        mood.label(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        mood.whenShown(),
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 8.dp)) {
                Text("Small sizes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    for (s in SmallSizes) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Ember(
                                EmberMood.Idle,
                                size = s.dp,
                                modifier = Modifier.semantics { contentDescription = "Ember at $s dp" },
                            )
                            Text("$s dp", style = MaterialTheme.typography.labelSmall, color = extras.muted)
                        }
                    }
                }
            }
        }
    }
}

private val SmallSizes = intArrayOf(24, 32, 44, 64)
