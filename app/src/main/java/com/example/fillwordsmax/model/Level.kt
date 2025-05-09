package com.example.fillwordsmax.model

data class Level(
    val id: Int,
    val name: String,
    val category: String,
    val isLocked: Boolean = true,
    val isCompleted: Boolean = false,
    val score: Int = 0,
    val time: Long = 0L,
    val stars: Int = 0
)

data class LevelCategory(
    val id: Int,
    val name: String,
    val description: String,
    val levels: List<Level>
) 