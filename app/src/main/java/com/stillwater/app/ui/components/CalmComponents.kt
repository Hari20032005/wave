package com.stillwater.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.stillwater.app.ui.theme.Spacing
import com.stillwater.app.ui.theme.Tones

/*
 * Base components with the calm-celebration and calm-lapse variants baked in.
 * Feature code composes THESE, not raw Material buttons/cards, so no screen
 * can drift into error-red lapses or confetti celebrations later.
 */

/** Emotional register of a surface. Chosen here, once — not per screen. */
enum class CalmTone { Neutral, Celebrate, Lapse }

@Composable
private fun CalmTone.container(): Color = when (this) {
    CalmTone.Neutral -> MaterialTheme.colorScheme.surfaceContainer
    CalmTone.Celebrate -> Tones.current.celebrateContainer
    CalmTone.Lapse -> Tones.current.lapseContainer
}

@Composable
private fun CalmTone.content(): Color = when (this) {
    CalmTone.Neutral -> MaterialTheme.colorScheme.onSurface
    CalmTone.Celebrate -> Tones.current.onCelebrateContainer
    CalmTone.Lapse -> Tones.current.onLapseContainer
}

/**
 * Standard card. `tone = CalmTone.Lapse` renders warm sand — the ONLY
 * sanctioned styling for lapse content. `Celebrate` is the soft green bloom.
 */
@Composable
fun CalmCard(
    modifier: Modifier = Modifier,
    tone: CalmTone = CalmTone.Neutral,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = tone.container(),
        contentColor = tone.content(),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), content = content)
    }
}

/**
 * The one filled button per screen (single focal action). `isCrisis = true`
 * grows it to the SOS touch target for use inside the urge flow.
 */
@Composable
fun CalmPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCrisis: Boolean = false,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = if (isCrisis) Spacing.sosTouchTarget else Spacing.minTouchTarget + Spacing.sm),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            // Disabled reads as "not yet", never as an error.
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Quiet escape hatch ("Not now", "Skip"). Always low-emphasis: leaving a
 * flow is never punished visually (user-in-control, Design Psychology #10).
 */
@Composable
fun CalmQuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = Spacing.minTouchTarget),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
