package com.aldef.system.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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

/** Menahan layar tetap menyala selama komposabel ini tampil (speedometer, kompas). */
@Composable
fun KeepScreenOn(enabled: Boolean = true) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
