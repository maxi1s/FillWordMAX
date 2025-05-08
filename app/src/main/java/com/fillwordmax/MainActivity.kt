package com.fillwordmax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fillwordmax.auth.AuthManager
import com.fillwordmax.ui.AuthScreen
import com.fillwordmax.ui.GameScreen
import com.fillwordmax.ui.theme.FillWordMaxTheme

class MainActivity : ComponentActivity() {
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FillWordMaxTheme {
                AppNavigation(authManager)
            }
        }
    }
}

@Composable
fun AppNavigation(authManager: AuthManager) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AuthScreen(
                onSignIn = { email, password ->
                    // Обработка входа
                },
                onSignUp = { email, password ->
                    // Обработка регистрации
                },
                onAuthSuccess = {
                    navController.navigate("game") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        composable("game") {
            GameScreen(
                onSignOut = {
                    authManager.signOut()
                    navController.navigate("auth") {
                        popUpTo("game") { inclusive = true }
                    }
                }
            )
        }
    }
} 