package com.example.fillwordsmax.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fillwordsmax.model.Cell
import com.example.fillwordsmax.model.GameField
import com.example.fillwordsmax.model.GameSettings
import com.example.fillwordsmax.model.HintLevel
import com.example.fillwordsmax.model.Level
import com.example.fillwordsmax.model.Word
import kotlin.math.abs
import com.example.fillwordsmax.data.GameData
import com.example.fillwordsmax.data.ProgressRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalDensity
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavController,
    level: Level,
    onLevelCompleted: (Level) -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var currentSettings by remember { mutableStateOf(GameSettings()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(level.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        }
    ) { padding ->
        GameField(
            level = level,
            settings = currentSettings,
            onLevelCompleted = onLevelCompleted
        )
    }

    if (showSettings) {
        SettingsDialog(
            currentSettings = currentSettings,
            onSettingsChanged = { newSettings ->
                currentSettings = newSettings
                showSettings = false
            },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
fun GameField(
    level: Level,
    settings: GameSettings,
    onLevelCompleted: (Level) -> Unit
) {
    var gameField by remember { mutableStateOf<GameField?>(null) }
    var selectedCells by remember { mutableStateOf<List<Cell>>(emptyList()) }
    var foundWords by remember { mutableStateOf<Set<String>>(emptySet()) }
    var foundWordPaths by remember { mutableStateOf<Map<String, List<Cell>>>(emptyMap()) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var isRunning by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Таймер
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            elapsedTime += 1
        }
    }

    gameField = createGameField(level.id)

    // Добавляем функцию выбора клетки
    fun onCellSelected(cell: Cell) {
        val updatedSelection = handleCellSelection(cell, selectedCells, gameField!!)
        selectedCells = updatedSelection
        val foundWord = checkWord(updatedSelection, gameField!!)
        if (foundWord != null) {
            foundWords = foundWords + foundWord
            foundWordPaths = foundWordPaths + (foundWord to updatedSelection)
            selectedCells = emptyList()
        }
    }

    // Функция для подсчёта звёзд (пример: 3 звезды < 30 сек, 2 звезды < 60 сек, иначе 1)
    fun calculateStars(time: Long): Int = when {
        time < 30 -> 3
        time < 60 -> 2
        else -> 1
    }

    val cellSize = 40.dp
    val cellSizePx = with(LocalDensity.current) { cellSize.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Показываем таймер
        Text(
            text = "Время: ${elapsedTime}s",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp)
        )
        // Game grid с Canvas
        Box(
            modifier = Modifier
                .padding(8.dp)
                .border(2.dp, Color.Black)
        ) {
            Column {
                gameField?.grid?.forEach { row ->
                    Row {
                        row.forEach { cell ->
                            val isSelected = selectedCells.any { it.x == cell.x && it.y == cell.y }
                            GameCell(
                                cell = cell,
                                isSelected = isSelected,
                                isFound = cell.isFound,
                                onClick = { onCellSelected(cell) }
                            )
                        }
                    }
                }
            }
            // Рисуем линии по найденным словам
            Canvas(modifier = Modifier.matchParentSize()) {
                foundWordPaths.values.forEach { pathCells ->
                    if (pathCells.isNotEmpty()) {
                        val path = Path()
                        path.moveTo(
                            pathCells[0].x * cellSizePx + cellSizePx / 2,
                            pathCells[0].y * cellSizePx + cellSizePx / 2
                        )
                        for (i in 1 until pathCells.size) {
                            path.lineTo(
                                pathCells[i].x * cellSizePx + cellSizePx / 2,
                                pathCells[i].y * cellSizePx + cellSizePx / 2
                            )
                        }
                        drawPath(
                            path = path,
                            color = Color.Red.copy(alpha = 0.8f),
                            style = Stroke(width = 6f)
                        )
                    }
                }
            }
        }
        // Word list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            when (settings.hintLevel) {
                HintLevel.NONE -> {}
                HintLevel.EASY -> {
                    Text(
                        text = "Найдено слов: ${foundWords.size} из ${gameField?.words?.size ?: 0}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                HintLevel.HIGH -> {
                    Text(
                        text = "Слова для поиска:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    gameField?.words?.forEach { word ->
                        WordItem(
                            word = word,
                            isFound = foundWords.contains(word.text)
                        )
                    }
                }
            }
        }
        // Кнопка подсказки с рекламой (перемещена под список слов)
        val context = LocalContext.current
        val activity = context as? Activity
        val rewardedHelper = remember { YandexRewardedHelper(context, "R-M-15419348-1") }
        Button(
            onClick = {
                if (activity != null) {
                    rewardedHelper.loadAndShow(activity, onReward = {
                        // После просмотра рекламы — подсказка
                        val notFoundWord = gameField?.words?.firstOrNull { !foundWords.contains(it.text) }
                        if (notFoundWord != null) {
                            val grid = gameField!!.grid
                            var x = notFoundWord.startX
                            var y = notFoundWord.startY
                            for (i in notFoundWord.text.indices) {
                                val cell = grid[y][x]
                                if (!cell.isFound) {
                                    cell.isFound = true
                                    break
                                }
                                if (notFoundWord.isHorizontal) {
                                    x += 1
                                } else {
                                    y += 1
                                }
                            }
                        }
                    },
                    onError = { errorMsg ->
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                    })
                }
            },
            enabled = gameField?.words?.any { !foundWords.contains(it.text) } == true,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Подсказка")
        }
        // Показываем звезды, если уровень завершён
        if (foundWords.size == gameField?.words?.size) {
            isRunning = false
            val stars = calculateStars(elapsedTime)
            val maxScore = 100
            val score = when (stars) {
                3 -> maxScore
                2 -> (maxScore * 0.7).toInt()
                else -> (maxScore * 0.4).toInt()
            }
            // Сохраняем прогресс
            ProgressRepository.saveLevelProgress(
                levelId = level.id,
                isCompleted = true,
                isLocked = false,
                score = score,
                time = elapsedTime,
                stars = stars
            )
            // Показываем результат
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Уровень пройден!", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Время: $elapsedTime сек", fontSize = 18.sp)
                Text("Очки: $score / $maxScore", fontSize = 18.sp)
                Row {
                    repeat(3) { i ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Звезда",
                            tint = if (i < stars) Color.Yellow else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            // Завершаем уровень через 2 секунды
            LaunchedEffect(Unit) {
                delay(2000)
                onLevelCompleted(level.copy(score = score, time = elapsedTime, stars = stars))
            }
        }
    }
}

@Composable
fun GameCell(
    cell: Cell,
    isSelected: Boolean,
    isFound: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = when {
                    isFound -> Color.Green.copy(alpha = 0.3f)
                    isSelected -> Color.Blue.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cell.letter.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isFound) 0.8f else 1f),
            textDecoration = if (isFound) TextDecoration.LineThrough else TextDecoration.None
        )
    }
}

