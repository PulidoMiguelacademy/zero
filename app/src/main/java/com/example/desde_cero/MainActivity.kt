package com.example.desde_cero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.desde_cero.ui.navigation.AppNavigation
import com.example.desde_cero.ui.theme.Desde_ceroTheme
import com.example.desde_cero.viewmodel.AuthViewModel
import com.example.desde_cero.viewmodel.CurrencyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Desde_ceroTheme {
                val authViewModel: AuthViewModel = viewModel()
                val currencyViewModel: CurrencyViewModel = viewModel()

                AppNavigation(
                    authViewModel = authViewModel,
                    currencyViewModel = currencyViewModel
                )
            }
        }
    }
}