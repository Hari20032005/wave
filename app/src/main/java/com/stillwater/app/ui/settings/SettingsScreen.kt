package com.stillwater.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillwater.app.ui.components.CalmCard
import com.stillwater.app.ui.components.CalmPrimaryButton
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.components.CalmTone
import com.stillwater.app.ui.theme.Spacing

@Composable
fun SettingsScreen(
    onOpenPrivacyPolicy: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    var confirmingDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenEdge, vertical = Spacing.lg),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.lg))

        CalmCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Steady hand", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Require your device unlock before protection can be " +
                            "changed — so one hard moment can't undo a calm decision.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                Switch(
                    checked = state.protectionLockEnabled,
                    onCheckedChange = { wanted ->
                        val act = activity ?: return@Switch
                        // Turning the lock OFF is itself gated.
                        if (!wanted && state.protectionLockEnabled) {
                            AppLock.requireUnlock(act, "Turn off the lock") { ok ->
                                if (ok) viewModel.setProtectionLock(false)
                            }
                        } else {
                            viewModel.setProtectionLock(wanted)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))

        CalmCard {
            Text("Your data", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Everything lives on this phone. Export it as readable JSON, " +
                    "or erase all of it — completely, immediately, no copy anywhere.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            CalmQuietButton(
                text = "Export my data",
                onClick = {
                    viewModel.export { intent ->
                        context.startActivity(
                            android.content.Intent.createChooser(intent, "Export Stillwater data"),
                        )
                    }
                },
            )
            CalmQuietButton(
                text = "Delete all my data",
                onClick = { confirmingDelete = true },
            )
        }

        if (confirmingDelete) {
            Spacer(Modifier.height(Spacing.md))
            // Confirmation is calm and honest — no red panic styling.
            CalmCard(tone = CalmTone.Lapse) {
                Text("Erase everything?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Every urge, log, plan, and setting will be gone for good, and " +
                        "the app returns to its first screen. There is no undo — we keep " +
                        "no copies.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(Spacing.sm))
                CalmPrimaryButton(
                    text = "Yes, erase everything",
                    onClick = { viewModel.deleteEverything { confirmingDelete = false } },
                )
                CalmQuietButton(text = "Keep my data", onClick = { confirmingDelete = false })
            }
        }

        Spacer(Modifier.height(Spacing.md))
        CalmCard {
            Text("Privacy", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "No account, no cloud, no analytics. Read exactly what that means:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            CalmQuietButton(text = "Privacy policy", onClick = onOpenPrivacyPolicy)
        }

        Spacer(Modifier.height(Spacing.lg))
        CalmQuietButton(
            text = "Back",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
