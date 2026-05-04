package com.freedomfighter.jeuxdujour.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object LeMot : Screen("lemot")
    data object Hexagone : Screen("hexagone")
    data object Connexions : Screen("connexions")
}
