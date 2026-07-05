package com.stillwater.app.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.stillwater.app.ui.home.HomeScreen
import com.stillwater.app.ui.sos.SosScreen
import com.stillwater.app.ui.theme.StillwaterTheme

@Preview(name = "Home — Light", showBackground = true)
@Composable
fun HomeScreenPreview() {
    StillwaterTheme {
        HomeScreen(onStartSos = {})
    }
}

// Dark is the PRIMARY theme (late-night is the peak-risk context) —
// review every screen here first.
@Preview(
    name = "Home — Dark (primary)",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F1714,
)
@Composable
fun HomeScreenDarkPreview() {
    StillwaterTheme {
        HomeScreen(onStartSos = {})
    }
}

@Preview(name = "SOS — Light", showBackground = true)
@Composable
fun SosScreenPreview() {
    StillwaterTheme {
        SosScreen(onClose = {})
    }
}

@Preview(
    name = "SOS — Dark (primary)",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0F1714,
)
@Composable
fun SosScreenDarkPreview() {
    StillwaterTheme {
        SosScreen(onClose = {})
    }
}
