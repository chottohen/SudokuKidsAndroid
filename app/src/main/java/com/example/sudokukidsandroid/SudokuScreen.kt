package com.example.sudokukidsandroid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SudokuScreen(
    modifier: Modifier = Modifier,
    viewModel: SudokuViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val animals = if (state.size == 4) ANIMALS_4 else ANIMALS_9

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sudoku Animaux",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.size == 4,
                onClick = { viewModel.newGame(4) },
                label = { Text("4×4") }
            )
            FilterChip(
                selected = state.size == 9,
                onClick = { viewModel.newGame(9) },
                label = { Text("9×9") }
            )
        }

        if (state.solution.isNotEmpty()) {
            SudokuGrid(
                state = state,
                animals = animals,
                onCellClick = { row, col -> viewModel.selectCell(row, col) }
            )
        }

        AnimalPicker(
            animals = animals,
            onAnimalSelected = { viewModel.placeAnimal(it) },
            onClear = { viewModel.clearSelectedCell() }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.checkGrid() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Vérifier")
            }
            Button(
                onClick = { viewModel.resetGrid() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color.Black
                )
            ) {
                Text("Reset")
            }
            Button(
                onClick = { viewModel.newGame() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("Nouveau")
            }
        }

        if (state.isSuccess) {
            Text(
                text = "Bravo! 🎉",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SudokuGrid(
    state: SudokuState,
    animals: List<String>,
    onCellClick: (Int, Int) -> Unit
) {
    val boxSize = if (state.size == 4) 2 else 3

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = maxWidth / state.size

        Box(
            modifier = Modifier
                .size(maxWidth)
                .drawBehind {
                    drawRect(color = Color.DarkGray, style = Stroke(width = 3.dp.toPx()))
                }
        ) {
            Column {
                for (row in 0 until state.size) {
                    Row {
                        for (col in 0 until state.size) {
                            SudokuCell(
                                value = state.userGrid[row][col],
                                isGiven = state.givens[row][col],
                                isSelected = state.selectedCell == Pair(row, col),
                                isError = Pair(row, col) in state.errorCells,
                                animals = animals,
                                cellSize = cellSize,
                                row = row,
                                col = col,
                                gridSize = state.size,
                                boxSize = boxSize,
                                onClick = { onCellClick(row, col) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SudokuCell(
    value: Int,
    isGiven: Boolean,
    isSelected: Boolean,
    isError: Boolean,
    animals: List<String>,
    cellSize: Dp,
    row: Int,
    col: Int,
    gridSize: Int,
    boxSize: Int,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> Color(0xFFFFF9C4)
        isError -> Color(0xFFFFCDD2)
        isGiven -> Color(0xFFEEEEEE)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .background(bgColor)
            .drawWithContent {
                drawContent()
                if (isError) {
                    drawRect(color = Color(0xFFD32F2F), style = Stroke(width = 2.dp.toPx()))
                }
                if (col < gridSize - 1) {
                    val isBoxBoundary = (col + 1) % boxSize == 0
                    drawLine(
                        color = Color.DarkGray,
                        start = Offset(size.width, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = if (isBoxBoundary) 4.dp.toPx() else 1.5.dp.toPx()
                    )
                }
                if (row < gridSize - 1) {
                    val isBoxBoundary = (row + 1) % boxSize == 0
                    drawLine(
                        color = Color.DarkGray,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = if (isBoxBoundary) 4.dp.toPx() else 1.5.dp.toPx()
                    )
                }
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) {
            Text(
                text = animals[value - 1],
                fontSize = (cellSize.value * 0.55f).sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AnimalPicker(
    animals: List<String>,
    onAnimalSelected: (Int) -> Unit,
    onClear: () -> Unit
) {
    val clearButton: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .height(64.dp)
                .background(Color(0xFFFFEBEE), shape = MaterialTheme.shapes.medium)
                .clickable { onClear() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✕ Effacer", fontSize = 16.sp)
        }
    }

    if (animals.size <= 4) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            animals.forEachIndexed { index, animal ->
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFE3F2FD), shape = MaterialTheme.shapes.medium)
                        .clickable { onAnimalSelected(index + 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = animal, fontSize = 32.sp)
                }
            }
            clearButton()
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                animals.subList(0, 5).forEachIndexed { index, animal ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFE3F2FD), shape = MaterialTheme.shapes.medium)
                            .clickable { onAnimalSelected(index + 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = animal, fontSize = 32.sp)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                animals.subList(5, 9).forEachIndexed { index, animal ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFE3F2FD), shape = MaterialTheme.shapes.medium)
                            .clickable { onAnimalSelected(index + 6) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = animal, fontSize = 32.sp)
                    }
                }
                clearButton()
            }
        }
    }
}
