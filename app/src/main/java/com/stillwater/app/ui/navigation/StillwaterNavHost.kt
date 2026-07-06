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
import com.stillwater.app.ui.home.NotificationDisclosureScreen
import com.stillwater.app.ui.onboarding.OnboardingScreen
import com.stillwater.app.ui.paywall.PaywallScreen
import com.stillwater.app.ui.plans.PlanBuilderScreen
import com.stillwater.app.ui.plans.PlanScreen
import com.stillwater.app.ui.progress.ProgressScreen
import com.stillwater.app.ui.protection.AppPickerScreen
import com.stillwater.app.ui.protection.ProtectionScreen
import com.stillwater.app.ui.settings.PrivacyPolicyScreen
import com.stillwater.app.ui.settings.SettingsScreen
import com.stillwater.app.ui.sos.SosScreen
import com.stillwater.app.ui.theme.Motion
import kotlinx.serialization.Serializable

/*
 * Type-safe routes. Later milestones add: PlanBuilder (M3), Progress +
 * Paywall (M5), Settings (M6) — added when built, not before.
 */
@Serializable data object OnboardingRoute
@Serializable data object HomeRoute
@Serializable data class SosRoute(
    val entryPoint: String = "IN_APP",
    val interceptedPackage: String? = null,
)
@Serializable data object NotificationDisclosureRoute
@Serializable data object PlanRoute
@Serializable data object PlanBuilderRoute
@Serializable data object ProtectionRoute
@Serializable data object AppPickerRoute
@Serializable data object ProgressRoute
@Serializable data object PaywallRoute
@Serializable data object SettingsRoute
@Serializable data object PrivacyPolicyRoute

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
            HomeScreen(
                onStartSos = { navController.navigate(SosRoute(entryPoint = "IN_APP")) },
                onLogSlip = { navController.navigate(SosRoute(entryPoint = "RETRO_LOG")) },
                onSetupQuickAccess = { navController.navigate(NotificationDisclosureRoute) },
                onOpenPlan = { navController.navigate(PlanRoute) },
                onOpenProtection = { navController.navigate(ProtectionRoute) },
                onOpenProgress = { navController.navigate(ProgressRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<SosRoute> {
            SosScreen(onClose = { navController.popBackStack() })
        }
        composable<NotificationDisclosureRoute> {
            NotificationDisclosureScreen(onDone = { navController.popBackStack() })
        }
        composable<PlanRoute> {
            PlanScreen(
                onCreatePlan = { navController.navigate(PlanBuilderRoute) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<PlanBuilderRoute> {
            PlanBuilderScreen(onFinished = { navController.popBackStack() })
        }
        composable<ProtectionRoute> {
            ProtectionScreen(
                onPickApps = { navController.navigate(AppPickerRoute) },
                onOpenPaywall = { navController.navigate(PaywallRoute) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<AppPickerRoute> {
            AppPickerScreen(onBack = { navController.popBackStack() })
        }
        composable<ProgressRoute> {
            ProgressScreen(onBack = { navController.popBackStack() })
        }
        composable<PaywallRoute> {
            PaywallScreen(onClose = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onOpenPrivacyPolicy = { navController.navigate(PrivacyPolicyRoute) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<PrivacyPolicyRoute> {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
    }
}
