data class Cell(
    val x: Int,
    val y: Int,
    val letter: Char,
    var isSelected: Boolean = false,
    var isFound: Boolean = false,
    var isHinted: Boolean = false
) 