package com.example.fillwordsmax.model

enum class HintLevel {
    NONE,      // Без подсказок
    EASY,      // Легкий - показываем прогресс
    HIGH       // Высокий - показываем все слова
}

data class GameSettings(
    var hintLevel: HintLevel = HintLevel.EASY
) 