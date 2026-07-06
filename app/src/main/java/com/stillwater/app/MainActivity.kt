package com.stillwater.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.stillwater.app.ui.navigation.HomeRoute
import com.stillwater.app.ui.navigation.OnboardingRoute
import com.stillwater.app.ui.navigation.SosRoute
import com.stillwater.app.ui.navigation.StillwaterNavHost
import com.stillwater.app.ui.theme.StillwaterTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_OPEN_SOS = "com.stillwater.app.OPEN_SOS"
        const val EXTRA_ENTRY_POINT = "entry_point"
        const val EXTRA_INTERCEPTED_PACKAGE = "intercepted_package"
    }

    private val viewModel: MainViewModel by viewModels()

    /** Set by widget/notification/intercept taps; consumed once the nav graph is up. */
    private val pendingSosEntryPoint = MutableStateFlow<Pair<String, String?>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        consumeSosIntent(intent)
        setContent {
            StillwaterTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Empty surface while prefs load (a few ms) — avoids
                    // flashing the wrong start destination.
                    when (val state = uiState) {
                        is MainUiState.Loading -> Unit
                        is MainUiState.Ready -> {
                            val navController = rememberNavController()
                            StillwaterNavHost(
                                navController = navController,
                                startDestination = if (state.onboardingComplete) {
                                    HomeRoute
                                } else {
                                    OnboardingRoute
                                },
                            )
                            val pendingSos by pendingSosEntryPoint.collectAsStateWithLifecycle()
                            LaunchedEffect(pendingSos) {
                                val (entryPoint, pkg) = pendingSos ?: return@LaunchedEffect
                                // SOS entry points only make sense once set up.
                                if (state.onboardingComplete) {
                                    navController.navigate(
                                        SosRoute(entryPoint = entryPoint, interceptedPackage = pkg),
                                    )
                                }
                                pendingSosEntryPoint.value = null
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeSosIntent(intent)
    }

    private fun consumeSosIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_SOS) {
            pendingSosEntryPoint.value =
                (intent.getStringExtra(EXTRA_ENTRY_POINT) ?: "WIDGET") to
                intent.getStringExtra(EXTRA_INTERCEPTED_PACKAGE)
        }
    }
}
