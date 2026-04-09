package com.example.sudokukidsandroid

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(
    onBack: () -> Unit,
    viewModel: PuzzleViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.loadImage(context, it) }
    }

    // ── ViewConfiguration avec long-press réduit au tiers ───────────────────
    val viewConfig = LocalViewConfiguration.current
    val fastViewConfig = remember(viewConfig) {
        object : ViewConfiguration by viewConfig {
            override val longPressTimeoutMillis: Long
                get() = viewConfig.longPressTimeoutMillis / 3
        }
    }

    // ── Drag state ──────────────────────────────────────────────────────────
    var dragPiece by remember { mutableStateOf<PuzzlePiece?>(null) }
    var dragWindowPos by remember { mutableStateOf(Offset.Zero) }
    var rootWindowPos by remember { mutableStateOf(Offset.Zero) }
    var cellSizePx by remember { mutableStateOf(0f) }
    val cellCoords = remember { mutableStateMapOf<Pair<Int, Int>, LayoutCoordinates>() }

    fun handleDragEnd() {
        val piece = dragPiece ?: return
        val target = cellCoords.entries.firstOrNull { (_, coords) ->
            val tl = coords.localToWindow(Offset.Zero)
            Rect(tl, Size(coords.size.width.toFloat(), coords.size.height.toFloat()))
                .contains(dragWindowPos)
        }?.key
        if (target != null) viewModel.tryPlacePiece(piece, target.first, target.second)
        dragPiece = null
    }

    // ── Root Box: contains Scaffold + floating drag overlay ─────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootWindowPos = it.localToWindow(Offset.Zero) }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Puzzle", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
                    },
                    actions = {
                        if (state.imageLoaded) {
                            IconButton(onClick = { launcher.launch("image/*") }) {
                                Text("📷", fontSize = 20.sp)
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator()

                    !state.imageLoaded -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("🧩", fontSize = 64.sp)
                            Text("Choisissez une photo", fontSize = 18.sp)
                            Button(onClick = { launcher.launch("image/*") }) {
                                Text("Choisir une photo")
                            }
                        }
                    }

                    state.isComplete -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Bravo ! 🎉",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Puzzle terminé !", fontSize = 20.sp)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { launcher.launch("image/*") }) {
                                Text("Nouveau puzzle")
                            }
                        }
                    }

                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {

                            // ── Puzzle board ─────────────────────────────
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                val cellSize = maxWidth / PuzzleViewModel.COLS
                                cellSizePx = with(density) { cellSize.toPx() }
                                val ratio = (PuzzleViewModel.TILE_PX + 2f * PuzzleViewModel.TAB_PX) / PuzzleViewModel.TILE_PX.toFloat()

                                // ── Grille : fond + bordures + zones de dépôt ──
                                Column(
                                    modifier = Modifier
                                        .height(cellSize * PuzzleViewModel.ROWS)
                                        .fillMaxWidth()
                                        .background(Color(0xFFE8EAF6))
                                ) {
                                    repeat(PuzzleViewModel.ROWS) { row ->
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            repeat(PuzzleViewModel.COLS) { col ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(cellSize)
                                                        .border(0.5.dp, Color(0xFF9E9E9E))
                                                        .onGloballyPositioned {
                                                            cellCoords[Pair(row, col)] = it
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                // ── Pièces : Canvas unique pour éviter les problèmes de z-order ──
                                // drawImage utilise SrcOver : les zones BLANK (alpha=0) ne couvrent
                                // pas les tabs des pièces voisines déjà dessinés.
                                Canvas(
                                    modifier = Modifier
                                        .height(cellSize * PuzzleViewModel.ROWS)
                                        .fillMaxWidth()
                                ) {
                                    val cellPx  = cellSizePx
                                    val piecePx = cellPx * ratio
                                    val tabOff  = cellPx * PuzzleViewModel.TAB_PX / PuzzleViewModel.TILE_PX.toFloat()

                                    (0 until PuzzleViewModel.ROWS).forEach { row ->
                                        (0 until PuzzleViewModel.COLS).forEach { col ->
                                            val placed = state.board[row][col] ?: return@forEach
                                            drawImage(
                                                image     = placed.bitmap,
                                                srcOffset = IntOffset.Zero,
                                                srcSize   = IntSize(placed.bitmap.width, placed.bitmap.height),
                                                dstOffset = IntOffset(
                                                    (col * cellPx - tabOff).roundToInt(),
                                                    (row * cellPx - tabOff).roundToInt()
                                                ),
                                                dstSize = IntSize(piecePx.roundToInt(), piecePx.roundToInt())
                                            )
                                        }
                                    }
                                }

                                // ── Miniature en haut à droite ───────────
                                state.thumbnail?.let { thumb ->
                                    val thumbW = cellSize * 2
                                    val thumbH = cellSize * (PuzzleViewModel.ROWS.toFloat() / PuzzleViewModel.COLS * 2)
                                    Image(
                                        bitmap = thumb,
                                        contentDescription = "Aperçu",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(width = thumbW, height = thumbH)
                                            .border(1.5.dp, Color.White)
                                            .shadow(4.dp)
                                            .alpha(0.85f)
                                    )
                                }
                            }

                            // ── Carousel ─────────────────────────────────
                            HorizontalDivider()
                            Text(
                                "Maintenez une pièce pour la déplacer",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            CompositionLocalProvider(LocalViewConfiguration provides fastViewConfig) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(state.carouselPieces, key = { it.id }) { piece ->
                                    var itemCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                                    var curWindowPos by remember { mutableStateOf(Offset.Zero) }
                                    val isDragging = dragPiece?.id == piece.id

                                    // Box sized to the full piece bitmap (tile + 2×tab margins)
                                    val fullPieceDp = 72.dp * (PuzzleViewModel.TILE_PX + 2f * PuzzleViewModel.TAB_PX) / PuzzleViewModel.TILE_PX
                                    Box(
                                        modifier = Modifier
                                            .size(fullPieceDp)
                                            .background(Color(0xFFF0F4F8), shape = MaterialTheme.shapes.small)
                                            .border(
                                                1.dp,
                                                Color(0xFF90A4AE),
                                                shape = MaterialTheme.shapes.small
                                            )
                                            .alpha(if (isDragging) 0.2f else 1f)
                                            .onGloballyPositioned { itemCoords = it }
                                            .pointerInput(piece.id) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { localOffset ->
                                                        val wp = itemCoords?.localToWindow(localOffset)
                                                            ?: localOffset
                                                        curWindowPos = wp
                                                        dragWindowPos = wp
                                                        dragPiece = piece
                                                    },
                                                    onDrag = { change, delta ->
                                                        change.consume()
                                                        curWindowPos += delta
                                                        dragWindowPos = curWindowPos
                                                    },
                                                    onDragEnd = { handleDragEnd() },
                                                    onDragCancel = { dragPiece = null }
                                                )
                                            }
                                    ) {
                                        Image(
                                            bitmap = piece.bitmap,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                            } // end CompositionLocalProvider
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // ── Floating drag overlay (above everything, including TopAppBar) ──
        dragPiece?.let { piece ->
            val localPos = dragWindowPos - rootWindowPos
            val sizeDp = with(density) { cellSizePx.toDp() }.coerceAtLeast(60.dp)
            Image(
                bitmap = piece.bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (localPos.x - cellSizePx / 2).roundToInt(),
                            (localPos.y - cellSizePx / 2).roundToInt()
                        )
                    }
                    .size(sizeDp)
                    .shadow(8.dp, shape = MaterialTheme.shapes.small)
                    .zIndex(100f)
                    .alpha(0.92f)
            )
        }
    }
}
