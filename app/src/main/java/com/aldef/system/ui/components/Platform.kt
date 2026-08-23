package com.aldef.system.ui.components

import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.fragment.app.FragmentActivity

/** Menelusuri rantai ContextWrapper sampai menemukan Activity host. */
tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
fun rememberFragmentActivity(): FragmentActivity? {
    val context = LocalContext.current
    return remember(context) { context.findFragmentActivity() }
}

/**
 * Melaporkan apakah perangkat punya jaringan yang benar-benar tersambung.
 *
 * Yang diperiksa NET_CAPABILITY_VALIDATED, bukan sekadar "ada network":
 * terhubung ke Wi-Fi yang tidak jalan ke internet tetap dihitung offline.
 */
@Composable
fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    var online by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        if (manager == null) {
            online = false
            return@DisposableEffect onDispose { }
        }

        fun hasInternet(network: Network?): Boolean {
            val capabilities = network?.let { manager.getNetworkCapabilities(it) } ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        online = hasInternet(manager.activeNetwork)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                online = hasInternet(network)
            }

            override fun onLost(network: Network) {
                online = hasInternet(manager.activeNetwork)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { manager.registerNetworkCallback(request, callback) }

        onDispose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }

    return online
}

/** Menahan layar tetap menyala selama komposabel ini tampil (speedometer, kompas). */
@Composable
fun KeepScreenOn(enabled: Boolean = true) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
