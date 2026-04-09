package com.example.sudokukidsandroid

import kotlin.random.Random

enum class Difficulty { EASY, MEDIUM, HARD }

data class WallSet(
    val top: Boolean = true,
    val right: Boolean = true,
    val bottom: Boolean = true,
    val left: Boolean = true
)

/**
 * wallDir: 0 = entrance from top (open top wall),
 *          3 = entrance from left (open left wall)
 */
data class EntranceCell(val row: Int, val col: Int, val wallDir: Int)

data class MazeState(
    val rows: Int,
    val cols: Int,
    val grid: List<List<WallSet>>,
    val entrances: List<EntranceCell>,
    val exitRow: Int,
    val exitCol: Int,
    val winningEntrance: Int,
    val solutionPath: List<Pair<Int, Int>>,
    val selectedEntrance: Int? = null,
    val isValidated: Boolean = false,
    val difficulty: Difficulty = Difficulty.EASY
) {
    val nEntrances: Int get() = entrances.size
    val entranceLabels: List<String> get() = (0 until nEntrances).map { ('A' + it).toString() }
    val isWon: Boolean get() = isValidated && selectedEntrance == winningEntrance
}

object MazeGenerator {
    private val DR  = intArrayOf(-1, 0, 1, 0)   // top, right, bottom, left
    private val DC  = intArrayOf(0, 1, 0, -1)
    private val OPP = intArrayOf(2, 3, 0, 1)

    fun generate(difficulty: Difficulty): MazeState {
        val rows: Int; val cols: Int; val n: Int; val minLen: Int
        when (difficulty) {
            Difficulty.EASY   -> { rows = 9;  cols = 9;  n = 3; minLen = 5  }
            Difficulty.MEDIUM -> { rows = 13; cols = 13; n = 4; minLen = 10 }
            Difficulty.HARD   -> { rows = 17; cols = 17; n = 5; minLen = 20 }
        }

        // Exit: bottom-right area
        val exitRow = rows - 1
        val exitCol = cols - 1

        // Distribute entrances: ceil(n/2) on top, floor(n/2) on left
        // Positions are evenly spaced, avoiding corners and exit area.
        val nTop  = (n + 1) / 2
        val nLeft = n - nTop
        val topCols  = (0 until nTop ).map { i -> (i + 1) * (cols - 1) / (nTop  + 1) }
        val leftRows = (0 until nLeft).map { i -> (i + 1) * (rows - 1) / (nLeft + 1) }

        val entrances = topCols.map  { c -> EntranceCell(0, c, 0) } +
                        leftRows.map { r -> EntranceCell(r, 0, 3) }
        val entrancePairs = entrances.map { it.row to it.col }.toSet()

        // Retry up to 15 times to satisfy the minimum path length constraint.
        repeat(15) {
            val result = tryGenerate(rows, cols, minLen, exitRow, exitCol,
                entrances, entrancePairs, difficulty)
            if (result != null) return result
        }
        // Fallback: relax the length constraint.
        return tryGenerate(rows, cols, 0, exitRow, exitCol,
            entrances, entrancePairs, difficulty)!!
    }

    private fun tryGenerate(
        rows: Int, cols: Int, minLen: Int,
        exitRow: Int, exitCol: Int,
        entrances: List<EntranceCell>,
        entrancePairs: Set<Pair<Int, Int>>,
        difficulty: Difficulty
    ): MazeState? {

        // ── Build spanning tree via DFS from exit ─────────────────────────────
        val walls   = Array(rows) { Array(cols) { BooleanArray(4) { true } } }
        val visited = Array(rows) { BooleanArray(cols) }
        val stack   = ArrayDeque<Pair<Int, Int>>()
        stack.add(exitRow to exitCol)
        visited[exitRow][exitCol] = true

        while (stack.isNotEmpty()) {
            val (r, c) = stack.last()
            val dirs = (0..3).filter { d ->
                val nr = r + DR[d]; val nc = c + DC[d]
                if (nr !in 0 until rows || nc !in 0 until cols) return@filter false
                if (visited[nr][nc]) return@filter false
                // Keep entrance cells as leaves: block same-edge links involving entrance cells.
                if ((r to c) in entrancePairs || (nr to nc) in entrancePairs) {
                    if (r == 0 && nr == 0) return@filter false  // row-0 horizontal
                    if (c == 0 && nc == 0) return@filter false  // col-0 vertical
                }
                true
            }.shuffled()

            if (dirs.isEmpty()) { stack.removeLast(); continue }
            val d = dirs.first()
            val nr = r + DR[d]; val nc = c + DC[d]
            walls[r][c][d] = false
            walls[nr][nc][OPP[d]] = false
            visited[nr][nc] = true
            stack.add(nr to nc)
        }

        // Open entrance outer walls and exit bottom wall.
        for (e in entrances) walls[e.row][e.col][e.wallDir] = false
        walls[exitRow][exitCol][2] = false

        // ── Smart winner selection ────────────────────────────────────────────
        // Pick the winner that maximises the minimum unique-path length across
        // all losers (= cells in loser's path before it merges with winner's path).
        val n    = entrances.size
        val exit = exitRow to exitCol
        val cells = entrances.map { it.row to it.col }

        val winnerIdx = (0 until n).maxByOrNull { w ->
            val wPath = bfs(walls, rows, cols, cells[w], exit) ?: return@maxByOrNull -1
            val wSet  = wPath.toSet()
            (0 until n).filter { it != w }.minOf { i ->
                val lp = bfs(walls, rows, cols, cells[i], exit) ?: return@minOf 0
                lp.indexOfFirst { it in wSet }.takeIf { it > 0 } ?: 0
            }
        } ?: return null

        val winnerPath = bfs(walls, rows, cols, cells[winnerIdx], exit) ?: return null
        val winnerSet  = winnerPath.toSet()

        // ── Cut each loser's path at the merge point ──────────────────────────
        for (i in 0 until n) {
            if (i == winnerIdx) continue
            val lp       = bfs(walls, rows, cols, cells[i], exit) ?: continue
            val mergeIdx = lp.indexOfFirst { it in winnerSet }
            if (mergeIdx <= 0) continue
            if (minLen > 0 && mergeIdx < minLen) return null   // path too short → retry

            val (r1, c1) = lp[mergeIdx - 1]; val (r2, c2) = lp[mergeIdx]
            val d = dir(r1, c1, r2, c2)
            walls[r1][c1][d] = true
            walls[r2][c2][OPP[d]] = true
        }

        val grid = (0 until rows).map { r ->
            (0 until cols).map { c ->
                WallSet(walls[r][c][0], walls[r][c][1], walls[r][c][2], walls[r][c][3])
            }
        }
        return MazeState(rows, cols, grid, entrances, exitRow, exitCol,
            winnerIdx, winnerPath, difficulty = difficulty)
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
        val queue  = ArrayDeque<Pair<Int, Int>>()
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
