package com.nendie.sudoku

import kotlin.random.Random

enum class Difficulty(val label: String, val clueTarget: Int, val color: Long) {
    EASY("简单", 44, 0xFF1FA06A),
    NORMAL("普通", 34, 0xFF3F5FD0),
    EXPERT("专家", 24, 0xFFD63A63)
}

/** 数独变体：标准 / X 数独（双对角线）/ 超数独（四角 3×3 窗）。 */
enum class Variant(val label: String) {
    NORMAL("标准"),
    X("X数独"),
    HYPER("超数独")
}

/**
 * 数独核心引擎：生成唯一解题目（180° 对称挖空）、解法计数、合法校验。
 * 支持变体（X / 超数独）与种子（每日一题）。
 */
object SudokuEngine {

    private val peers: Map<Variant, Array<IntArray>> = Variant.entries.associateWith { buildPeers(it) }

    private fun buildPeers(variant: Variant): Array<IntArray> = Array(81) { i ->
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
        if (variant == Variant.X) {
            if (r == c) for (k in 0..8) if (k != r) set.add(k * 9 + k)
            if (r + c == 8) for (k in 0..8) if (k != r) set.add(k * 9 + (8 - k))
        }
        if (variant == Variant.HYPER) {
            val wins = arrayOf(
                intArrayOf(1, 3, 1, 3),
                intArrayOf(1, 3, 5, 7),
                intArrayOf(5, 7, 1, 3),
                intArrayOf(5, 7, 5, 7)
            )
            for (w in wins) {
                if (r in w[0]..w[1] && c in w[2]..w[3]) {
                    for (rr in w[0]..w[1]) {
                        for (cc in w[2]..w[3]) {
                            if (rr != r || cc != c) set.add(rr * 9 + cc)
                        }
                    }
                }
            }
        }
        set.toIntArray()
    }

    fun peersOf(variant: Variant, index: Int): IntArray = peers.getValue(variant)[index]

    fun arePeers(variant: Variant, a: Int, b: Int): Boolean =
        peers.getValue(variant)[a].contains(b)

    private fun isValid(peers: Array<IntArray>, grid: IntArray, index: Int, value: Int): Boolean {
        for (p in peers[index]) if (grid[p] == value) return false
        return true
    }

    /** 统计解法数量（最多数到 [limit] 即返回，用于判断唯一性）。 */
    fun countSolutions(peers: Array<IntArray>, grid: IntArray, limit: Int = 2): Int {
        var best = -1
        var bestCands: List<Int>? = null
        for (i in 0 until 81) {
            if (grid[i] != 0) continue
            val cands = (1..9).filter { isValid(peers, grid, i, it) }
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
            total += countSolutions(peers, grid, limit)
            grid[best] = 0
            if (total >= limit) break
        }
        return total
    }

    /** 解题搜索工作量：回溯搜索中尝试填数的次数，越大代表题越难。 */
    fun solveCost(peers: Array<IntArray>, grid: IntArray): Int {
        var nodes = 0
        fun search(g: IntArray): Int {
            var best = -1
            var bestCands: List<Int>? = null
            for (i in 0 until 81) {
                if (g[i] != 0) continue
                val cands = (1..9).filter { isValid(peers, g, i, it) }
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

    private fun fillSolution(peers: Array<IntArray>, grid: IntArray, rng: Random): Boolean {
        for (i in 0 until 81) {
            if (grid[i] != 0) continue
            val cands = (1..9).filter { isValid(peers, grid, i, it) }.shuffled(rng)
            for (v in cands) {
                grid[i] = v
                if (fillSolution(peers, grid, rng)) return true
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
    fun generate(difficulty: Difficulty, seed: Long? = null, variant: Variant = Variant.NORMAL): Puzzle {
        val rng = if (seed != null) Random(seed) else Random.Default
        val p = peers.getValue(variant)
        val target = difficulty.clueTarget
        if (difficulty == Difficulty.EXPERT) {
            var last: Puzzle? = null
            for (attempt in 0 until 200) {
                last = dig(p, target, rng)
                if (last.clues <= 28 && solveCost(p, last.givens) >= 250) return last
            }
            return last!!
        }
        for (attempt in 0 until 8) {
            val puzzle = dig(p, target, rng)
            if (puzzle.clues <= target) return puzzle
        }
        return dig(p, target, rng)
    }

    private fun dig(peers: Array<IntArray>, target: Int, rng: Random): Puzzle {
        val grid = IntArray(81)
        fillSolution(peers, grid, rng)
        val solution = grid.copyOf()
        var remaining = 81

        // 奇偶修正：优先挖掉中心格，使后续对称成对挖空能精确达到目标提示数
        val parityOrder = listOf(40) + (0 until 81).filter { it != 40 }
        for (idx in parityOrder) {
            if (grid[idx] == 0) continue
            val saved = grid[idx]
            grid[idx] = 0
            if (countSolutions(peers, grid, 2) == 1) {
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
            if (countSolutions(peers, grid, 2) == 1) {
                remaining -= 2
            } else {
                grid[p] = a
                grid[q] = b
            }
        }
        return Puzzle(grid, solution, remaining)
    }
}
