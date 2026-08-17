package com.nendie.sudoku

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Screen { HOME, GAME, RANKING }

enum class GameStatus { GENERATING, PLAYING, WON }

enum class GameMode { DIFF, DAILY, VDAILY }

enum class ErrorMode { IMMEDIATE, FINAL }

data class RankingEntry(
    val name: String,
    val elapsed: Int,
    val mistakes: Int,
    val ts: Long
)

data class GameUiState(
    val screen: Screen = Screen.HOME,
    val status: GameStatus = GameStatus.GENERATING,
    val mode: GameMode = GameMode.DIFF,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val variant: Variant = Variant.NORMAL,
    val givens: Set<Int> = emptySet(),
    val solution: IntArray = IntArray(81),
    val cells: IntArray = IntArray(81),
    val notes: IntArray = IntArray(81),
    val selected: Int = -1,
    val notesMode: Boolean = false,
    val mistakes: Int = 0,
    val elapsedSeconds: Int = 0,
    val wonDialog: Boolean = false,
    val showExitDialog: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val resumeAvailable: Boolean = false,
    val resumeSummary: String = "",
    val recordAvailable: Boolean = false,
    val recordSummary: String = "",
    val errorMode: ErrorMode = ErrorMode.IMMEDIATE,
    val simActive: Boolean = false,
    val simCells: IntArray = IntArray(81),
    val simNotes: IntArray = IntArray(81),
    val showErrors: Boolean = false,
    val showCheckDialog: Boolean = false,
    val checkTitle: String = "",
    val showHintDialog: Boolean = false,
    val showVariantDialog: Boolean = false,
    val variantDialogTitle: String = "",
    val pendingVariant: Variant = Variant.NORMAL,
    val profileName: String = "玩家",
    val rankMode: String = "easy",
    val rankEntries: List<RankingEntry> = emptyList(),
    val rankBest: Int = -1,
    val rankLatest: Int = -1,
    val rankAvg: Int = -1,
    val rankCount: Int = 0,
    val rankCompare: String = ""
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    var state by mutableStateOf(GameUiState())
        private set

    private val prefs = application.getSharedPreferences("sudoku_prefs", Context.MODE_PRIVATE)
    private var generationJob: Job? = null
    private var timerJob: Job? = null
    private val undoStack = ArrayDeque<Move>()
    private var simUndo: ArrayDeque<Move>? = null

    private data class Move(val index: Int, val value: Int, val notes: Int)

    init {
        val name = loadProfileName()
        val errorMode = runCatching {
            ErrorMode.valueOf(prefs.getString(KEY_ERROR_MODE, null) ?: ErrorMode.IMMEDIATE.name)
        }.getOrDefault(ErrorMode.IMMEDIATE)
        state = state.copy(profileName = name, errorMode = errorMode)
        refreshHomeData()
    }

    /** 每日一题的日期文案，如「8月18日 · 周二」。 */
    val dailyLabel: String
        get() = dailyLabelText()

    /* ================= 活跃图层（真实盘面 / 推演模拟层） ================= */

    private fun curCells(): IntArray = if (state.simActive) state.simCells else state.cells

    private fun curNotes(): IntArray = if (state.simActive) state.simNotes else state.notes

    private fun curUndo(): ArrayDeque<Move> = if (state.simActive && simUndo != null) simUndo!! else undoStack

    private fun withActiveLayer(cells: IntArray, notes: IntArray): GameUiState =
        if (state.simActive) state.copy(simCells = cells, simNotes = notes)
        else state.copy(cells = cells, notes = notes)

    /* ================= 开始游戏 ================= */

    fun startNewGame(difficulty: Difficulty) {
        state = state.copy(mode = GameMode.DIFF, difficulty = difficulty, variant = Variant.NORMAL)
        startGeneration(seed = null)
    }

    fun startDaily() {
        state = state.copy(mode = GameMode.DAILY, difficulty = Difficulty.NORMAL, variant = Variant.NORMAL)
        startGeneration(seed = LocalDate.now().toEpochDay())
    }

    fun startVariantDaily(variant: Variant) {
        state = state.copy(mode = GameMode.VDAILY, difficulty = Difficulty.NORMAL, variant = variant)
        startGeneration(seed = LocalDate.now().toEpochDay() * 10 + variant.ordinal)
    }

    fun startVariantGame(variant: Variant, difficulty: Difficulty) {
        state = state.copy(mode = GameMode.DIFF, difficulty = difficulty, variant = variant)
        startGeneration(seed = null)
    }

    fun requestVariant(variant: Variant) {
        state = state.copy(
            showVariantDialog = true,
            variantDialogTitle = variant.label + " · 选择难度",
            pendingVariant = variant
        )
    }

    fun dismissVariantDialog() {
        state = state.copy(showVariantDialog = false)
    }

    private fun startGeneration(seed: Long?) {
        generationJob?.cancel()
        timerJob?.cancel()
        undoStack.clear()
        simUndo = null
        state = state.copy(
            screen = Screen.GAME,
            status = GameStatus.GENERATING,
            givens = emptySet(),
            solution = IntArray(81),
            cells = IntArray(81),
            notes = IntArray(81),
            simActive = false,
            simCells = IntArray(81),
            simNotes = IntArray(81),
            showErrors = false,
            showCheckDialog = false,
            selected = -1,
            mistakes = 0,
            elapsedSeconds = 0,
            wonDialog = false,
            showExitDialog = false
        )
        generationJob = viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) {
                if (seed != null) SudokuEngine.generate(state.difficulty, seed, state.variant)
                else SudokuEngine.generate(state.difficulty, null, state.variant)
            }
            val givens = buildSet {
                for (i in 0 until 81) if (puzzle.givens[i] != 0) add(i)
            }
            state = state.copy(
                status = GameStatus.PLAYING,
                givens = givens,
                solution = puzzle.solution,
                cells = puzzle.givens,
                notes = IntArray(81),
                elapsedSeconds = 0,
                wonDialog = false
            )
            saveGame()
            startTimer()
        }
    }

    /** 继续上次未完成的游戏。 */
    fun resumeGame() {
        val saved = parseGame(prefs.getString(KEY_GAME, null)) ?: return
        if (saved.status != GameStatus.PLAYING) return
        generationJob?.cancel()
        timerJob?.cancel()
        undoStack.clear()
        simUndo = null
        state = saved.copy(
            selected = -1,
            notesMode = false,
            wonDialog = false,
            showExitDialog = false,
            simActive = false,
            simCells = IntArray(81),
            simNotes = IntArray(81),
            showErrors = false,
            showCheckDialog = false
        )
        startTimer()
    }

    /** 查看上次完成的棋盘。 */
    fun viewRecord() {
        val r = parseRecord(prefs.getString(KEY_RECORD, null)) ?: return
        generationJob?.cancel()
        timerJob?.cancel()
        undoStack.clear()
        simUndo = null
        state = GameUiState(
            screen = Screen.GAME,
            status = GameStatus.WON,
            mode = r.mode,
            difficulty = r.difficulty,
            variant = r.variant,
            givens = r.puzzle.indices.filter { r.puzzle[it] != 0 }.toSet(),
            solution = r.solution,
            cells = r.solution.copyOf(),
            notes = IntArray(81),
            elapsedSeconds = r.elapsed,
            mistakes = r.mistakes,
            wonDialog = false
        )
    }

    /** 用上次完成的同一道题重新玩。 */
    fun replayRecord() {
        val r = parseRecord(prefs.getString(KEY_RECORD, null)) ?: return
        generationJob?.cancel()
        timerJob?.cancel()
        undoStack.clear()
        simUndo = null
        state = GameUiState(
            screen = Screen.GAME,
            status = GameStatus.PLAYING,
            mode = r.mode,
            difficulty = r.difficulty,
            variant = r.variant,
            givens = r.puzzle.indices.filter { r.puzzle[it] != 0 }.toSet(),
            solution = r.solution,
            cells = r.puzzle.copyOf(),
            notes = IntArray(81),
            elapsedSeconds = 0,
            mistakes = 0,
            wonDialog = false
        )
        saveGame()
        startTimer()
    }

    /* ================= 退出与存档 ================= */

    fun onBackPressed() {
        when (state.screen) {
            Screen.GAME -> {
                if (state.status == GameStatus.PLAYING || state.status == GameStatus.GENERATING) {
                    state = state.copy(showExitDialog = true)
                } else {
                    goHome()
                }
            }
            Screen.RANKING -> closeRanking()
            Screen.HOME -> Unit
        }
    }

    fun exitSave() {
        saveGame()
        state = state.copy(showExitDialog = false)
        goHome()
    }

    fun exitDiscard() {
        deleteGame()
        state = state.copy(showExitDialog = false)
        goHome()
    }

    fun cancelExit() {
        state = state.copy(showExitDialog = false)
    }

    fun confirmDiscardResume() {
        state = state.copy(showDiscardDialog = true)
    }

    fun cancelDiscard() {
        state = state.copy(showDiscardDialog = false)
    }

    fun discardResume() {
        deleteGame()
        state = state.copy(showDiscardDialog = false)
        refreshHomeData()
    }

    fun goHome() {
        generationJob?.cancel()
        timerJob?.cancel()
        state = state.copy(screen = Screen.HOME, wonDialog = false, showExitDialog = false)
        refreshHomeData()
    }

    /* ================= 报错模式 ================= */

    fun setErrorMode(mode: ErrorMode) {
        if (state.errorMode == mode) return
        val s = if (mode == ErrorMode.FINAL) {
            state.copy(errorMode = mode, showErrors = false, mistakes = 0)
        } else {
            state.copy(errorMode = mode)
        }
        state = s
        prefs.edit().putString(KEY_ERROR_MODE, mode.name).apply()
    }

    fun dismissCheck() {
        state = state.copy(showCheckDialog = false)
    }

    /* ================= 推演模式 ================= */

    fun toggleSim() {
        if (state.simActive) exitSim() else enterSim()
    }

    fun enterSim() {
        if (state.status != GameStatus.PLAYING || state.simActive) return
        simUndo = ArrayDeque()
        state = state.copy(
            simActive = true,
            simCells = state.cells.copyOf(),
            simNotes = state.notes.copyOf(),
            selected = -1
        )
    }

    fun exitSim() {
        if (!state.simActive) return
        simUndo = null
        state = state.copy(
            simActive = false,
            simCells = IntArray(81),
            simNotes = IntArray(81),
            selected = -1
        )
    }

    /** 应用推演：把模拟结果覆盖到真实盘面，然后退出推演。 */
    fun applySim() {
        val s = state
        if (!s.simActive) return
        undoStack.clear()
        simUndo = null
        state = s.copy(
            cells = s.simCells.copyOf(),
            notes = s.simNotes.copyOf(),
            simActive = false,
            simCells = IntArray(81),
            simNotes = IntArray(81),
            selected = -1
        )
        if (state.errorMode == ErrorMode.FINAL) {
            state = state.copy(showErrors = false, mistakes = 0)
            onMoveDone()
        } else {
            state = state.copy(mistakes = countWrongCells(state))
            checkWin()
        }
        saveGame()
    }

    /* ================= 排行榜 ================= */

    fun startRanking() {
        val name = loadProfileName()
        state = state.copy(screen = Screen.RANKING, profileName = name)
        loadRanking("easy")
    }

    fun closeRanking() {
        state = state.copy(screen = Screen.HOME)
        refreshHomeData()
    }

    fun switchRankMode(mode: String) {
        loadRanking(mode)
    }

    fun saveProfile(name: String) {
        val n = name.trim().ifBlank { "玩家" }
        state = state.copy(profileName = n)
        prefs.edit().putString(KEY_PROFILE, n).apply()
        loadRanking(state.rankMode)
    }

    private fun loadRanking(mode: String) {
        val entries = loadRankingEntries(mode)
            .sortedWith(compareBy<RankingEntry> { it.elapsed }.thenBy { it.ts })
        val top = entries.take(10)
        val name = loadProfileName()
        val mine = entries.filter { it.name == name }
        val best = mine.minOfOrNull { it.elapsed } ?: -1
        val latest = mine.lastOrNull()?.elapsed ?: -1
        val avg = if (mine.isNotEmpty()) mine.map { it.elapsed }.average().toInt() else -1
        val compare = when {
            mine.isEmpty() -> ""
            latest < best -> "打破最快纪录，快了 ${fmtTime(best - latest)}"
            latest > best -> "比最快慢 ${fmtTime(latest - best)}"
            else -> "与最快纪录持平"
        }
        state = state.copy(
            rankMode = mode,
            rankEntries = top,
            rankBest = best,
            rankLatest = latest,
            rankAvg = avg,
            rankCount = mine.size,
            rankCompare = compare
        )
    }

    private fun loadRankingEntries(mode: String): List<RankingEntry> {
        val raw = prefs.getString("rankings_$mode", null) ?: return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 4) null
            else RankingEntry(
                name = p[0],
                elapsed = p[1].toIntOrNull() ?: 0,
                mistakes = p[2].toIntOrNull() ?: 0,
                ts = p[3].toLongOrNull() ?: 0L
            )
        }.toList()
    }

    private fun recordScore() {
        val name = loadProfileName()
        val mode = rankModeKey()
        val entries = loadRankingEntries(mode).toMutableList()
        entries += RankingEntry(name, state.elapsedSeconds, state.mistakes, System.currentTimeMillis())
        val trimmed = if (entries.size > 200) entries.sortedBy { it.ts }.takeLast(200) else entries
        val data = trimmed.joinToString("\n") {
            "${it.name}|${it.elapsed}|${it.mistakes}|${it.ts}"
        }
        prefs.edit().putString("rankings_$mode", data).apply()
    }

    private fun rankModeKey(): String = when {
        state.mode == GameMode.DAILY -> "daily"
        state.variant == Variant.X -> "x_" + state.difficulty.name.lowercase()
        state.variant == Variant.HYPER -> "hyper_" + state.difficulty.name.lowercase()
        else -> state.difficulty.name.lowercase()
    }

    /* ================= 游戏操作 ================= */

    fun select(index: Int) {
        if (state.status != GameStatus.PLAYING) return
        state = state.copy(selected = index)
    }

    fun inputNumber(n: Int) {
        val s = state
        if (s.status != GameStatus.PLAYING || s.selected == -1) return
        val i = s.selected
        if (i in s.givens) return
        val cells = curCells().copyOf()
        val notes = curNotes().copyOf()

        if (s.notesMode) {
            if (cells[i] != 0) return
            pushUndo(i, s)
            notes[i] = notes[i] xor (1 shl (n - 1))
            state = withActiveLayer(cells, notes)
            onMoveDone()
            return
        }

        if (cells[i] == n) {
            pushUndo(i, s)
            cells[i] = 0
            notes[i] = 0
            state = withActiveLayer(cells, notes)
            onMoveDone()
            return
        }

        pushUndo(i, s)
        cells[i] = n
        notes[i] = 0
        val newMistakes = if (!s.simActive && s.errorMode == ErrorMode.IMMEDIATE && s.solution[i] != n) {
            s.mistakes + 1
        } else {
            s.mistakes
        }
        state = withActiveLayer(cells, notes).copy(mistakes = newMistakes)
        onMoveDone()
    }

    fun toggleNotesMode() {
        if (state.status != GameStatus.PLAYING) return
        state = state.copy(notesMode = !state.notesMode)
    }

    fun undo() {
        val s = state
        if (s.status != GameStatus.PLAYING) return
        val stack = curUndo()
        val move = stack.removeLastOrNull() ?: return
        val cells = curCells().copyOf()
        val notes = curNotes().copyOf()
        cells[move.index] = move.value
        notes[move.index] = move.notes
        state = withActiveLayer(cells, notes)
        onMoveDone()
    }

    fun erase() {
        val s = state
        if (s.status != GameStatus.PLAYING || s.selected == -1) return
        val i = s.selected
        if (i in s.givens) return
        val cells = curCells().copyOf()
        val notes = curNotes().copyOf()
        if (cells[i] == 0 && notes[i] == 0) return
        pushUndo(i, s)
        cells[i] = 0
        notes[i] = 0
        state = withActiveLayer(cells, notes)
        onMoveDone()
    }

    fun hint() {
        val s = state
        if (s.status != GameStatus.PLAYING) return
        val cells = curCells()
        val target = if (s.selected != -1 && cells[s.selected] == 0) s.selected else -1
        if (target == -1) {
            state = state.copy(showHintDialog = true)
            return
        }
        pushUndo(target, s)
        val newCells = cells.copyOf()
        val newNotes = curNotes().copyOf()
        newCells[target] = s.solution[target]
        newNotes[target] = 0
        state = withActiveLayer(newCells, newNotes).copy(selected = target)
        onMoveDone()
    }

    fun dismissWin() {
        state = state.copy(wonDialog = false)
    }

    fun dismissHint() {
        state = state.copy(showHintDialog = false)
    }

    /** 重开一局：每日一题重开同一题，普通难度生成新题。 */
    fun restart() {
        when (state.mode) {
            GameMode.DAILY -> startDaily()
            GameMode.VDAILY -> startVariantDaily(state.variant)
            GameMode.DIFF -> startVariantGame(state.variant, state.difficulty)
        }
    }

    private fun pushUndo(index: Int, s: GameUiState) {
        val stack = curUndo()
        stack.addLast(Move(index, curCells()[index], curNotes()[index]))
        if (stack.size > 200) stack.removeFirst()
    }

    private fun onMoveDone() {
        if (state.simActive) return
        if (state.errorMode == ErrorMode.FINAL) maybeCheckFinal() else checkWin()
    }

    private fun boardFull(s: GameUiState): Boolean {
        for (i in 0 until 81) if (s.cells[i] == 0) return false
        return true
    }

    private fun countWrongCells(s: GameUiState): Int {
        var wrong = 0
        for (i in 0 until 81) {
            if (i !in s.givens && s.cells[i] != 0 && s.cells[i] != s.solution[i]) wrong++
        }
        return wrong
    }

    private fun maybeCheckFinal() {
        val s = state
        if (!boardFull(s)) return
        val wrong = countWrongCells(s)
        if (wrong == 0) {
            checkWin()
            return
        }
        state = s.copy(
            showErrors = true,
            mistakes = wrong,
            showCheckDialog = true,
            checkTitle = "还有 $wrong 处错误"
        )
    }

    private fun checkWin() {
        val s = state
        for (i in 0 until 81) {
            if (s.cells[i] != s.solution[i]) return
        }
        timerJob?.cancel()
        state = s.copy(status = GameStatus.WON, wonDialog = true)
        saveGame()
        saveRecord()
        recordScore()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (state.status != GameStatus.PLAYING) break
                state = state.copy(elapsedSeconds = state.elapsedSeconds + 1)
                saveGame()
            }
        }
    }

    /* ================= 持久化 ================= */

    private fun saveGame() {
        if (state.screen != Screen.GAME || state.status == GameStatus.GENERATING) return
        prefs.edit().putString(KEY_GAME, serializeGame()).apply()
    }

    private fun deleteGame() {
        prefs.edit().remove(KEY_GAME).apply()
    }

    private fun saveRecord() {
        prefs.edit().putString(KEY_RECORD, serializeRecord()).apply()
    }

    private fun serializeGame(): String {
        val s = state
        val statusCode = if (s.status == GameStatus.WON) "2" else "1"
        return listOf(
            s.mode.name,
            s.difficulty.name,
            s.variant.name,
            s.elapsedSeconds.toString(),
            s.mistakes.toString(),
            statusCode,
            s.puzzle.joinToString(","),
            s.solution.joinToString(","),
            s.cells.joinToString(","),
            s.notes.joinToString(",")
        ).joinToString("|")
    }

    private fun serializeRecord(): String {
        val s = state
        return listOf(
            s.mode.name,
            s.difficulty.name,
            s.variant.name,
            s.elapsedSeconds.toString(),
            s.mistakes.toString(),
            System.currentTimeMillis().toString(),
            s.puzzle.joinToString(","),
            s.solution.joinToString(",")
        ).joinToString("|")
    }

    private fun parseGame(str: String?): GameUiState? {
        if (str == null) return null
        val parts = str.split("|")
        if (parts.size < 9) return null
        val hasVariant = parts.size >= 10
        val puzzle = parseInts(parts[if (hasVariant) 6 else 5]) ?: return null
        val solution = parseInts(parts[if (hasVariant) 7 else 6]) ?: return null
        val cells = parseInts(parts[if (hasVariant) 8 else 7]) ?: return null
        val notes = parseInts(parts[if (hasVariant) 9 else 8]) ?: return null
        return GameUiState(
            screen = Screen.GAME,
            status = if (parts[if (hasVariant) 5 else 4] == "2") GameStatus.WON else GameStatus.PLAYING,
            mode = enumOr(GameMode.DIFF, parts[0]),
            difficulty = enumOr(Difficulty.NORMAL, parts[1]),
            variant = if (hasVariant) enumOr(Variant.NORMAL, parts[2]) else Variant.NORMAL,
            givens = puzzle.indices.filter { puzzle[it] != 0 }.toSet(),
            solution = solution,
            cells = cells,
            notes = notes,
            elapsedSeconds = parts[if (hasVariant) 3 else 2].toIntOrNull() ?: 0,
            mistakes = parts[if (hasVariant) 4 else 3].toIntOrNull() ?: 0
        )
    }

    private data class SavedRecord(
        val mode: GameMode,
        val difficulty: Difficulty,
        val variant: Variant,
        val elapsed: Int,
        val mistakes: Int,
        val completedAt: Long,
        val puzzle: IntArray,
        val solution: IntArray
    )

    private fun parseRecord(str: String?): SavedRecord? {
        if (str == null) return null
        val parts = str.split("|")
        if (parts.size < 7) return null
        val hasVariant = parts.size >= 8
        val puzzle = parseInts(parts[if (hasVariant) 6 else 5]) ?: return null
        val solution = parseInts(parts[if (hasVariant) 7 else 6]) ?: return null
        return SavedRecord(
            mode = enumOr(GameMode.DIFF, parts[0]),
            difficulty = enumOr(Difficulty.NORMAL, parts[1]),
            variant = if (hasVariant) enumOr(Variant.NORMAL, parts[2]) else Variant.NORMAL,
            elapsed = parts[if (hasVariant) 3 else 2].toIntOrNull() ?: 0,
            mistakes = parts[if (hasVariant) 4 else 3].toIntOrNull() ?: 0,
            completedAt = parts[if (hasVariant) 5 else 4].toLongOrNull() ?: 0L,
            puzzle = puzzle,
            solution = solution
        )
    }

    private fun parseInts(s: String): IntArray? {
        val arr = s.split(",").mapNotNull { it.toIntOrNull() }
        return if (arr.size == 81) arr.toIntArray() else null
    }

    private inline fun <reified T : Enum<T>> enumOr(default: T, name: String): T =
        runCatching { enumValueOf<T>(name) }.getOrDefault(default)

    private fun loadProfileName(): String {
        val raw = prefs.getString(KEY_PROFILE, null) ?: return "玩家"
        return raw.split("|").getOrNull(0)?.takeIf { it.isNotBlank() } ?: "玩家"
    }

    private fun refreshHomeData() {
        val saved = parseGame(prefs.getString(KEY_GAME, null))
        val resumeAvailable = saved != null && saved.status == GameStatus.PLAYING
        val resumeSummary = if (resumeAvailable && saved != null) {
            val filled = saved.cells.count { it != 0 }
            val label = modeLabel(saved.mode, saved.difficulty, saved.variant)
            "$label · 已填 $filled/81 · ${fmtTime(saved.elapsedSeconds)}"
        } else {
            ""
        }

        val record = parseRecord(prefs.getString(KEY_RECORD, null))
        val recordAvailable = record != null
        val recordSummary = if (record != null) {
            val label = modeLabel(record.mode, record.difficulty, record.variant)
            val d = Instant.ofEpochMilli(record.completedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            "$label · 用时 ${fmtTime(record.elapsed)} · 错误 ${record.mistakes} · ${d.monthValue}月${d.dayOfMonth}日"
        } else {
            ""
        }

        state = state.copy(
            resumeAvailable = resumeAvailable,
            resumeSummary = resumeSummary,
            recordAvailable = recordAvailable,
            recordSummary = recordSummary
        )
    }

    private fun dailyLabelText(): String {
        val d = LocalDate.now()
        val week = arrayOf("日", "一", "二", "三", "四", "五", "六")
        return "${d.monthValue}月${d.dayOfMonth}日 · 周${week[d.dayOfWeek.value % 7]}"
    }

    private fun modeLabel(mode: GameMode, difficulty: Difficulty, variant: Variant): String {
        val base = if (mode == GameMode.DAILY) "每日一题" else difficulty.label
        return if (variant == Variant.NORMAL) base else variant.label + " · " + base
    }

    private fun fmtTime(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    companion object {
        private const val KEY_GAME = "game"
        private const val KEY_RECORD = "record"
        private const val KEY_ERROR_MODE = "error_mode"
        private const val KEY_PROFILE = "profile"
    }
}
