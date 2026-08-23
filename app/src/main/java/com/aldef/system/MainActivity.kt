package com.aldef.system

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.aldef.system.navigation.AldefNavGraph
import com.aldef.system.ui.theme.AldefSystemTheme

/**
 * FragmentActivity, bukan ComponentActivity: BiometricPrompt menuntut host
 * berbasis fragment untuk menampilkan dialog sistemnya.
 */
class MainActivity : FragmentActivity() {

    // Rute tujuan bila dibuka lewat perintah suara ALDEF AI ("buka kalkulator").
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingRoute = intent?.getStringExtra(EXTRA_ROUTE)

        setContent {
            AldefSystemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AldefNavGraph(navController = navController)

                    // Lompat ke fitur yang diminta suara, lalu konsumsi sekali.
                    LaunchedEffect(pendingRoute) {
                        pendingRoute?.let { route ->
                            navController.navigate(route)
                            pendingRoute = null
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = intent.getStringExtra(EXTRA_ROUTE)
    }

    companion object {
        /** Extra berisi route NavGraph tujuan (lihat [com.aldef.system.data.Screen]). */
        const val EXTRA_ROUTE = "aldef_target_route"
    }
}
