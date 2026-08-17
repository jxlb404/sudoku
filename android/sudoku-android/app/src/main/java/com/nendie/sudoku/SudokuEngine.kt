package com.nendie.sudoku

import kotlin.random.Random

enum class Difficulty(val label: String, val clueTarget: Int, val color: Long) {
    EASY("简单", 42, 0xFF1FA06A),
    NORMAL("普通", 36, 0xFF3F5FD0),
    EXPERT("专家", 24, 0xFFD63A63)
}

/**
 * 数独核心引擎：生成唯一解题目（180° 对称挖空）、解法计数、合法校验。
 * 支持传入种子（用于"每日一题"：同一天固定同一道题）。
 */
object SudokuEngine {

    private val peers: Array<IntArray> = Array(81) { i ->
        val r = i / 9
        val c = i % 9
        val set = LinkedHashSet<Int>()
        for (cc in 0..8) if (cc != c) set.add(r * 9 + cc)
        for (rr in 0..8) if (rr != r) set.add(rr * 9 + c)
        val br = r / 3 * 3
        val bc = c / 3 * 3
        for (rr in br until br + 3) {
            for (cc in bc until bc + 3) {
                if (rr != r || cc != c) set.add(rr * 9 + cc)
            }
        }
        set.toIntArray()
    }

    /** 与 [index] 同行、同列或同宫的格子集合。 */
    fun peersOf(index: Int): IntArray = peers[index]

    /** [a] 与 [b] 是否同行/同列/同宫。 */
    fun arePeers(a: Int, b: Int): Boolean = peers[a].contains(b)

    private fun isValid(grid: IntArray, index: Int, value: Int): Boolean {
        for (p in peers[index]) if (grid[p] == value) return false
        return true
    }

    /** 统计解法数量（最多数到 [limit] 即返回，用于判断唯一性）。 */
    fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        var best = -1
        var bestCands: List<Int>? = null
        for (i in 0 until 81) {
            if (grid[i] != 0) continue
            val cands = (1..9).filter { isValid(grid, i, it) }
            if (bestCands == null || cands.size < bestCands.size) {
                best = i
                bestCands = cands
                if (cands.size <= 1) break
            }
        }
        if (best == -1) return 1
        var total = 0
        for (v in bestCands!!) {
            grid[best] = v
            total += countSolutions(grid, limit)
            grid[best] = 0
            if (total >= limit) break
        }
        return total
    }

    /** 解题搜索工作量：回溯搜索中尝试填数的次数，越大代表题越难。 */
    fun solveCost(grid: IntArray): Int {
        var nodes = 0
        fun search(g: IntArray): Int {
            var best = -1
            var bestCands: List<Int>? = null
            for (i in 0 until 81) {
                if (g[i] != 0) continue
                val cands = (1..9).filter { isValid(g, i, it) }
                if (bestCands == null || cands.size < bestCands.size) {
                    best = i
                    bestCands = cands
                    if (cands.size <= 1) break
                }
            }
            if (best == -1) return 1
            var found = 0
            for (v in bestCands!!) {
                nodes++
                g[best] = v
                found += search(g)
                g[best] = 0
                if (found >= 1) break
            }
            return found
        }
        search(grid.copyOf())
        return nodes
    }

    private fun fillSolution(grid: IntArray, rng: Random): Boolean {
        for (i in 0 until 81) {
            if (grid[i] != 0) continue
            val cands = (1..9).filter { isValid(grid, i, it) }.shuffled(rng)
            for (v in cands) {
                grid[i] = v
                if (fillSolution(grid, rng)) return true
                grid[i] = 0
            }
            return false
        }
        return true
    }

    data class Puzzle(val givens: IntArray, val solution: IntArray, val clues: Int)

    /**
     * 生成一道 [difficulty] 的题目：先随机生成完整终盘，再按 180° 对称挖空，
     * 每步保证唯一解。传 [seed] 可让同一天题目固定（每日一题）。
     */
    fun generate(difficulty: Difficulty, seed: Long? = null): Puzzle {
        val rng = if (seed != null) Random(seed) else Random.Default
        val target = difficulty.clueTarget
        val maxAttempts = if (difficulty == Difficulty.EXPERT) 20 else 8
        var best: Puzzle? = null
        repeat(maxAttempts) {
            val puzzle = dig(target, rng)
            if (best == null || puzzle.clues < best.clues) best = puzzle
            val accepted = if (difficulty == Difficulty.EXPERT) {
                puzzle.clues <= 28 && solveCost(puzzle.givens) >= 200
            } else {
                puzzle.clues <= target
            }
            if (accepted) return puzzle
        }
        return best!!
    }

    private fun dig(target: Int, rng: Random): Puzzle {
        val grid = IntArray(81)
        fillSolution(grid, rng)
        val solution = grid.copyOf()
        var remaining = 81

        // 奇偶修正：优先挖掉中心格，使后续对称成对挖空能精确达到目标提示数
        val parityOrder = listOf(40) + (0 until 81).filter { it != 40 }
        for (idx in parityOrder) {
            if (grid[idx] == 0) continue
            val saved = grid[idx]
            grid[idx] = 0
            if (countSolutions(grid, 2) == 1) {
                remaining--
                break
            }
            grid[idx] = saved
        }

        val positions = (0 until 81).shuffled(rng)
        for (p in positions) {
            if (remaining <= target) break
            if (grid[p] == 0) continue
            val q = (8 - p / 9) * 9 + (8 - p % 9)
            if (p == q || grid[q] == 0) continue
            val a = grid[p]
            val b = grid[q]
            grid[p] = 0
            grid[q] = 0
            if (countSolutions(grid, 2) == 1) {
                remaining -= 2
            } else {
                grid[p] = a
                grid[q] = b
            }
        }
        return Puzzle(grid, solution, remaining)
    }
}
