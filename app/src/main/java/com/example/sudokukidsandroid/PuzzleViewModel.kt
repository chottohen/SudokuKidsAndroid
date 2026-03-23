package com.example.sudokukidsandroid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PuzzleViewModel : ViewModel() {

    private val _state = MutableStateFlow(PuzzleState())
    val state: StateFlow<PuzzleState> = _state.asStateFlow()

    companion object {
        const val COLS = 6
        const val ROWS = 4
    }

    fun loadImage(context: Context, uri: Uri) {
        _state.value = PuzzleState(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, uri)
                ) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            // Ensure ARGB_8888 (required for createBitmap slicing)
            val bitmap = raw.copy(Bitmap.Config.ARGB_8888, false)

            // Scale to a fixed canvas: COLS*120 x ROWS*120 = 720x480
            val targetW = COLS * 120
            val targetH = ROWS * 120
            val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            val tileW = targetW / COLS
            val tileH = targetH / ROWS

            val pieces = (0 until ROWS).flatMap { row ->
                (0 until COLS).map { col ->
                    val tile = Bitmap.createBitmap(scaled, col * tileW, row * tileH, tileW, tileH)
                        .copy(Bitmap.Config.ARGB_8888, false)
                    PuzzlePiece(
                        id = row * COLS + col,
                        correctRow = row,
                        correctCol = col,
                        bitmap = tile.asImageBitmap()
                    )
                }
            }

            withContext(Dispatchers.Main) {
                _state.value = PuzzleState(
                    pieces = pieces,
                    carouselPieces = pieces.shuffled(),
                    imageLoaded = true
                )
            }
        }
    }

    fun tryPlacePiece(piece: PuzzlePiece, targetRow: Int, targetCol: Int) {
        val s = _state.value
        if (piece !in s.carouselPieces) return
        if (s.board[targetRow][targetCol] != null) return  // cell already occupied
        if (piece.correctRow != targetRow || piece.correctCol != targetCol) return  // wrong cell

        val newBoard = s.board.mapIndexed { r, row ->
            row.mapIndexed { c, cell ->
                if (r == targetRow && c == targetCol) piece else cell
            }
        }
        val newCarousel = s.carouselPieces - piece
        _state.value = s.copy(
            board = newBoard,
            carouselPieces = newCarousel,
            isComplete = newBoard.all { row -> row.all { it != null } }
        )
    }
}
