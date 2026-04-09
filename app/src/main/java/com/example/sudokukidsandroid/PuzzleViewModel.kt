package com.example.sudokukidsandroid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Path
import android.graphics.RectF
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
import kotlin.random.Random

class PuzzleViewModel : ViewModel() {

    private val _state = MutableStateFlow(PuzzleState())
    val state: StateFlow<PuzzleState> = _state.asStateFlow()

    companion object {
        const val COLS = 6
        const val ROWS = 4
        const val TILE_PX = 120          // tile content size
        const val TAB_PX  = 18           // connector protrusion beyond tile boundary
        // Full piece bitmap = (TILE_PX + 2*TAB_PX)² = 156 × 156
    }

    // hEdge[row][col] = connector on the RIGHT of (row,col) / LEFT of (row,col+1) is complementary
    // vEdge[row][col] = connector on the BOTTOM of (row,col) / TOP of (row+1,col) is complementary
    private fun buildEdgeGrid(rows: Int, cols: Int): Array<Array<PieceEdges>> {
        val hEdge = Array(rows) { Array(cols - 1) { if (Random.nextBoolean()) EdgeType.TAB else EdgeType.BLANK } }
        val vEdge = Array(rows - 1) { Array(cols) { if (Random.nextBoolean()) EdgeType.TAB else EdgeType.BLANK } }

        return Array(rows) { row ->
            Array(cols) { col ->
                PieceEdges(
                    top    = if (row == 0)        EdgeType.FLAT
                             else if (vEdge[row - 1][col] == EdgeType.TAB) EdgeType.BLANK else EdgeType.TAB,
                    right  = if (col == cols - 1) EdgeType.FLAT else hEdge[row][col],
                    bottom = if (row == rows - 1) EdgeType.FLAT else vEdge[row][col],
                    left   = if (col == 0)        EdgeType.FLAT
                             else if (hEdge[row][col - 1] == EdgeType.TAB) EdgeType.BLANK else EdgeType.TAB
                )
            }
        }
    }

