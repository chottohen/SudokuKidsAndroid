package com.example.sudokukidsandroid

import androidx.compose.ui.graphics.ImageBitmap

data class PuzzlePiece(
    val id: Int,           // row * COLS + col
    val correctRow: Int,
    val correctCol: Int,
    val bitmap: ImageBitmap
)

data class PuzzleState(
    val pieces: List<PuzzlePiece> = emptyList(),
    val board: List<List<PuzzlePiece?>> = List(4) { List(6) { null } },
    val carouselPieces: List<PuzzlePiece> = emptyList(),
    val isComplete: Boolean = false,
    val isLoading: Boolean = false,
    val imageLoaded: Boolean = false
)
