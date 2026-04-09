package com.example.sudokukidsandroid

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MazeViewModel : ViewModel() {

    private val _state = MutableStateFlow(MazeGenerator.generate(Difficulty.EASY))
    val state: StateFlow<MazeState> = _state.asStateFlow()

    fun newMaze(difficulty: Difficulty = _state.value.difficulty) {
        _state.value = MazeGenerator.generate(difficulty)
    }

    fun selectEntrance(index: Int) {
        val s = _state.value
        if (s.isValidated) return
        _state.value = s.copy(selectedEntrance = if (s.selectedEntrance == index) null else index)
    }

    fun validate() {
        if (_state.value.selectedEntrance == null) return
        _state.value = _state.value.copy(isValidated = true)
    }

    fun retry() {
        _state.value = _state.value.copy(selectedEntrance = null, isValidated = false)
    }
}
