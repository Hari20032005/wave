package com.stillwater.app.ui.sos

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
import androidx.compose.ui.text.style.TextAlign
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.theme.Spacing

/**
 * Placeholder SOS destination — proves the nav scaffold and the calm
 * cross-fade. The real escalation ladder is M2.
 */
@Composable
fun SosScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Spacing.screenEdge),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "You're here.\nThat's the hard part done.",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Start,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = "The urge SOS flow is built in milestone M2.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xxl))
        CalmQuietButton(text = "Back", onClick = onClose)
    }
}