    /**
     * Builds a puzzle-shaped bitmap from a rectangular tile.
     *
     * The result is (TILE + 2×TAB) × (TILE + 2×TAB).  The tile image fills the
     * full extended area (scaled slightly).  A Path clips it to the puzzle shape:
     *
     *   TAB   – convex semicircle protruding OUTWARD (into the TAB_PX margin)
     *   BLANK – concave semicircle cut INWARD (into the tile area)
     *   FLAT  – straight edge (outer border of the puzzle → border-piece marker)
     *
     * Arc convention (Android Canvas, y-axis pointing DOWN):
     *   0°→right, 90°→down, 180°→left, 270°→up
     *
     * For each edge the arc oval has its centre ON the tile boundary and radius = TAB_PX.
     * TAB sweep is -180° (passes through 270° = UP/LEFT/etc., the outward direction).
     * BLANK sweep is +180° (passes through 90°/0°/etc., the inward direction).
     */
    private fun shapePiece(src: Bitmap, edges: PieceEdges): Bitmap {
        val t  = TAB_PX.toFloat()
        val bw = src.width  + 2 * TAB_PX  // = TILE_PX + 2*TAB_PX
        val bh = src.height + 2 * TAB_PX

        // Tile boundary within the extended bitmap
        val L  = t
        val T  = t
        val R  = t + src.width
        val B  = t + src.height
        val mX = (L + R) / 2f
        val mY = (T + B) / 2f

        val path = Path()
        path.moveTo(L, T)

        // ── Top edge: L,T → R,T ──────────────────────────────────────────────────
        when (edges.top) {
            EdgeType.FLAT  -> path.lineTo(R, T)
            EdgeType.TAB   -> {
                path.lineTo(mX - t, T)
                // oval centre (mX, T); start 180°=(mX-t,T); sweep +180° CW passes 270°=(mX,T-t) [UP = outward]
                path.arcTo(RectF(mX - t, T - t, mX + t, T + t), 180f, 180f)
                path.lineTo(R, T)
            }
            EdgeType.BLANK -> {
                path.lineTo(mX - t, T)
                // sweep -180° CCW passes 90°=(mX,T+t) [DOWN = inward]
                path.arcTo(RectF(mX - t, T - t, mX + t, T + t), 180f, -180f)
                path.lineTo(R, T)
            }
        }

        // ── Right edge: R,T → R,B ────────────────────────────────────────────────
        when (edges.right) {
            EdgeType.FLAT  -> path.lineTo(R, B)
            EdgeType.TAB   -> {
                path.lineTo(R, mY - t)
                // oval centre (R, mY); 270°→(R, mY-t); sweep +180° passes 0°=(R+t, mY) [RIGHT = outward]
                path.arcTo(RectF(R - t, mY - t, R + t, mY + t), 270f, 180f)
                path.lineTo(R, B)
            }
            EdgeType.BLANK -> {
                path.lineTo(R, mY - t)
                // sweep -180° passes 180°=(R-t, mY) [LEFT = inward]
                path.arcTo(RectF(R - t, mY - t, R + t, mY + t), 270f, -180f)
                path.lineTo(R, B)
            }
        }

        // ── Bottom edge: R,B → L,B ───────────────────────────────────────────────
        when (edges.bottom) {
            EdgeType.FLAT  -> path.lineTo(L, B)
            EdgeType.TAB   -> {
                path.lineTo(mX + t, B)
                // oval centre (mX, B); 0°→(mX+t, B); sweep +180° passes 90°=(mX, B+t) [DOWN = outward]
                path.arcTo(RectF(mX - t, B - t, mX + t, B + t), 0f, 180f)
                path.lineTo(L, B)
            }
            EdgeType.BLANK -> {
                path.lineTo(mX + t, B)
                // sweep -180° passes 270°=(mX, B-t) [UP = inward]
                path.arcTo(RectF(mX - t, B - t, mX + t, B + t), 0f, -180f)
                path.lineTo(L, B)
            }
        }

        // ── Left edge: L,B → L,T ─────────────────────────────────────────────────
        when (edges.left) {
            EdgeType.FLAT  -> path.lineTo(L, T)
            EdgeType.TAB   -> {
                path.lineTo(L, mY + t)
                // oval centre (L, mY); 90°→(L, mY+t); sweep +180° passes 180°=(L-t, mY) [LEFT = outward]
                path.arcTo(RectF(L - t, mY - t, L + t, mY + t), 90f, 180f)
                path.lineTo(L, T)
            }
            EdgeType.BLANK -> {
                path.lineTo(L, mY + t)
                // sweep -180° passes 0°=(L+t, mY) [RIGHT = inward]
                path.arcTo(RectF(L - t, mY - t, L + t, mY + t), 90f, -180f)
                path.lineTo(L, T)
            }
        }

        path.close()

        // Scale source to fill the extended bitmap (slight zoom-out, barely perceptible)
        val fullScaled = Bitmap.createScaledBitmap(src, bw, bh, true)

        val result = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(fullScaled, 0f, 0f, null)
        canvas.restore()

        return result
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
            val bitmap = raw.copy(Bitmap.Config.ARGB_8888, false)

            val targetW = COLS * TILE_PX   // 720
            val targetH = ROWS * TILE_PX   // 480
            val scaled  = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)

            val edgeGrid = buildEdgeGrid(ROWS, COLS)

            val pieces = (0 until ROWS).flatMap { row ->
                (0 until COLS).map { col ->
                    val rawTile = Bitmap.createBitmap(scaled, col * TILE_PX, row * TILE_PX, TILE_PX, TILE_PX)
                        .copy(Bitmap.Config.ARGB_8888, false)
                    val edges = edgeGrid[row][col]
                    PuzzlePiece(
                        id         = row * COLS + col,
                        correctRow = row,
                        correctCol = col,
                        bitmap     = shapePiece(rawTile, edges).asImageBitmap(),
                        edges      = edges
                    )
                }
            }

            val thumbnail = Bitmap.createScaledBitmap(bitmap, COLS * 20, ROWS * 20, true)
                .copy(Bitmap.Config.ARGB_8888, false)
                .asImageBitmap()

            withContext(Dispatchers.Main) {
                _state.value = PuzzleState(
                    pieces        = pieces,
                    carouselPieces = pieces.shuffled(),
                    thumbnail     = thumbnail,
                    imageLoaded   = true
                )
            }
        }
    }

    fun tryPlacePiece(piece: PuzzlePiece, targetRow: Int, targetCol: Int) {
        val s = _state.value
        if (piece !in s.carouselPieces) return
        if (s.board[targetRow][targetCol] != null) return
        if (piece.correctRow != targetRow || piece.correctCol != targetCol) return

        val newBoard = s.board.mapIndexed { r, row ->
            row.mapIndexed { c, cell ->
                if (r == targetRow && c == targetCol) piece else cell
            }
        }
        val newCarousel = s.carouselPieces - piece
        _state.value = s.copy(
            board      = newBoard,
            carouselPieces = newCarousel,
            isComplete = newBoard.all { row -> row.all { it != null } }
        )
    }
}
