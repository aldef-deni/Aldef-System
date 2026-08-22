package com.aldef.system

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.aldef.system.navigation.AldefNavGraph
import com.aldef.system.ui.theme.AldefSystemTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var keepSplashScreen by mutableStateOf(true)

        splashScreen.setKeepOnScreenCondition {
            keepSplashScreen
        }

        setContent {
            AldefSystemTheme {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1500)
                    keepSplashScreen = false
                }

                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AldefNavGraph(navController = navController)
                }
            }
        }
    }
}
