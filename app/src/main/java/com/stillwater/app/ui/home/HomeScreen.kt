package com.stillwater.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stillwater.app.ui.components.CalmCard
import com.stillwater.app.ui.components.CalmPrimaryButton
import com.stillwater.app.ui.components.CalmTone
import com.stillwater.app.ui.theme.Spacing

/**
 * M0 placeholder home. Its real job right now: prove the design system on a
 * device — palette, type, spacing, and the celebrate/lapse tones.
 */
@Composable
fun HomeScreen(onStartSos: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenEdge),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Stillwater",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Design-system preview build (M0)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.xxl))

        CalmCard(tone = CalmTone.Celebrate) {
            Text("Calm celebration", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "This is how an urge surfed will feel — a soft bloom, not confetti.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        CalmCard(tone = CalmTone.Lapse) {
            Text("Calm lapse", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "And this is a lapse — warm and matter-of-fact. Never red, never a failure screen.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(Spacing.xxl))

        CalmPrimaryButton(
            text = "I'm feeling an urge",
            onClick = onStartSos,
            isCrisis = true,
        )
    }
}
