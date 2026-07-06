package com.stillwater.app.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.stillwater.app.ui.components.CalmPrimaryButton
import com.stillwater.app.ui.components.CalmQuietButton
import com.stillwater.app.ui.theme.Spacing
import com.stillwater.app.ui.theme.StillwaterTheme
import com.stillwater.app.ui.theme.Tones

/**
 * The full-screen intercept window (TYPE_APPLICATION_OVERLAY). Crisis UX:
 * three choices max, healthiest first, none of them shaming. Shown only
 * for user-selected apps inside user-defined risk windows.
 */
class InterceptOverlay(private val context: Context) {

    /**
     * API 30+ requires a window context to add windows from a service;
     * a plain service context throws or no-ops.
     */
    private val windowContext: Context =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val display = context.getSystemService(android.hardware.display.DisplayManager::class.java)
                .getDisplay(android.view.Display.DEFAULT_DISPLAY)
            context.createDisplayContext(display)
                .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        } else {
            context
        }

    private val windowManager =
        windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    val isShowing: Boolean get() = view != null

    fun show(
        appLabel: String,
        onSurf: () -> Unit,
        onSkip: () -> Unit,
        onContinue: () -> Unit,
    ) {
        if (view != null) return

        val owner = OverlayLifecycleOwner().also { it.attach() }
        val composeView = ComposeView(windowContext).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                StillwaterTheme(darkTheme = true) {
                    InterceptContent(
                        appLabel = appLabel,
                        onSurf = onSurf,
                        onSkip = onSkip,
                        onContinue = onContinue,
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )

        runCatching {
            windowManager.addView(composeView, params)
            view = composeView
            lifecycleOwner = owner
        }.onFailure {
            android.util.Log.e("InterceptOverlay", "addView failed", it)
            owner.detach()
        }
    }

    fun dismiss() {
        view?.let { v ->
            runCatching { windowManager.removeView(v) }
        }
        lifecycleOwner?.detach()
        view = null
        lifecycleOwner = null
    }
}

@Composable
private fun InterceptContent(
    appLabel: String,
    onSurf: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = Spacing.screenEdge),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "A wave?",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "You were opening $appLabel — and this is one of the hours " +
                    "you asked to be met at the door.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.xxl))
            CalmPrimaryButton(text = "Surf it with me", onClick = onSurf, isCrisis = true)
            Spacer(Modifier.height(Spacing.md))
            SandButton(text = "I'll skip it", onClick = onSkip)
            CalmQuietButton(
                text = "Continue anyway",
                onClick = onContinue,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

/** Warm-sand secondary — a dignified, full-size second choice. */
@Composable
private fun SandButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Spacing.sosTouchTarget),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = Tones.current.celebrateContainer,
            contentColor = Tones.current.onCelebrateContainer,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
