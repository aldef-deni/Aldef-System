package com.aldef.system.data

object UserCredentials {
    const val USERNAME = "aldef"
    const val PASSWORD = "deniretna"
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object QRIS : Screen("qris")
    object Compass : Screen("compass")
    object Calculator : Screen("calculator")
    object Speedometer : Screen("speedometer")
    object HiddenFiles : Screen("hidden_files")
}
