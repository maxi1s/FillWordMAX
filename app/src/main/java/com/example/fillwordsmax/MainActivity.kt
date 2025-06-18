package com.example.fillwordsmax

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fillwordsmax.auth.AuthManager
import com.example.fillwordsmax.model.Level
import com.example.fillwordsmax.model.LevelCategory
import com.example.fillwordsmax.ui.AuthScreen
import com.example.fillwordsmax.ui.CategoriesScreen
import com.example.fillwordsmax.ui.GameScreen
import com.example.fillwordsmax.ui.LevelsScreen
import com.example.fillwordsmax.ui.theme.FillWordMaxTheme
import com.example.fillwordsmax.data.ProgressRepository
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import com.yandex.mobile.ads.common.MobileAds

class MainActivity : ComponentActivity() {
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        authManager = AuthManager(applicationContext)
        
        // Initialize Google Sign-In
        val clientId = getString(R.string.default_web_client_id)
        authManager.initGoogleSignIn(clientId)
        
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
    var isAuthenticated by remember { mutableStateOf<Boolean?>(null) } // null = не авторизирован
    var categories by remember { mutableStateOf(createInitialCategories()) }
    var isProgressLoaded by remember { mutableStateOf(false) }

    // Проверяем состояние авторизации при запуске
    LaunchedEffect(Unit) {
        isAuthenticated = authManager.isUserSignedIn()
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated == true) {
            isProgressLoaded = false
            categories = createInitialCategories()
            ProgressRepository.loadProgress { progressMap ->
                categories = categories.map { category ->
                    val updatedLevels = category.levels.map { level ->
                        progressMap[level.id]?.let { progress ->
                            level.copy(
                                isCompleted = progress.isCompleted,
                                isLocked = progress.isLocked,
                                score = progress.score,
                                time = progress.time,
                                stars = progress.stars
                            )
                        } ?: level
                    }.toMutableList()

                    // Открываем следующий уровень, если предыдущий пройден
                    for (i in 0 until updatedLevels.size - 1) {
                        if (updatedLevels[i].isCompleted) {
                            updatedLevels[i + 1] = updatedLevels[i + 1].copy(isLocked = false)
                        }
                    }

                    category.copy(levels = updatedLevels)
                }
                isProgressLoaded = true
            }
        }
    }

    when (isAuthenticated) { // Проверка на авторизацию
        null -> {
            // Показываем экран загрузки
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        true -> {
            if (!isProgressLoaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                NavHost(navController, startDestination = "categories") {
                    composable("auth") {
                        AuthScreen(
                            authManager = authManager,
                            onAuthSuccess = {
                                isAuthenticated = true
                                // isProgressLoaded сбросится в LaunchedEffect
                                navController.navigate("categories") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("categories") {
                        CategoriesScreen(
                            categories = categories,
                            onCategoryClick = { category: LevelCategory ->
                                navController.navigate("levels/${category.id}")
                            },
                            onSignOut = {
                                authManager.signOut()
                                isAuthenticated = false
                                categories = createInitialCategories()
                                isProgressLoaded = false
                                navController.navigate("auth") {
                                    popUpTo("categories") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(
                        route = "levels/{categoryId}",
                        arguments = listOf(
                            navArgument("categoryId") { type = NavType.IntType }
                        )
                    ) { backStackEntry ->
                        val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 1
                        val category = categories.find { it.id == categoryId }
                        category?.let {
                            LevelsScreen(
                                category = it,
                                onLevelClick = { level ->
                                    navController.navigate("game/${level.id}")
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                    composable(
                        route = "game/{levelId}",
                        arguments = listOf(
                            navArgument("levelId") { type = NavType.IntType }
                        )
                    ) { backStackEntry ->
                        val levelId = backStackEntry.arguments?.getInt("levelId") ?: 1
                        // Находим категорию, к которой принадлежит выбранный уровень
                        val selectedCategory = categories.find { category ->
                            category.levels.any { it.id == levelId }
                        }
                        val level = selectedCategory?.levels?.find { it.id == levelId }
                        if (level != null) {
                            GameScreen(
                                navController = navController,
                                level = level,
                                onLevelCompleted = { completedLevel ->
                                    val maxScore = 100
                                    val score = when {
                                        completedLevel.stars == 3 -> maxScore
                                        completedLevel.stars == 2 -> (maxScore * 0.7).toInt()
                                        else -> (maxScore * 0.4).toInt()
                                    }
                                    // Сохраняем прогресс в облако
                                    ProgressRepository.saveLevelProgress(
                                        levelId = completedLevel.id,
                                        isCompleted = true,
                                        isLocked = false, // или нужное значение
                                        score = score,
                                        time = completedLevel.time,
                                        stars = completedLevel.stars
                                    )
                                    // Обновляем локально, (хранение происходит и локально в том числе)
                                    categories = categories.map { category ->
                                        if (category.levels.any { it.id == completedLevel.id }) {
                                            category.copy(
                                                levels = category.levels.map { l ->
                                                    if (l.id == completedLevel.id) l.copy(
                                                        isCompleted = true,
                                                        score = completedLevel.score,
                                                        time = completedLevel.time,
                                                        stars = completedLevel.stars
                                                    )
                                                    else if (l.id == completedLevel.id + 1) l.copy(isLocked = false)
                                                    else l
                                                }
                                            )
                                        } else category
                                    }
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
        false -> {
            NavHost(navController, startDestination = "auth") {
                composable("auth") {
                    AuthScreen(
                        authManager = authManager,
                        onAuthSuccess = {
                            isAuthenticated = true
                            navController.navigate("categories") {
                                popUpTo("auth") { inclusive = true }
                            }
                        }
                    )
                }
                composable("categories") {
                    CategoriesScreen(
                        categories = categories,
                        onCategoryClick = { category: LevelCategory ->
                            navController.navigate("levels/${category.id}")
                        },
                        onSignOut = {
                            authManager.signOut()
                            isAuthenticated = false
                            categories = createInitialCategories()
                            isProgressLoaded = false
                            navController.navigate("auth") {
                                popUpTo("categories") { inclusive = true }
                            }
                        }
                    )
                }
                composable(
                    route = "levels/{categoryId}",
                    arguments = listOf(
                        navArgument("categoryId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 1
                    val category = categories.find { it.id == categoryId }
                    category?.let {
                        LevelsScreen(
                            category = it,
                            onLevelClick = { level ->
                                navController.navigate("game/${level.id}")
                            },
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
                composable(
                    route = "game/{levelId}",
                    arguments = listOf(
                        navArgument("levelId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val levelId = backStackEntry.arguments?.getInt("levelId") ?: 1
                    // Находим категорию к которой принадлежит выбр уровень
                    val selectedCategory = categories.find { category ->
                        category.levels.any { it.id == levelId }
                    }
                    val level = selectedCategory?.levels?.find { it.id == levelId }
                    if (level != null) {
                        GameScreen(
                            navController = navController,
                            level = level,
                            onLevelCompleted = { completedLevel ->
                                val maxScore = 100
                                val score = when {
                                    completedLevel.stars == 3 -> maxScore
                                    completedLevel.stars == 2 -> (maxScore * 0.7).toInt()
                                    else -> (maxScore * 0.4).toInt()
                                }
                                // Сохраняем прогресс в облако
                                ProgressRepository.saveLevelProgress(
                                    levelId = completedLevel.id,
                                    isCompleted = true,
                                    isLocked = false,
                                    score = score,
                                    time = completedLevel.time,
                                    stars = completedLevel.stars
                                )
                                // Обновляем локально
                                categories = categories.map { category ->
                                    if (category.levels.any { it.id == completedLevel.id }) {
                                        category.copy(
                                            levels = category.levels.map { l ->
                                                if (l.id == completedLevel.id) l.copy(
                                                    isCompleted = true,
                                                    score = completedLevel.score,
                                                    time = completedLevel.time,
                                                    stars = completedLevel.stars
                                                )
                                                else if (l.id == completedLevel.id + 1) l.copy(isLocked = false)
                                                else l
                                            }
                                        )
                                    } else category
                                }
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
//Инциализация уровней и категорий, уровни(id) в data/GameData
private fun createInitialCategories(): List<LevelCategory> {
    return listOf(
        LevelCategory(
            id = 1,
            name = "Животные",
            description = "Слова, связанные с животными",
            levels = listOf(
                Level(id = 1, name = "Домашние животные", category = "Животные", isLocked = false),
                Level(id = 2, name = "Дикие животные", category = "Животные", isLocked = true),
                Level(id = 3, name = "Морские животные", category = "Животные", isLocked = true)
            )
        ),
        LevelCategory(
            id = 2,
            name = "Еда",
            description = "Слова, связанные с едой",
            levels = listOf(
                Level(id = 4, name = "Фрукты", category = "Еда", isLocked = false),
                Level(id = 5, name = "Овощи", category = "Еда", isLocked = true),
                Level(id = 6, name = "Напитки", category = "Еда", isLocked = true)
            )
        ),
        LevelCategory(
            id = 3,
            name = "Природа",
            description = "Слова, связанные с природой",
            levels = listOf(
                Level(id = 7, name = "Растения", category = "Природа", isLocked = false),
                Level(id = 8, name = "Цветы", category = "Природа", isLocked = true),
                Level(id = 9, name = "Деревья", category = "Природа", isLocked = true)
            )
        )
    )
} 