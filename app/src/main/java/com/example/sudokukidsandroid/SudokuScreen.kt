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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuScreen(
    modifier: Modifier = Modifier,
    viewModel: SudokuViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val symbols = symbolsFor(state.theme, state.size)
    val context = LocalContext.current
    val instrumentPlayer = remember { InstrumentPlayer(context) }
    DisposableEffect(Unit) { onDispose { instrumentPlayer.release() } }
    var menuExpanded by remember { mutableStateOf(false) }
    var menuPage by remember { mutableStateOf("main") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sudoku Animaux", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Text("☰", fontSize = 22.sp)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = {
                                menuExpanded = false
                                menuPage = "main"
                            }
                        ) {
                            when (menuPage) {
                                "main" -> {
                                    DropdownMenuItem(
                                        text = { Text("🎨 Thèmes") },
                                        trailingIcon = { Text("▶", fontSize = 12.sp) },
                                        onClick = { menuPage = "themes" }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("⚙️ Paramètres") },
                                        trailingIcon = { Text("▶", fontSize = 12.sp) },
                                        onClick = { menuPage = "settings" }
                                    )
                                }
                                "themes" -> {
                                    DropdownMenuItem(
                                        text = { Text("← Thèmes", fontWeight = FontWeight.Bold) },
                                        onClick = { menuPage = "main" }
                                    )
                                    listOf(
                                        Theme.ANIMALS to "🎨 Animaux",
                                        Theme.SAFARI  to "🌿 Safari",
                                        Theme.NUMBERS to "🔢 Chiffres",
                                        Theme.MUSIC   to "🎵 Musique"
                                    ).forEach { (theme, label) ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    label,
                                                    fontWeight = if (state.theme == theme) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            trailingIcon = {
                                                if (state.theme == theme) Text("✓")
                                            },
                                            onClick = {
                                                viewModel.setTheme(theme)
                                                menuExpanded = false
                                                menuPage = "main"
                                            }
                                        )
                                    }
                                }
                                "settings" -> {
                                    DropdownMenuItem(
                                        text = { Text("← Paramètres", fontWeight = FontWeight.Bold) },
                                        onClick = { menuPage = "main" }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Aide (choix possibles)") },
                                        trailingIcon = {
                                            Text(if (state.hintsEnabled) "✓" else "")
                                        },
                                        onClick = { viewModel.toggleHints() }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

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
                symbols = symbols,
                onCellClick = { row, col ->
                    if (state.theme == Theme.MUSIC) {
                        val v = state.userGrid[row][col]
                        if (v != 0) instrumentPlayer.play(v)
                    }
                    viewModel.selectCell(row, col)
                }
            )
        }

        val validValues: Set<Int>? = if (!state.hintsEnabled) null else state.selectedCell?.let { (row, col) ->
            val size = state.size
            val boxSize = if (size == 4) 2 else 3
            val used = mutableSetOf<Int>()
            for (c in 0 until size) used.add(state.userGrid[row][c])
            for (r in 0 until size) used.add(state.userGrid[r][col])
            val boxRow = (row / boxSize) * boxSize
            val boxCol = (col / boxSize) * boxSize
            for (r in boxRow until boxRow + boxSize)
                for (c in boxCol until boxCol + boxSize)
                    used.add(state.userGrid[r][c])
            used.remove(0)
            (1..size).toSet() - used
        }

        AnimalPicker(
            symbols = symbols,
            isNumbers = state.theme == Theme.NUMBERS,
            validValues = validValues,
            onAnimalSelected = { value ->
                if (state.theme == Theme.MUSIC) instrumentPlayer.play(value)
                viewModel.placeAnimal(value)
            },
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
    } // end Scaffold
}

@Composable
fun SudokuGrid(
    state: SudokuState,
    symbols: List<String>,
    onCellClick: (Int, Int) -> Unit
) {
    val boxSize = if (state.size == 4) 2 else 3

    val borderTargetColor = when {
        state.isSuccess -> Color(0xFF4CAF50)
        state.gridFlashError -> Color(0xFFD32F2F)
        else -> Color.DarkGray
    }
    val borderColor by animateColorAsState(
        targetValue = borderTargetColor,
        animationSpec = tween(durationMillis = 400),
        label = "gridBorder"
    )
    val borderWidth = if (state.isSuccess || state.gridFlashError) 6.dp else 3.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = maxWidth / state.size

        Box(
            modifier = Modifier
                .size(maxWidth)
                .drawBehind {
                    drawRect(color = borderColor, style = Stroke(width = borderWidth.toPx()))
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
                                symbols = symbols,
                                isNumbers = state.theme == Theme.NUMBERS,
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
    symbols: List<String>,
    isNumbers: Boolean,
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
                text = symbols[value - 1],
                fontSize = if (isNumbers) (cellSize.value * 0.5f).sp else (cellSize.value * 0.55f).sp,
                fontWeight = if (isNumbers) FontWeight.Bold else FontWeight.Normal,
                color = if (isNumbers) Color(0xFF1A1A1A) else Color.Unspecified,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AnimalPicker(
    symbols: List<String>,
    isNumbers: Boolean,
    validValues: Set<Int>?,
    onAnimalSelected: (Int) -> Unit,
    onClear: () -> Unit
) {
    val symbolFontSize = if (isNumbers) 26.sp else 32.sp
    val symbolFontWeight = if (isNumbers) FontWeight.Bold else FontWeight.Normal

    val clearButton: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFFFEBEE), shape = MaterialTheme.shapes.medium)
                .clickable { onClear() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✕", fontSize = 28.sp)
        }
    }

    @Composable
    fun SymbolBox(symbol: String, value: Int, onClick: () -> Unit) {
        val enabled = validValues == null || value in validValues
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFE3F2FD), shape = MaterialTheme.shapes.medium)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                fontSize = symbolFontSize,
                fontWeight = symbolFontWeight,
                color = if (isNumbers) Color(0xFF1A1A1A) else Color.Unspecified
            )
            if (!enabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Color(0x99000000),
                            shape = MaterialTheme.shapes.medium
                        )
                )
            }
        }
    }

    if (symbols.size <= 4) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            symbols.forEachIndexed { index, symbol ->
                SymbolBox(symbol, index + 1) { onAnimalSelected(index + 1) }
            }
            clearButton()
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                symbols.subList(0, 5).forEachIndexed { index, symbol ->
                    SymbolBox(symbol, index + 1) { onAnimalSelected(index + 1) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                symbols.subList(5, 9).forEachIndexed { index, symbol ->
                    SymbolBox(symbol, index + 6) { onAnimalSelected(index + 6) }
                }
                clearButton()
            }
        }
    }
}
