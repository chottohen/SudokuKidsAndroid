package com.example.sudokukidsandroid

enum class Theme { ANIMALS, SAFARI, NUMBERS, MUSIC }

// Animaux colorés (style original)
val ANIMALS_4 = listOf("🐶", "🐱", "🐰", "🐸")
val ANIMALS_9 = listOf("🐶", "🐱", "🐰", "🐸", "🦁", "🐮", "🐷", "🐻", "🦊")

// Animaux safari (style flat, silhouettes)
val SAFARI_4 = listOf("🐘", "🦒", "🦓", "🐊")
val SAFARI_9 = listOf("🐘", "🦒", "🦓", "🐊", "🦏", "🦛", "🐆", "🐅", "🦁")

// Chiffres
val NUMBERS_4 = listOf("1", "2", "3", "4")
val NUMBERS_9 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")

// Instruments de musique
val MUSIC_4 = listOf("🎹", "🎸", "🥁", "🎺")
val MUSIC_9 = listOf("🎹", "🎸", "🥁", "🎺", "🎻", "🎷", "🪗", "🪘", "🪈")

fun symbolsFor(theme: Theme, size: Int): List<String> = when (theme) {
    Theme.ANIMALS -> if (size == 4) ANIMALS_4 else ANIMALS_9
    Theme.SAFARI  -> if (size == 4) SAFARI_4 else SAFARI_9
    Theme.NUMBERS -> if (size == 4) NUMBERS_4 else NUMBERS_9
    Theme.MUSIC   -> if (size == 4) MUSIC_4 else MUSIC_9
}

data class SudokuState(
    val size: Int = 4,
    val theme: Theme = Theme.ANIMALS,
    val solution: List<List<Int>> = emptyList(),
    val givens: List<List<Boolean>> = emptyList(),
    val userGrid: List<List<Int>> = emptyList(),
    val errorCells: Set<Pair<Int, Int>> = emptySet(),
    val selectedCell: Pair<Int, Int>? = null,
    val isSuccess: Boolean = false,
    val gridFlashError: Boolean = false
)

object SudokuGenerator {
    fun generate(size: Int): Pair<List<List<Int>>, List<List<Boolean>>> {
        val grid = Array(size) { IntArray(size) }
        solve(grid, size)
        val solution = grid.map { it.toList() }

        val givens = Array(size) { BooleanArray(size) { true } }
        val toRemove = if (size == 4) 6 else 40
        (0 until size * size).toMutableList().shuffled().take(toRemove).forEach { idx ->
            givens[idx / size][idx % size] = false
        }

        return solution to givens.map { it.toList() }
    }

    private fun solve(grid: Array<IntArray>, size: Int): Boolean {
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (grid[row][col] == 0) {
                    for (num in (1..size).toList().shuffled()) {
                        if (isValid(grid, row, col, num, size)) {
                            grid[row][col] = num
                            if (solve(grid, size)) return true
                            grid[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    private fun isValid(grid: Array<IntArray>, row: Int, col: Int, num: Int, size: Int): Boolean {
        if (num in grid[row]) return false
        if ((0 until size).any { grid[it][col] == num }) return false
        val boxSize = if (size == 4) 2 else 3
        val boxRow = (row / boxSize) * boxSize
        val boxCol = (col / boxSize) * boxSize
        for (r in boxRow until boxRow + boxSize) {
            for (c in boxCol until boxCol + boxSize) {
                if (grid[r][c] == num) return false
            }
        }
        return true
    }
}
