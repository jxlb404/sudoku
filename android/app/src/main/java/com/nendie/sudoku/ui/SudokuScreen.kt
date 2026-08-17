package com.nendie.sudoku.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nendie.sudoku.Difficulty
import com.nendie.sudoku.ErrorMode
import com.nendie.sudoku.GameMode
import com.nendie.sudoku.GameStatus
import com.nendie.sudoku.GameUiState
import com.nendie.sudoku.GameViewModel
import com.nendie.sudoku.RankingEntry
import com.nendie.sudoku.Screen
import com.nendie.sudoku.SudokuEngine
import com.nendie.sudoku.Variant
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun SudokuApp(viewModel: GameViewModel = viewModel()) {
    val state = viewModel.state
    when (state.screen) {
        Screen.HOME -> HomeScreen(viewModel)
        Screen.GAME -> GameScreen(state, viewModel)
        Screen.RANKING -> RankingScreen(state, viewModel)
    }
}

/* ================= 主页 ================= */

@Composable
private fun HomeScreen(viewModel: GameViewModel) {
    val state = viewModel.state
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF5B5BEF), Color(0xFF8B8DFA)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.GridOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "数独",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(end = 14.sp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "清爽界面 · 每天一题 · 进度可保存",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(22.dp))

            DailyCard(title = "每日一题", dateLabel = viewModel.dailyLabel, onClick = viewModel::startDaily)
            Spacer(Modifier.height(16.dp))

            ErrorModeSelector(errorMode = state.errorMode, onSelect = viewModel::setErrorMode)
            Spacer(Modifier.height(16.dp))

            RankCard(
                profileName = state.profileName,
                onClick = viewModel::startRanking
            )
            Spacer(Modifier.height(16.dp))

            DailyCard(title = "⊞ 今日超数独", dateLabel = viewModel.dailyLabel) {
                viewModel.startVariantDaily(Variant.HYPER)
            }
            Spacer(Modifier.height(16.dp))
            DailyCard(title = "✕ 今日X数独", dateLabel = viewModel.dailyLabel) {
                viewModel.startVariantDaily(Variant.X)
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                VariantCard("⊞", "超数独", "四窗 Windoku") { viewModel.requestVariant(Variant.HYPER) }
                VariantCard("✕", "X 数独", "双对角线") { viewModel.requestVariant(Variant.X) }
            }
            Spacer(Modifier.height(16.dp))

            if (state.resumeAvailable) {
                ResumeCard(
                    summary = state.resumeSummary,
                    onResume = viewModel::resumeGame,
                    onDiscard = viewModel::confirmDiscardResume
                )
                Spacer(Modifier.height(16.dp))
            }

            if (state.recordAvailable) {
                RecordCard(
                    summary = state.recordSummary,
                    onView = viewModel::viewRecord,
                    onReplay = viewModel::replayRecord
                )
                Spacer(Modifier.height(16.dp))
            }

            Difficulty.entries.forEach { difficulty ->
                DifficultyCard(difficulty) { viewModel.startNewGame(difficulty) }
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "每道题都保证只有唯一解",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }

    if (state.showDiscardDialog) {
        Dialog(onDismissRequest = viewModel::cancelDiscard) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("舍弃当前进度？", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "未完成的进度将被删除，且不可恢复。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = viewModel::discardResume,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("确定舍弃")
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = viewModel::cancelDiscard, modifier = Modifier.fillMaxWidth()) {
                        Text("取消")
                    }
                }
            }
        }
    }

    if (state.showVariantDialog) {
        Dialog(onDismissRequest = viewModel::dismissVariantDialog) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.variantDialogTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "与标准数独相同的难度优化",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { viewModel.startVariantGame(state.pendingVariant, Difficulty.EASY) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("简单")
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.startVariantGame(state.pendingVariant, Difficulty.NORMAL) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("普通")
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.startVariantGame(state.pendingVariant, Difficulty.EXPERT) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("专家")
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = viewModel::dismissVariantDialog, modifier = Modifier.fillMaxWidth()) {
                        Text("取消")
                    }
                }
            }
        }
    }
}

@Composable
private fun VariantCard(icon: String, name: String, sub: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(3.dp))
        Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(sub, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DailyCard(title: String, dateLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF6C453), Color(0xFFE8892C))))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF3A2408)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = dateLabel,
                fontSize = 12.sp,
                color = Color(0xFF3A2408).copy(alpha = 0.82f)
            )
        }
        Text(
            text = "开始 →",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF3A2408)
        )
    }
}

