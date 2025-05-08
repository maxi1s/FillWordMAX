package com.example.fillwordsmax.data

import com.example.fillwordsmax.model.GameField
import com.example.fillwordsmax.model.Cell
import com.example.fillwordsmax.model.Word
import kotlin.random.Random

object GameData {
    private val gameFields = mapOf(
        1 to generateGameField(
            id = 1,
            size = 4,
            wordsList = listOf("КОТ", "СОБАКА", "ПОПУГАЙ")
        ),
        2 to generateGameField(
            id = 2,
            size = 5,
            wordsList = listOf("ЛЕВ", "ТИГР", "ВОЛК")
        ),
        3 to generateGameField(
            id = 3,
            size = 5,
            wordsList = listOf("КИТ", "ДЕЛЬФИН", "АКУЛА")
        ),
        4 to generateGameField(
            id = 4,
            size = 5,
            wordsList = listOf("ЯБЛОКО", "ГРУША", "СЛИВА")
        ),
        5 to generateGameField(
            id = 5,
            size = 5,
            wordsList = listOf("МОРКОВЬ", "ОГУРЕЦ", "ЛУК")
        ),
        6 to generateGameField(
            id = 6,
            size = 4,
            wordsList = listOf("ЧАЙ", "КОФЕ", "СОК")
        ),
        7 to generateGameField(
            id = 7,
            size = 4,
            wordsList = listOf("РОЗА", "ТЮЛЬПАН", "ПАПОРОТНИК")
        ),
        8 to generateGameField(
            id = 8,
            size = 5,
            wordsList = listOf("РОМАШКА", "ЛИЛИЯ", "ГВОЗДИКА")
        ),
        9 to generateGameField(
            id = 9,
            size = 5,
            wordsList = listOf("ДУБ", "БЕРЕЗА", "СОСНА")
        )
    )

    fun getGameField(levelId: Int): GameField? = gameFields[levelId]
}

private fun randomChar(): Char {
    val alphabet = "АБВГДЕЖЗИКЛМНОПРСТУФХЦЧШЩЫЭЮЯ"
    return alphabet[Random.nextInt(alphabet.length)]
}

private fun generateGameField(id: Int, size: Int, wordsList: List<String>): GameField {
    val grid = Array(size) { Array(size) { '_' } }
    val used = Array(size) { BooleanArray(size) { false } }
    val placedWords = mutableListOf<Word>()
    val directions = listOf(
        Pair(0, 1),  // вниз
        Pair(1, 0),  // вправо
        Pair(0, -1), // вверх
        Pair(-1, 0)  // влево
    )

    fun canPlace(word: String, x: Int, y: Int, path: MutableList<Pair<Int, Int>>, used: Array<BooleanArray>): Boolean {
        if (path.size == word.length) return true
        for ((dx, dy) in directions.shuffled()) {
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until size && ny in 0 until size && !used[ny][nx]) {
                if (grid[ny][nx] == '_' || grid[ny][nx] == word[path.size]) {
                    used[ny][nx] = true
                    path.add(Pair(nx, ny))
                    if (canPlace(word, nx, ny, path, used)) return true
                    path.removeAt(path.size - 1)
                    used[ny][nx] = false
                }
            }
        }
        return false
    }

    for (word in wordsList) {
        var placed = false
        outer@for (y in 0 until size) {
            for (x in 0 until size) {
                if (grid[y][x] == '_' || grid[y][x] == word[0]) {
                    val path = mutableListOf(Pair(x, y))
                    used[y][x] = true
                    if (canPlace(word, x, y, path, used)) {
                        // Размещаем слово
                        for ((i, pos) in path.withIndex()) {
                            grid[pos.second][pos.first] = word[i]
                        }
                        placedWords.add(Word(word, path[0].first, path[0].second, true))
                        placed = true
                        break@outer
                    }
                    used[y][x] = false
                }
            }
        }
    }
    // Заполняем пустые клетки случайными буквами
    val cellGrid = List(size) { y ->
        List(size) { x ->
            Cell(x, y, if (grid[y][x] == '_') randomChar() else grid[y][x])
        }
    }
    return GameField(
        id = id,
        words = placedWords,
        grid = cellGrid,
        size = size
    )
} 