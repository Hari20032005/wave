package com.stillwater.app.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillwater.app.ui.components.CalmCard
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.components.CalmTone
import com.stillwater.app.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * Progress as self-compassion: urges surfed, a quiet trend, recovery speed.
 * No streaks, no resets, no comparisons — by design, forever.
 */
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenEdge, vertical = Spacing.lg),
    ) {
        Text(
            text = "Your water",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.lg))

        if (!state.hasAnyData) {
            CalmCard {
                Text(
                    text = "Nothing here yet — and that's fine. The first time you ride " +
                        "out a wave with the SOS flow, it lands here.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                StatTile(
                    value = state.totalSurfed.toString(),
                    label = "waves surfed",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = state.surfedThisWeek.toString(),
                    label = "this week",
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = state.metAtTheDoor.toString(),
                    label = "met at the door",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(Spacing.md))

            CalmCard {
                Text(text = "Waves surfed, week by week", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.md))
                TrendSparkline(points = state.weeklySurfed.map { it.surfed })
                Spacer(Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "6 weeks ago",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "this week: ${state.weeklySurfed.lastOrNull()?.surfed ?: 0}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))

            CalmCard(tone = CalmTone.Lapse) {
                Text(text = "Coming back", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = when {
                        state.lapseCount == 0 ->
                            "No slips logged. If one ever comes, it'll be information, " +
                                "not a verdict — and what will matter is how gently you return."
                        state.recoveryHours == null ->
                            "You've logged ${state.lapseCount} slip" +
                                (if (state.lapseCount == 1) "" else "s") +
                                ". What matters most is the coming back — that shows here " +
                                "once you surf your next wave."
                        else -> buildString {
                            append("After a slip, you've been back on your feet in about ")
                            append(formatHours(state.recoveryHours!!))
                            append(".")
                            when (state.recoveryImproving) {
                                true -> append(" You're coming back faster than before.")
                                false -> append(" However long it takes — coming back is what counts.")
                                null -> {}
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        CalmQuietButton(
            text = "Back",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

private fun formatHours(hours: Double): String = when {
    hours < 1.0 -> "${(hours * 60).roundToInt()} minutes"
    hours < 48.0 -> "${hours.roundToInt()} hour" + if (hours.roundToInt() == 1) "" else "s"
    else -> "${(hours / 24).roundToInt()} days"
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    CalmCard(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Single-series sparkline: one muted hue, 2dp line, no gridlines, endpoints
 * carried by the text labels around it. Quiet by construction.
 */
@Composable
private fun TrendSparkline(points: List<Int>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val baseline = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
    ) {
        if (points.size < 2) return@Canvas
        val max = (points.max().coerceAtLeast(1)).toFloat()
        val stepX = size.width / (points.size - 1)
        val yFor = { v: Int -> size.height - (v / max) * (size.height - 8f) - 4f }

        drawLine(
            color = baseline,
            start = Offset(0f, size.height - 1f),
            end = Offset(size.width, size.height - 1f),
            strokeWidth = 1.dp.toPx(),
        )

        val path = Path()
        points.forEachIndexed { i, v ->
            val x = i * stepX
            val y = yFor(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx()))

        // Mark only the latest point — no number confetti on every vertex.
        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx(),
            center = Offset((points.size - 1) * stepX, yFor(points.last())),
        )
    }
}
