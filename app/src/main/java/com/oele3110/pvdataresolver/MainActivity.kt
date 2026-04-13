package com.oele3110.pvdataresolver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oele3110.pvdataresolver.data.auth.AuthRepository
import com.oele3110.pvdataresolver.ui.dashboard.DashboardScreen
import com.oele3110.pvdataresolver.ui.login.LoginScreen
import com.oele3110.pvdataresolver.ui.theme.PvDataResolverTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PvDataResolverTheme {
                SetStatusBarColor()
                val navController = rememberNavController()
                val startDestination = if (authRepository.isLoggedIn()) "dashboard" else "login"

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("dashboard") {
                        DashboardScreen()
                    }
                }
            }
        }
    }

    @Composable
    private fun SetStatusBarColor() {
        val color = MaterialTheme.colorScheme.background
        val window = this.window
        window.statusBarColor = color.toArgb()
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = color.luminance() > 0.5f
    }
}
