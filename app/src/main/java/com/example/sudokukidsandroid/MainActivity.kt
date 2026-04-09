package com.example.sudokukidsandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.sudokukidsandroid.ui.theme.SudokuKidsAndroidTheme

sealed class AppScreen {
    object Menu : AppScreen()
    object Sudoku : AppScreen()
    object Puzzle : AppScreen()
    object Maze : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SudokuKidsAndroidTheme {
                var screen by remember { mutableStateOf<AppScreen>(AppScreen.Menu) }
                BackHandler(enabled = screen != AppScreen.Menu) {
                    screen = AppScreen.Menu
                }
                when (screen) {
                    AppScreen.Menu -> MainMenuScreen(
                        onPlaySudoku = { screen = AppScreen.Sudoku },
                        onPlayPuzzle = { screen = AppScreen.Puzzle },
                        onPlayMaze   = { screen = AppScreen.Maze }
                    )
                    AppScreen.Sudoku -> SudokuScreen(
                        onBack = { screen = AppScreen.Menu }
                    )
                    AppScreen.Puzzle -> PuzzleScreen(
                        onBack = { screen = AppScreen.Menu }
                    )
                    AppScreen.Maze -> MazeScreen(
                        onBack = { screen = AppScreen.Menu }
                    )
                }
            }
        }
    }
}