@Composable
fun WordItem(
    word: Word,
    isFound: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = word.text,
            fontSize = 16.sp,
            color = if (isFound) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (isFound) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f)
        )
        if (isFound) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Found",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun handleCellSelection(cell: Cell, selectedCells: List<Cell>, gameField: GameField): List<Cell> {
    if (cell.isFound) return selectedCells

    return if (selectedCells.isEmpty()) {
        listOf(cell)
    } else {
        val lastCell = selectedCells.last()
        if (isAdjacent(lastCell, cell)) {
            selectedCells + cell
        } else {
            listOf(cell)
        }
    }
}

private fun isAdjacent(cell1: Cell, cell2: Cell): Boolean {
    return (cell1.x == cell2.x && Math.abs(cell1.y - cell2.y) == 1) ||
            (cell1.y == cell2.y && Math.abs(cell1.x - cell2.x) == 1)
}

private fun checkWord(selectedCells: List<Cell>, gameField: GameField): String? {
    if (selectedCells.size < 3) return null

    val word = selectedCells.joinToString("") { it.letter.toString() }
    return if (gameField.words.any { it.text == word }) word else null
}

private fun createGameField(levelId: Int): GameField? {
    return GameData.getGameField(levelId)
}

@Composable
fun SettingsDialog(
    currentSettings: GameSettings,
    onSettingsChanged: (GameSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHintLevel by remember { mutableStateOf(currentSettings.hintLevel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки") },
        text = {
            Column {
                Text("Уровень подсказок:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HintLevel.values().forEach { hintLevel ->
                        FilterChip(
                            selected = selectedHintLevel == hintLevel,
                            onClick = { selectedHintLevel = hintLevel },
                            label = {
                                Text(
                                    when (hintLevel) {
                                        HintLevel.NONE -> "Нет"
                                        HintLevel.EASY -> "Легкий"
                                        HintLevel.HIGH -> "Высокий"
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSettingsChanged(GameSettings(hintLevel = selectedHintLevel))
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

// Вспомогательная функция для получения пути слова (списка клеток)
private fun getWordPath(word: Word, grid: List<List<Cell>>): List<Cell> {
    val path = mutableListOf<Cell>()
    var x = word.startX
    var y = word.startY
    path.add(grid[y][x])
    for (i in 1 until word.text.length) {
        if (word.isHorizontal) {
            x += 1
        } else {
            y += 1
        }
        if (y in grid.indices && x in grid[y].indices) {
            path.add(grid[y][x])
        }
    }
    return path
} 