package com.aldef.system.data

/** Kredensial tunggal aplikasi pribadi ini. */
object UserCredentials {
    const val USERNAME = "aldef"
    const val PASSWORD = "deniretna"

    fun matches(user: String, pass: String): Boolean =
        user.trim().equals(USERNAME, ignoreCase = true) && pass == PASSWORD
}

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Qris : Screen("qris")
    data object Compass : Screen("compass")
    data object Calculator : Screen("calculator")
    data object Speedometer : Screen("speedometer")
    data object Calendar : Screen("calendar")
    data object Vault : Screen("vault")
}
