package com.stillwater.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.theme.Spacing

/**
 * The in-app privacy policy. The identical text lives in PRIVACY_POLICY.md at
 * the repo root for hosting (Play requires a public URL) — keep them in sync.
 */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenEdge, vertical = Spacing.lg),
    ) {
        Text(
            text = "Privacy policy",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.lg))
        PolicySection(
            "The short version",
            "Stillwater collects nothing. Everything you enter stays in a private " +
                "database on your phone. There is no account, no server, no cloud sync, " +
                "and no analytics. We cannot see, sell, or leak your data, because it " +
                "never reaches us.",
        )
        PolicySection(
            "What the app stores (on your device only)",
            "Your mode and onboarding answers; urges and slips you log, with the " +
                "feelings, situations, and notes you attach; your if-then plans and " +
                "values; your chosen protected apps and risk windows; app settings. " +
                "This data lives in the app's private storage, protected by Android's " +
                "app sandbox and your device encryption.",
        )
        PolicySection(
            "App protection and permissions",
            "If you enable protection, the app reads the package name of the app " +
                "currently on screen (via Android's usage-access permission) to decide " +
                "whether to show a pause screen. It never reads content inside other " +
                "apps, notifications, messages, or browsing activity, and none of this " +
                "leaves the device. Notifications, overlay, and battery permissions are " +
                "used only for the features you explicitly turn on.",
        )
        PolicySection(
            "Purchases",
            "Subscriptions are processed entirely by Google Play. We receive no " +
                "payment details. The app only stores a yes/no record of whether " +
                "Premium is active.",
        )
        PolicySection(
            "Your controls",
            "Settings → Export my data gives you everything as a readable file. " +
                "Settings → Delete all my data erases everything immediately and " +
                "permanently — we keep no copies, so there is nothing for us to delete " +
                "on any server.",
        )
        PolicySection(
            "Children",
            "Stillwater is intended for adults (17+).",
        )
        PolicySection(
            "Changes and contact",
            "If this policy ever changes, the app will show the updated text here " +
                "before the change takes effect. Questions: hariharan944212005@gmail.com.",
        )
        Spacer(Modifier.height(Spacing.lg))
        CalmQuietButton(
            text = "Back",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.md))
}
