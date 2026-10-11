package com.example.desde_cero.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object UserHome : Screen("user_home")
    object AdminAudit : Screen("admin_audit")
}
