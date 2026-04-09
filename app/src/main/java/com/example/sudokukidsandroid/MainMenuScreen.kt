package com.example.sudokukidsandroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onPlaySudoku: () -> Unit,
    onPlayPuzzle: () -> Unit,
    onPlayMaze: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Jeux", fontWeight = FontWeight.Bold) })
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                GameCard(
                    emoji = "🔢",
                    title = "Sudoku",
                    subtitle = "Animaux & Chiffres",
                    onClick = onPlaySudoku
                )
                GameCard(
                    emoji = "🧩",
                    title = "Puzzle",
                    subtitle = "Photo à reconstituer",
                    onClick = onPlayPuzzle
                )
                GameCard(
                    emoji = "🌀",
                    title = "Labyrinthe",
                    subtitle = "Trouve le bon chemin !",
                    onClick = onPlayMaze
                )
            }
        }
    }
}

@Composable
private fun GameCard(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(emoji, fontSize = 44.sp)
            Column {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
