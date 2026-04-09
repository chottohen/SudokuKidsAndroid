package com.example.sudokukidsandroid

import androidx.compose.ui.graphics.ImageBitmap

enum class EdgeType { FLAT, TAB, BLANK }

data class PieceEdges(
    val top: EdgeType,
    val right: EdgeType,
    val bottom: EdgeType,
    val left: EdgeType
)

data class PuzzlePiece(
    val id: Int,           // row * COLS + col
    val correctRow: Int,
    val correctCol: Int,
    val bitmap: ImageBitmap,
    val edges: PieceEdges
)

data class PuzzleState(
    val pieces: List<PuzzlePiece> = emptyList(),
    val board: List<List<PuzzlePiece?>> = List(4) { List(6) { null } },
    val carouselPieces: List<PuzzlePiece> = emptyList(),
    val thumbnail: ImageBitmap? = null,
    val isComplete: Boolean = false,
    val isLoading: Boolean = false,
    val imageLoaded: Boolean = false
)
