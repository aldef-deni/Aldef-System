package com.aldef.system

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash bawaan sistem hanya jembatan sepersekian detik; animasi
        // sebenarnya digambar oleh SplashScreen berbasis Compose.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AldefSystemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AldefNavGraph(navController = rememberNavController())
                }
            }
        }
    }
}
