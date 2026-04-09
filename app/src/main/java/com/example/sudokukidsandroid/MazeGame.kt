package com.example.sudokukidsandroid

import kotlin.random.Random

enum class Difficulty { EASY, MEDIUM, HARD }

data class WallSet(
    val top: Boolean = true,
    val right: Boolean = true,
    val bottom: Boolean = true,
    val left: Boolean = true
)

data class MazeState(
    val rows: Int,
    val cols: Int,
    val grid: List<List<WallSet>>,
    val entranceCols: List<Int>,        // column of each entrance on row 0
    val exitCol: Int,                    // column of exit on row rows-1
    val winningEntrance: Int,           // 0-based index into entranceCols
    val solutionPath: List<Pair<Int, Int>>,
    val selectedEntrance: Int? = null,
    val isValidated: Boolean = false,
    val difficulty: Difficulty = Difficulty.EASY
) {
    val nEntrances: Int get() = entranceCols.size
    val entranceLabels: List<String> get() = (0 until nEntrances).map { ('A' + it).toString() }
    val isWon: Boolean get() = isValidated && selectedEntrance == winningEntrance
}

object MazeGenerator {
    private val DR  = intArrayOf(-1, 0, 1, 0)   // top, right, bottom, left
    private val DC  = intArrayOf(0, 1, 0, -1)
    private val OPP = intArrayOf(2, 3, 0, 1)

    fun generate(difficulty: Difficulty): MazeState {
        val (rows, cols, n) = when (difficulty) {
            Difficulty.EASY   -> Triple(9,  9,  3)
            Difficulty.MEDIUM -> Triple(13, 13, 4)
            Difficulty.HARD   -> Triple(17, 17, 5)
        }
        val exitRow = rows - 1
        val exitCol = cols / 2
        val entranceCols = (0 until n).map { i -> (i + 1) * cols / (n + 1) }

        // walls[r][c][dir] = true → wall is present. dir: 0=top 1=right 2=bottom 3=left
        val walls = Array(rows) { Array(cols) { BooleanArray(4) { true } } }

        // DFS from exit to build a spanning tree (perfect maze).
        // Constraint: no horizontal connections along row 0 so each entrance cell
        // is a leaf, which guarantees no entrance is on another entrance's path.
        val visited = Array(rows) { BooleanArray(cols) }
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.add(exitRow to exitCol)
        visited[exitRow][exitCol] = true

        while (stack.isNotEmpty()) {
            val (r, c) = stack.last()
            val dirs = (0..3).filter { d ->
                val nr = r + DR[d]; val nc = c + DC[d]
                if (r == 0 && nr == 0) return@filter false   // no row-0 horizontal links
                nr in 0 until rows && nc in 0 until cols && !visited[nr][nc]
            }.shuffled()
            if (dirs.isEmpty()) { stack.removeLast(); continue }
            val d = dirs.first()
            val nr = r + DR[d]; val nc = c + DC[d]
            walls[r][c][d] = false
            walls[nr][nc][OPP[d]] = false
            visited[nr][nc] = true
            stack.add(nr to nc)
        }

        // Open outer gaps: top wall of each entrance cell, bottom wall of exit cell.
        for (col in entranceCols) walls[0][col][0] = false
        walls[exitRow][exitCol][2] = false

        // Pick winner randomly, compute its path, then cut losers at their merge point.
        val winnerIdx = Random.nextInt(n)
        val exit = exitRow to exitCol
        val winnerPath = bfs(walls, rows, cols, 0 to entranceCols[winnerIdx], exit)!!
        val winnerSet  = winnerPath.toSet()

        for (i in 0 until n) {
            if (i == winnerIdx) continue
            val lp = bfs(walls, rows, cols, 0 to entranceCols[i], exit) ?: continue
            for (k in 1 until lp.size) {
                if (lp[k] in winnerSet) {
                    val (r1, c1) = lp[k - 1]; val (r2, c2) = lp[k]
                    val d = dir(r1, c1, r2, c2)
                    walls[r1][c1][d] = true
                    walls[r2][c2][OPP[d]] = true
                    break
                }
            }
        }

        val grid = (0 until rows).map { r ->
            (0 until cols).map { c ->
                WallSet(walls[r][c][0], walls[r][c][1], walls[r][c][2], walls[r][c][3])
            }
        }
        return MazeState(rows, cols, grid, entranceCols, exitCol, winnerIdx, winnerPath,
            difficulty = difficulty)
    }

    private fun dir(r1: Int, c1: Int, r2: Int, c2: Int) = when {
        r2 < r1 -> 0
        c2 > c1 -> 1
        r2 > r1 -> 2
        else    -> 3
    }

    private fun bfs(
        walls: Array<Array<BooleanArray>>,
        rows: Int, cols: Int,
        from: Pair<Int, Int>,
        to: Pair<Int, Int>
    ): List<Pair<Int, Int>>? {
        val queue = ArrayDeque<Pair<Int, Int>>()
        val parent = HashMap<Pair<Int, Int>, Pair<Int, Int>?>()
        queue.add(from); parent[from] = null
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (cur == to) {
                val path = mutableListOf<Pair<Int, Int>>()
                var node: Pair<Int, Int>? = cur
                while (node != null) { path.add(0, node); node = parent[node] }
                return path
            }
            val (r, c) = cur
            for (d in 0..3) {
                if (walls[r][c][d]) continue
                val nr = r + DR[d]; val nc = c + DC[d]
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                val nb = nr to nc
                if (nb in parent) continue
                parent[nb] = cur; queue.add(nb)
            }
        }
        return null
    }
}