@Composable
private fun ErrorModeSelector(errorMode: ErrorMode, onSelect: (ErrorMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "报错方式",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 10.dp)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(3.dp)
        ) {
            ModeSeg("⚡ 及时报错", errorMode == ErrorMode.IMMEDIATE) { onSelect(ErrorMode.IMMEDIATE) }
            ModeSeg("🏁 完成后检查", errorMode == ErrorMode.FINAL) { onSelect(ErrorMode.FINAL) }
        }
    }
}

@Composable
private fun ModeSeg(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RankCard(profileName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF7AA2FF), Color(0xFF5B5BEF)))),
            contentAlignment = Alignment.Center
        ) {
            Text("🏆", fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("排行榜", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$profileName · 本地排名 · 各模式前十",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("→", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ResumeCard(summary: String, onResume: () -> Unit, onDiscard: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onResume)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("▶", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("继续上次游戏", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(summary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onDiscard) {
            Text("舍弃")
        }
    }
}

@Composable
private fun RecordCard(summary: String, onView: () -> Unit, onReplay: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onView)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFF6C453), Color(0xFFE8892C)))),
            contentAlignment = Alignment.Center
        ) {
            Text("🏆", fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("上次完成", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(summary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onReplay) {
            Text("重玩")
        }
    }
}

@Composable
private fun DifficultyCard(difficulty: Difficulty, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Color(difficulty.color))
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = difficulty.label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = difficultyCluesLabel(difficulty),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun difficultyCluesLabel(difficulty: Difficulty): String =
    if (difficulty == Difficulty.EXPERT) "约 24–28 个提示 · 高难度"
    else "约 ${difficulty.clueTarget} 个提示"

/* ================= 排行榜 ================= */

@Composable
private fun RankingScreen(state: GameUiState, viewModel: GameViewModel) {
    var name by rememberSaveable { mutableStateOf(state.profileName) }
    val modes = listOf(
        "easy" to "简单",
        "normal" to "普通",
        "expert" to "专家",
        "daily" to "每日一题",
        "x_easy" to "X·简单",
        "x_normal" to "X·普通",
        "x_expert" to "X·专家",
        "hyper_easy" to "超·简单",
        "hyper_normal" to "超·普通",
        "hyper_expert" to "超·专家",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::closeRanking) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "🏆 排行榜",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("默认用户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = { viewModel.saveProfile(name) }, modifier = Modifier.fillMaxWidth()) {
                Text("保存用户名")
            }
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modes.forEach { (key, label) ->
                    val active = state.rankMode == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { viewModel.switchRankMode(key) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            if (state.rankEntries.isEmpty()) {
                Text(
                    text = "该模式还没有记录，完成一局即可上榜",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            } else {
                state.rankEntries.forEachIndexed { index, entry ->
                    RankRow(
                        index = index,
                        entry = entry,
                        isMe = entry.name == state.profileName && entry.address == state.profileAddress
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (state.rankCount > 0) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "最佳 ${formatTime(state.rankBest)} · 最近 ${formatTime(state.rankLatest)} · 平均 ${formatTime(state.rankAvg)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "共 ${state.rankCount} 局 · ${state.rankCompare}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RankRow(index: Int, entry: RankingEntry, isMe: Boolean) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (isMe) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val medal = when (index) {
            0 -> "🥇"
            1 -> "🥈"
            2 -> "🥉"
            else -> (index + 1).toString()
        }
        Text(
            text = medal,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatTime(entry.elapsed), fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(rankDateText(entry.ts), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun rankDateText(ts: Long): String {
    val d = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
    return "${d.monthValue}月${d.dayOfMonth}日"
}

/* ================= 游戏界面 ================= */

@Composable
private fun GameScreen(state: GameUiState, viewModel: GameViewModel) {
    BackHandler {
        viewModel.onBackPressed()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            TopBar(state, viewModel)
            Spacer(Modifier.height(16.dp))

            if (state.simActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SimPill("🔮 推演中 · 退出", Color(0xC63A3F55), Color.White, viewModel::exitSim)
                    Spacer(Modifier.width(8.dp))
                    SimPill("✓ 应用", Color(0xFF22A06B), Color.White, viewModel::applySim)
                }
                Spacer(Modifier.height(10.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp)
                    .aspectRatio(1f)
            ) {
                Board(
                    state = state,
                    onSelect = viewModel::select,
                    modifier = Modifier.fillMaxSize()
                )
                if (state.simActive) {
                    Box(Modifier.matchParentSize().background(Color(0x24000000)))
                }
            }
            if (state.variant == Variant.HYPER) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "⊞ 浅灰四窗区域也须包含 1–9",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (state.variant == Variant.X) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "✕ 两条对角线也须包含 1–9",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chipText(state),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusText(state),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            Controls(state, viewModel)
            Spacer(Modifier.height(16.dp))
            NumberPad(viewModel)
        }

        if (state.status == GameStatus.GENERATING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    if (state.showExitDialog) {
        ExitDialog(viewModel)
    }

    if (state.showCheckDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCheck,
            title = { Text("🔍 ${state.checkTitle}") },
            text = { Text("错误格子已标红，修改后会自动再次检查。") },
            confirmButton = {
                TextButton(onClick = viewModel::dismissCheck) {
                    Text("知道了")
                }
            }
        )
    }

    if (state.showHintDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissHint,
            title = { Text("💡 提示") },
            text = { Text("请先点选一个空格子，再点提示。") },
            confirmButton = {
                TextButton(onClick = viewModel::dismissHint) {
                    Text("知道了")
                }
            }
        )
    }

    if (state.wonDialog) {
        Dialog(onDismissRequest = viewModel::dismissWin) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏆 完成！", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    Text("用时 ${formatTime(state.elapsedSeconds)}", fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "错误 ${state.mistakes} 次",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            viewModel.dismissWin()
                            viewModel.restart()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("再来一局")
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.dismissWin()
                            viewModel.startRanking()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🏆 排行榜")
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = viewModel::goHome, modifier = Modifier.fillMaxWidth()) {
                        Text("回主页")
                    }
                }
            }
        }
    }
}

@Composable
private fun SimPill(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(999.dp), color = bg, shadowElevation = 4.dp) {
        Box(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExitDialog(viewModel: GameViewModel) {
    Dialog(onDismissRequest = viewModel::cancelExit) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⏸ 退出游戏？", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "选择保存方式，下次可以接着玩",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                Button(onClick = viewModel::exitSave, modifier = Modifier.fillMaxWidth()) {
                    Text("💾 保存并退出")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = viewModel::exitDiscard, modifier = Modifier.fillMaxWidth()) {
                    Text("🗑 不保存退出")
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = viewModel::cancelExit, modifier = Modifier.fillMaxWidth()) {
                    Text("继续游戏")
                }
            }
        }
    }
}

@Composable
private fun TopBar(state: GameUiState, viewModel: GameViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = viewModel::onBackPressed) {
            Icon(
                Icons.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titleText(state),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if ((state.mode != GameMode.DAILY && state.mode != GameMode.VDAILY) || state.status == GameStatus.WON) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                }
                Text(
                    text = subText(state),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = viewModel::restart) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "重新开始",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun titleText(s: GameUiState): String {
    if (s.mode == GameMode.VDAILY) {
        val base = s.variant.label + " · 每日一题"
        return if (s.status == GameStatus.WON) "$base · 已完成" else base
    }
    val base = if (s.mode == GameMode.DAILY) "每日一题" else s.difficulty.label
    val full = if (s.variant == Variant.NORMAL) base else s.variant.label + " · " + base
    return if (s.status == GameStatus.WON) "$full · 已完成" else full
}

private fun subText(s: GameUiState): String = when {
    s.status == GameStatus.WON -> "用时 ${formatTime(s.elapsedSeconds)}"
    s.mode == GameMode.DAILY || s.mode == GameMode.VDAILY -> dailyLabelText()
    else -> formatTime(s.elapsedSeconds)
}

private fun chipText(s: GameUiState): String = when {
    s.status == GameStatus.WON -> "已完成"
    s.mode == GameMode.DAILY -> "每日一题"
    s.mode == GameMode.VDAILY -> s.variant.label + " · 每日一题"
    s.variant == Variant.NORMAL -> s.difficulty.label
    else -> s.variant.label + " · " + s.difficulty.label
}

private fun statusText(s: GameUiState): String = when {
    s.simActive -> "推演中 · 不计错误"
    s.errorMode == ErrorMode.FINAL -> if (s.showErrors) "错误 ${s.mistakes} 次" else "完成后检查"
    else -> "错误 ${s.mistakes} 次"
}

private fun dailyLabelText(): String {
    val d = LocalDate.now()
    val week = arrayOf("日", "一", "二", "三", "四", "五", "六")
    return "${d.monthValue}月${d.dayOfMonth}日 · 周${week[d.dayOfWeek.value % 7]}"
}

/* ================= 棋盘 ================= */

private data class CellHighlight(
    val peer: Boolean,
    val same: Boolean,
    val selected: Boolean,
    val conflict: Boolean
)

@Composable
private fun Board(
    state: GameUiState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val strongLine = MaterialTheme.colorScheme.outline
    val cells = if (state.simActive) state.simCells else state.cells
    val notes = if (state.simActive) state.simNotes else state.notes
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, strongLine, shape)
    ) {
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until 9) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (c in 0 until 9) {
                        val i = r * 9 + c
                        Cell(
                            value = cells[i],
                            notes = notes[i],
                            isGiven = i in state.givens,
                            highlight = highlightFor(state, i),
                            win = state.variant == Variant.HYPER && isHyperCell(i),
                            wrong = state.errorMode == ErrorMode.FINAL && state.showErrors &&
                                cells[i] != 0 && cells[i] != state.solution[i],
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onSelect(i) }
                        )
                    }
                }
            }
        }
        Canvas(Modifier.matchParentSize()) {
            for (i in 1..8) {
                val x = size.width * i / 9f
                val y = size.height * i / 9f
                val stroke = if (i % 3 == 0) 2.dp.toPx() else 1.dp.toPx()
                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
            }
        }
    }
}

