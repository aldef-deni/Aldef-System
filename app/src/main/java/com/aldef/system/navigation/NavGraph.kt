package com.aldef.system.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aldef.system.data.Screen
import com.aldef.system.ui.screens.CalculatorScreen
import com.aldef.system.aldefai.ui.AldefAiSettingsScreen
import com.aldef.system.aldefai.ui.AldefAiSetupScreen
import com.aldef.system.ui.screens.CalendarScreen
import com.aldef.system.ui.screens.CompassScreen
import com.aldef.system.ui.screens.HomeScreen
import com.aldef.system.ui.screens.LoginScreen
import com.aldef.system.ui.screens.QrisScannerScreen
import com.aldef.system.ui.screens.SpeedometerScreen
import com.aldef.system.ui.screens.SplashScreen
import com.aldef.system.ui.screens.VaultScreen

private const val TRANSITION_MS = 420

@Composable
fun AldefNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(TRANSITION_MS)
            ) + fadeIn(tween(TRANSITION_MS))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(TRANSITION_MS)
            ) + fadeOut(tween(TRANSITION_MS))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(TRANSITION_MS)
            ) + fadeIn(tween(TRANSITION_MS))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(TRANSITION_MS)
            ) + fadeOut(tween(TRANSITION_MS))
        }
    ) {
        // Splash memudar, tidak menggeser — supaya perpindahan ke login terasa
        // menyatu dengan animasi logonya.
        composable(
            route = Screen.Splash.route,
            exitTransition = { fadeOut(tween(600)) + scaleOut(tween(600), targetScale = 1.08f) }
        ) {
            SplashScreen(navController)
        }
        composable(
            route = Screen.Login.route,
            enterTransition = { fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.94f) }
        ) {
            LoginScreen(navController)
        }
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Qris.route) { QrisScannerScreen(navController) }
        composable(Screen.Compass.route) { CompassScreen(navController) }
        composable(Screen.Calculator.route) { CalculatorScreen(navController) }
        composable(Screen.Speedometer.route) { SpeedometerScreen(navController) }
        composable(Screen.Calendar.route) { CalendarScreen(navController) }
        composable(Screen.AldefAi.route) { AldefAiSettingsScreen(navController) }
        composable(Screen.AldefAiSetup.route) { AldefAiSetupScreen(navController) }
        composable(Screen.Vault.route) { VaultScreen(navController) }
    }
}
