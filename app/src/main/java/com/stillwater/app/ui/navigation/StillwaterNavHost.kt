package com.stillwater.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stillwater.app.ui.home.HomeScreen
import com.stillwater.app.ui.onboarding.OnboardingScreen
import com.stillwater.app.ui.sos.SosScreen
import com.stillwater.app.ui.theme.Motion
import kotlinx.serialization.Serializable

/*
 * Type-safe routes. Later milestones add: PlanBuilder (M3), Progress +
 * Paywall (M5), Settings (M6) — added when built, not before.
 */
@Serializable data object OnboardingRoute
@Serializable data object HomeRoute
@Serializable data object SosRoute

@Composable
fun StillwaterNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        // Cross-fades at a calm pace — no slide/pop transitions anywhere.
        enterTransition = { fadeIn(tween(Motion.CALM, easing = Motion.CalmEase)) },
        exitTransition = { fadeOut(tween(Motion.CALM, easing = Motion.CalmEase)) },
        popEnterTransition = { fadeIn(tween(Motion.CALM, easing = Motion.CalmEase)) },
        popExitTransition = { fadeOut(tween(Motion.CALM, easing = Motion.CalmEase)) },
    ) {
        composable<OnboardingRoute> {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(HomeRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                },
            )
        }
        composable<HomeRoute> {
            HomeScreen(onStartSos = { navController.navigate(SosRoute) })
        }
        composable<SosRoute> {
            SosScreen(onClose = { navController.popBackStack() })
        }
    }
}