@Composable
private fun Cell(
    value: Int,
    notes: Int,
    isGiven: Boolean,
    highlight: CellHighlight,
    win: Boolean,
    wrong: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = when {
        highlight.conflict || wrong -> MaterialTheme.colorScheme.errorContainer
        highlight.selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        highlight.same -> MaterialTheme.colorScheme.primaryContainer
        highlight.peer -> MaterialTheme.colorScheme.surfaceVariant
        win -> Color(0x1F7A7A8C)
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    highlight.conflict || wrong -> MaterialTheme.colorScheme.error
                    isGiven -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        } else if (notes != 0) {
            NotesGrid(notes)
        }
    }
}

@Composable
private fun NotesGrid(notes: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(1.dp)
    ) {
        for (r in 0 until 3) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (c in 0 until 3) {
                    val n = r * 3 + c + 1
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (notes and (1 shl (n - 1)) != 0) {
                            Text(
                                text = n.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun highlightFor(state: GameUiState, i: Int): CellHighlight {
    val s = state
    val conflict = conflictOf(s, i)
    val selected = i == s.selected
    if (s.selected == -1) {
        return CellHighlight(peer = false, same = false, selected = false, conflict = conflict)
    }
    val cells = if (s.simActive) s.simCells else s.cells
    val peer = i != s.selected && SudokuEngine.arePeers(s.variant, s.selected, i)
    val same = cells[i] != 0 && cells[i] == cells[s.selected]
    return CellHighlight(
        peer = peer && !same,
        same = same,
        selected = selected,
        conflict = conflict
    )
}

/* 超数独四窗格子：四角的 3×3 区域 */
private fun isHyperCell(i: Int): Boolean {
    val r = i / 9
    val c = i % 9
    return ((r in 1..3) || (r in 5..7)) && ((c in 1..3) || (c in 5..7))
}

private fun conflictOf(s: GameUiState, i: Int): Boolean {
    val cells = if (s.simActive) s.simCells else s.cells
    val v = cells[i]
    if (v == 0) return false
    for (p in SudokuEngine.peersOf(s.variant, i)) {
        if (cells[p] == v) return true
    }
    return false
}

/* ================= 控制区 ================= */

@Composable
private fun Controls(state: GameUiState, viewModel: GameViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ControlButton(Icons.Rounded.EditNote, "笔记", active = state.notesMode) {
            viewModel.toggleNotesMode()
        }
        ControlButton(Icons.Rounded.Undo, "撤销", active = false) {
            viewModel.undo()
        }
        ControlButton(Icons.Rounded.Backspace, "擦除", active = false) {
            viewModel.erase()
        }
        ControlButton(Icons.Rounded.Lightbulb, "提示", active = false) {
            viewModel.hint()
        }
        ControlButton(Icons.Rounded.AutoAwesome, "推演", active = state.simActive) {
            viewModel.toggleSim()
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val bg = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

@Composable
private fun NumberPad(viewModel: GameViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(padColor)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        for (n in 1..9) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable { viewModel.inputNumber(n) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = n.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
