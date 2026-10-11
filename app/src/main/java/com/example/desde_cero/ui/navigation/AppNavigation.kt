package com.example.desde_cero.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.desde_cero.model.UserRole
import com.example.desde_cero.ui.screens.admin.AdminAuditScreen
import com.example.desde_cero.ui.screens.auth.LoginScreen
import com.example.desde_cero.ui.screens.auth.RegisterScreen
import com.example.desde_cero.ui.screens.user.UserHomeScreen
import com.example.desde_cero.viewmodel.AuthViewModel
import com.example.desde_cero.viewmodel.CurrencyViewModel

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    currencyViewModel: CurrencyViewModel,
    navController: NavHostController = rememberNavController()
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    val startDestination = when {
        currentUser?.role == UserRole.ADMIN -> Screen.AdminAudit.route
        currentUser?.role == UserRole.USER -> Screen.UserHome.route
        else -> Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = {
                    authViewModel.clearRegisterStatus()
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = { user ->
                    val targetRoute = if (user.role == UserRole.ADMIN) Screen.AdminAudit.route else Screen.UserHome.route
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    authViewModel.clearLoginStatus()
                    navController.popBackStack()
                },
                onRegisterSuccess = { user ->
                    val targetRoute = if (user.role == UserRole.ADMIN) Screen.AdminAudit.route else Screen.UserHome.route
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.UserHome.route) {
            UserHomeScreen(
                authViewModel = authViewModel,
                currencyViewModel = currencyViewModel,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminAudit.route) {
            AdminAuditScreen(
                authViewModel = authViewModel,
                currencyViewModel = currencyViewModel,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
