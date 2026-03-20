package com.example.sudokukidsandroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SudokuViewModel : ViewModel() {
    private val _state = MutableStateFlow(SudokuState())
    val state: StateFlow<SudokuState> = _state.asStateFlow()

    init {
        newGame(4)
    }

    fun newGame(size: Int = _state.value.size, theme: Theme = _state.value.theme) {
        val (solution, givens) = SudokuGenerator.generate(size)
        val userGrid = List(size) { row ->
            List(size) { col -> if (givens[row][col]) solution[row][col] else 0 }
        }
        _state.value = SudokuState(
            size = size,
            theme = theme,
            solution = solution,
            givens = givens,
            userGrid = userGrid
        )
    }

    fun setTheme(theme: Theme) {
        _state.value = _state.value.copy(theme = theme)
    }

    fun selectCell(row: Int, col: Int) {
        val s = _state.value
        if (s.givens[row][col]) return
        val newSelected = if (s.selectedCell == Pair(row, col)) null else Pair(row, col)
        _state.value = s.copy(selectedCell = newSelected, errorCells = emptySet())
    }

    fun placeAnimal(value: Int) {
        val s = _state.value
        val (row, col) = s.selectedCell ?: return
        val newGrid = s.userGrid.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, v -> if (r == row && c == col) value else v }
        }
        _state.value = s.copy(userGrid = newGrid, selectedCell = null, errorCells = emptySet())
    }

    fun clearSelectedCell() {
        val s = _state.value
        val (row, col) = s.selectedCell ?: return
        val newGrid = s.userGrid.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, v -> if (r == row && c == col) 0 else v }
        }
        _state.value = s.copy(userGrid = newGrid, errorCells = emptySet())
    }

    fun checkGrid() {
        val s = _state.value
        val errors = mutableSetOf<Pair<Int, Int>>()
        for (row in 0 until s.size) {
            for (col in 0 until s.size) {
                if (!s.givens[row][col] && s.userGrid[row][col] != 0 &&
                    s.userGrid[row][col] != s.solution[row][col]
                ) {
                    errors.add(Pair(row, col))
                }
            }
        }
        val allFilled = (0 until s.size).all { row ->
            (0 until s.size).all { col -> s.userGrid[row][col] != 0 }
        }
        val success = errors.isEmpty() && allFilled
        _state.value = s.copy(
            errorCells = errors,
            isSuccess = success,
            gridFlashError = errors.isNotEmpty()
        )
        if (errors.isNotEmpty()) {
            viewModelScope.launch {
                delay(800)
                _state.value = _state.value.copy(gridFlashError = false)
            }
        }
    }

    fun resetGrid() {
        val s = _state.value
        val userGrid = List(s.size) { row ->
            List(s.size) { col -> if (s.givens[row][col]) s.solution[row][col] else 0 }
        }
        _state.value = s.copy(
            userGrid = userGrid,
            errorCells = emptySet(),
            selectedCell = null,
            isSuccess = false
        )
    }
}
