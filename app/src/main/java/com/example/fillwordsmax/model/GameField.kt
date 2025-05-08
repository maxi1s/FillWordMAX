package com.example.fillwordsmax.model

data class GameField(
    val id: Int,
    val words: List<Word>,
    val grid: List<List<Cell>>,
    val size: Int
)

data class Word(
    val text: String,
    val startX: Int,
    val startY: Int,
    val isHorizontal: Boolean,
    var isFound: Boolean = false
)

data class Cell(
    val x: Int,
    val y: Int,
    val letter: Char,
    var isSelected: Boolean = false,
    var isFound: Boolean = false
) 