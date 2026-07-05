package com.stillwater.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.stillwater.app.ui.navigation.HomeRoute
import com.stillwater.app.ui.navigation.OnboardingRoute
import com.stillwater.app.ui.navigation.StillwaterNavHost
import com.stillwater.app.ui.theme.StillwaterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            StillwaterTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Empty surface while prefs load (a few ms) — avoids
                    // flashing the wrong start destination.
                    when (val state = uiState) {
                        is MainUiState.Loading -> Unit
                        is MainUiState.Ready -> StillwaterNavHost(
                            navController = rememberNavController(),
                            startDestination = if (state.onboardingComplete) {
                                HomeRoute
                            } else {
                                OnboardingRoute
                            },
                        )
                    }
                }
            }
        }
    }
}
