package com.example.sudokukidsandroid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MazeScreen(
    onBack: () -> Unit,
    viewModel: MazeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Labyrinthe", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
                },
                actions = {
                    IconButton(onClick = { viewModel.newMaze() }) {
                        Text("🔄", fontSize = 20.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Difficulty selector ──────────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Difficulty.entries.forEach { diff ->
                    FilterChip(
                        selected = state.difficulty == diff,
                        onClick  = { viewModel.newMaze(diff) },
                        label = {
                            Text(when (diff) {
                                Difficulty.EASY   -> "Facile"
                                Difficulty.MEDIUM -> "Moyen"
                                Difficulty.HARD   -> "Difficile"
                            })
                        }
                    )
                }
            }

            // ── Maze area ────────────────────────────────────────────────────
            // Layout:
            //   [44dp left pad]  [maze canvas]
            //   Entrance buttons on left (wallDir=3) and above (wallDir=0)
            //   Exit emoji below the exit cell (bottom-right)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopStart
            ) {
                val leftPad: Dp = 44.dp
                val topPad:  Dp = 44.dp
                val mazeW        = maxWidth - leftPad
                val cellDp       = mazeW / state.cols
                val mazeH        = cellDp * state.rows

                // Maze canvas
                Canvas(
                    modifier = Modifier
                        .absoluteOffset(x = leftPad, y = topPad)
                        .width(mazeW)
                        .height(mazeH)
                ) { drawMaze(state) }

                // Entrance buttons (top or left)
                state.entrances.forEachIndexed { i, e ->
                    val isSelected = state.selectedEntrance == i
                    val isWrong    = state.isValidated && !state.isWon && state.selectedEntrance == i
                    val bx: Dp
                    val by: Dp
                    if (e.wallDir == 0) {           // top entrance
                        bx = leftPad + cellDp * e.col.toFloat() + (cellDp - 38.dp) / 2
                        by = (topPad - 38.dp) / 2
                    } else {                         // left entrance (wallDir == 3)
                        bx = (leftPad - 38.dp) / 2
                        by = topPad + cellDp * e.row.toFloat() + (cellDp - 38.dp) / 2
                    }
                    EntranceButton(
                        label    = state.entranceLabels[i],
                        selected = isSelected,
                        isWrong  = isWrong,
                        enabled  = !state.isValidated,
                        modifier = Modifier.absoluteOffset(x = bx, y = by),
                        onClick  = { viewModel.selectEntrance(i) }
                    )
                }

                // Exit emoji below exit cell
                Text(
                    "🚪",
                    fontSize = 18.sp,
                    modifier = Modifier.absoluteOffset(
                        x = leftPad + cellDp * state.exitCol.toFloat() + (cellDp - 24.dp) / 2,
                        y = topPad + mazeH + 2.dp
                    )
                )
            }

            // ── Bottom controls ───────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    state.isWon -> {
                        Text(
                            "Gagné ! 🎉",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            "L'entrée ${state.entranceLabels[state.winningEntrance]} mène à la sortie.",
                            fontSize = 14.sp
                        )
                        Button(onClick = { viewModel.newMaze() }) {
                            Text("Nouveau labyrinthe")
                        }
                    }
                    state.isValidated && !state.isWon -> {
                        Text(
                            "Ce n'est pas le bon chemin… 😕",
                            fontSize = 16.sp,
                            color = Color(0xFFC62828)
                        )
                        Button(onClick = { viewModel.retry() }) {
                            Text("Réessayer")
                        }
                    }
                    else -> {
                        if (state.selectedEntrance == null) {
                            Text(
                                "Choisis une entrée (A, B, C…)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { viewModel.newMaze() }) {
                                Text("Nouveau")
                            }
                            Button(
                                onClick  = { viewModel.validate() },
                                enabled  = state.selectedEntrance != null
                            ) {
                                Text("Valider")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntranceButton(
    label: String,
    selected: Boolean,
    isWrong: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bg = when {
        isWrong  -> Color(0xFFEF9A9A)
        selected -> Color(0xFFFFEE58)
        else     -> MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bg)
            .border(
                width  = if (selected) 2.dp else 1.dp,
                color  = if (selected) Color(0xFFF9A825) else Color(0xFF9E9E9E),
                shape  = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

private fun DrawScope.drawMaze(state: MazeState) {
    val cellPx  = size.width / state.cols
    val wallClr = Color(0xFF0D47A1)
    val stroke  = (cellPx * 0.12f).coerceIn(2f, 5f)

    // Light background
    drawRect(color = Color(0xFFFFF8E1), size = size)

    // Solution path (green cells, shown only on win)
    if (state.isWon) {
        state.solutionPath.forEach { (r, c) ->
            drawRect(
                color   = Color(0xFF43A047).copy(alpha = 0.65f),
                topLeft = Offset(c * cellPx + 1f, r * cellPx + 1f),
                size    = Size(cellPx - 2f, cellPx - 2f)
            )
        }
    }

    // Selected entrance cell highlight
    if (!state.isValidated) {
        state.selectedEntrance?.let { i ->
            val e = state.entrances[i]
            drawRect(
                color   = Color(0xFFFDD835).copy(alpha = 0.6f),
                topLeft = Offset(e.col * cellPx, e.row * cellPx),
                size    = Size(cellPx, cellPx)
            )
        }
    }

    // Exit cell highlight
    drawRect(
        color   = Color(0xFFEF9A9A).copy(alpha = 0.5f),
        topLeft = Offset(state.exitCol * cellPx + 1f, state.exitRow * cellPx + 1f),
        size    = Size(cellPx - 2f, cellPx - 2f)
    )

    // ── Outer border (cell-by-cell to leave openings) ────────────────────────
    for (c in 0 until state.cols) {
        if (state.grid[0][c].top)
            drawLine(wallClr, Offset(c * cellPx, 0f), Offset((c + 1) * cellPx, 0f), stroke)
        if (state.grid[state.rows - 1][c].bottom)
            drawLine(wallClr, Offset(c * cellPx, size.height),
                Offset((c + 1) * cellPx, size.height), stroke)
    }
    for (r in 0 until state.rows) {
        if (state.grid[r][0].left)
            drawLine(wallClr, Offset(0f, r * cellPx), Offset(0f, (r + 1) * cellPx), stroke)
        if (state.grid[r][state.cols - 1].right)
            drawLine(wallClr, Offset(size.width, r * cellPx),
                Offset(size.width, (r + 1) * cellPx), stroke)
    }

    // ── Internal horizontal walls ────────────────────────────────────────────
    for (r in 1 until state.rows) {
        for (c in 0 until state.cols) {
            if (state.grid[r][c].top)
                drawLine(wallClr, Offset(c * cellPx, r * cellPx),
                    Offset((c + 1) * cellPx, r * cellPx), stroke)
        }
    }

    // ── Internal vertical walls ──────────────────────────────────────────────
    for (r in 0 until state.rows) {
        for (c in 1 until state.cols) {
            if (state.grid[r][c].left)
                drawLine(wallClr, Offset(c * cellPx, r * cellPx),
                    Offset(c * cellPx, (r + 1) * cellPx), stroke)
        }
    }
}
