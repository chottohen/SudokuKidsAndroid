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
                        onClick = { viewModel.newMaze(diff) },
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                val cellDp = maxWidth / state.cols

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Entrance buttons row, aligned above maze columns
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        state.entranceCols.forEachIndexed { i, col ->
                            val isSelected   = state.selectedEntrance == i
                            val isWrongShown = state.isValidated && !state.isWon && state.selectedEntrance == i
                            val bgColor = when {
                                isWrongShown -> Color(0xFFEF9A9A)
                                isSelected   -> Color(0xFFFFEE58)
                                else         -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Box(
                                modifier = Modifier
                                    .absoluteOffset(
                                        x = cellDp * col + (cellDp - 38.dp) / 2,
                                        y = 6.dp
                                    )
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFFF9A825) else Color(0xFF9E9E9E),
                                        shape = CircleShape
                                    )
                                    .clickable(enabled = !state.isValidated) {
                                        viewModel.selectEntrance(i)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    state.entranceLabels[i],
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    // Maze canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(state.cols.toFloat() / state.rows)
                    ) {
                        drawMaze(state)
                    }

                    // Exit label
                    Text(
                        "🚪 SORTIE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
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
                                onClick = { viewModel.validate() },
                                enabled = state.selectedEntrance != null
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

private fun DrawScope.drawMaze(state: MazeState) {
    val cellPx  = size.width / state.cols
    val wallClr = Color(0xFF0D47A1)   // bleu foncé profond
    val stroke  = (cellPx * 0.12f).coerceIn(2f, 5f)  // épaisseur proportionnelle à la taille des cellules

    // Fond clair
    drawRect(color = Color(0xFFFFF8E1), size = size)

    // Solution path highlight (shown only when won)
    if (state.isWon) {
        state.solutionPath.forEach { (r, c) ->
            drawRect(
                color   = Color(0xFF43A047).copy(alpha = 0.65f),
                topLeft = Offset(c * cellPx + 1f, r * cellPx + 1f),
                size    = Size(cellPx - 2f, cellPx - 2f)
            )
        }
    }

    // Selected entrance highlight
    if (!state.isValidated) {
        state.selectedEntrance?.let { i ->
            val col = state.entranceCols[i]
            drawRect(
                color   = Color(0xFFFDD835).copy(alpha = 0.6f),
                topLeft = Offset(col * cellPx, 0f),
                size    = Size(cellPx, cellPx)
            )
        }
    }

    // ── Outer border (cell-by-cell to handle openings) ───────────────────────
    for (c in 0 until state.cols) {
        if (state.grid[0][c].top)
            drawLine(wallClr, Offset(c * cellPx, 0f), Offset((c + 1) * cellPx, 0f), stroke)
        if (state.grid[state.rows - 1][c].bottom)
            drawLine(wallClr, Offset(c * cellPx, size.height), Offset((c + 1) * cellPx, size.height), stroke)
    }
    for (r in 0 until state.rows) {
        if (state.grid[r][0].left)
            drawLine(wallClr, Offset(0f, r * cellPx), Offset(0f, (r + 1) * cellPx), stroke)
        if (state.grid[r][state.cols - 1].right)
            drawLine(wallClr, Offset(size.width, r * cellPx), Offset(size.width, (r + 1) * cellPx), stroke)
    }

    // ── Internal horizontal walls ────────────────────────────────────────────
    for (r in 1 until state.rows) {
        for (c in 0 until state.cols) {
            if (state.grid[r][c].top)
                drawLine(wallClr, Offset(c * cellPx, r * cellPx), Offset((c + 1) * cellPx, r * cellPx), stroke)
        }
    }

    // ── Internal vertical walls ──────────────────────────────────────────────
    for (r in 0 until state.rows) {
        for (c in 1 until state.cols) {
            if (state.grid[r][c].left)
                drawLine(wallClr, Offset(c * cellPx, r * cellPx), Offset(c * cellPx, (r + 1) * cellPx), stroke)
        }
    }
}
