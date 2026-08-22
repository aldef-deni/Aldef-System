package com.aldef.system.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aldef.system.data.Screen
import com.aldef.system.ui.screens.*

@Composable
fun AldefNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { fadeInIn(animationSpec = tween(500)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(500)) },
        exitTransition = { fadeOutOut(animationSpec = tween(500)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(500)) },
        popEnterTransition = { fadeInIn(animationSpec = tween(500)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(500)) },
        popExitTransition = { fadeOutOut(animationSpec = tween(500)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(500)) }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.QRIS.route) {
            QRISScannerScreen(navController = navController)
        }
        composable(Screen.Compass.route) {
            CompassScreen(navController = navController)
        }
        composable(Screen.Calculator.route) {
            CalculatorScreen(navController = navController)
        }
        composable(Screen.Speedometer.route) {
            SpeedometerScreen(navController = navController)
        }
        composable(Screen.HiddenFiles.route) {
            HiddenFilesScreen(navController = navController)
        }
    }
}
