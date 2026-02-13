package com.firedispatch.log.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.firedispatch.log.ui.components.GlassCard
import com.firedispatch.log.ui.components.LiquidGlassBackground
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.ui.navigation.Screen
import com.firedispatch.log.ui.viewmodel.DispatchTableViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchTableScreen(
    navController: NavController,
    viewModel: DispatchTableViewModel = viewModel()
) {
    val context = LocalContext.current
    val memberRows by viewModel.memberRows.collectAsState()
    val eventColumns by viewModel.eventColumns.collectAsState()
    val attendanceSummaries by viewModel.attendanceSummaries.collectAsState()
    val showSummary by viewModel.showSummary.collectAsState()
    val showCurrentYearOnly by viewModel.showCurrentYearOnly.collectAsState()
    val selectedEventId by viewModel.selectedEventId.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LiquidGlassBackground(
        modifier = Modifier.fillMaxSize(),
        primaryColor = Color(0xFFFF5722),  // ディープオレンジ
        secondaryColor = Color(0xFFFF9800), // オレンジ
        tertiaryColor = Color(0xFFE53935),  // 赤
        animated = true
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "出動表",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "戻る",
                                tint = Color.Black
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White.copy(alpha = 0.9f)
                    )
                )
            }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 上部ボタン（1行に配置）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        selectedEventId?.let {
                            navController.navigate(Screen.EventEdit.createRoute(it))
                        }
                    },
                    enabled = selectedEventId != null,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selectedEventId != null) {
                                Color.White.copy(alpha = 0.5f)
                            } else {
                                Color.Gray.copy(alpha = 0.3f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("編集", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { navController.navigate(Screen.EventEdit.createRoute(-1)) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.5f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("追加", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { showDeleteDialog = true },
                    enabled = selectedEventId != null,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selectedEventId != null) {
                                Color.White.copy(alpha = 0.5f)
                            } else {
                                Color.Gray.copy(alpha = 0.3f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("削除", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showSummary,
                        onCheckedChange = { viewModel.toggleShowSummary() }
                    )
                    Text("合計", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            // グリッド表示 - ガラススタイル
            GlassCard(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                radius = 20.dp,
                baseAlpha = 0.35f
            ) {
                DispatchTableGrid(
                    memberRows = memberRows,
                    eventColumns = eventColumns,
                    attendanceSummaries = attendanceSummaries,
                    showSummary = showSummary,
                    selectedEventId = selectedEventId,
                    onEventSelect = { eventId ->
                        viewModel.selectEvent(if (selectedEventId == eventId) null else eventId)
                    }
                )
            }
        }
        }
    }

    // 削除確認ダイアログ
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("行事削除") },
            text = { Text("選択した行事を削除しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedEventId?.let { eventId ->
                            viewModel.deleteEvent(eventId) {
                                showDeleteDialog = false
                            }
                        }
                    }
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
fun DispatchTableGrid(
    memberRows: List<com.firedispatch.log.ui.viewmodel.MemberRow>,
    eventColumns: List<com.firedispatch.log.ui.viewmodel.EventColumn>,
    attendanceSummaries: Map<Long, com.firedispatch.log.ui.viewmodel.AttendanceSummary>,
    showSummary: Boolean,
    selectedEventId: Long?,
    onEventSelect: (Long) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalScrollState = rememberScrollState()
        val verticalScrollState = rememberScrollState()

        val headerHeight = 60.dp
        val nameColumnWidth = 95.dp  // やや広く
        val summaryColumnWidth = 60.dp
        val eventColumnWidth = 80.dp

        // 利用可能な高さからヘッダー高さを引いて、行数で割る
        val availableHeight = maxHeight
        val rowCount = memberRows.size
        val cellHeight = if (rowCount > 0) {
            ((availableHeight - headerHeight) / rowCount).coerceAtLeast(30.dp)
        } else {
            36.dp
        }

    // .NET MAUIと同じ配色
    val tealColor = Color(0xFF4ECDC4)
    val selectedOrange = Color(0xFFFF9F43)
    val salmonColor = Color(0xFFFF6B6B)
    val fixedColumnGray = Color(0xFFF0F0F0) // 氏名・集計列（薄いグレー）
    val rowBackground = Color(0xFFF5F5F5) // 行の背景（セルより濃い）
    val borderColor = Color(0xFFDDDDDD)

    Column(modifier = Modifier.fillMaxSize()) {
        // ヘッダー行
        Row(modifier = Modifier.fillMaxWidth()) {
            // 左固定: 氏名ヘッダー
            Box(
                modifier = Modifier
                    .width(nameColumnWidth)
                    .height(headerHeight)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = tealColor,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "氏名",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            // 中央スクロール: 行事ヘッダー
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(headerHeight)
                    .horizontalScroll(horizontalScrollState)
            ) {
                Row {
                    eventColumns.forEach { eventColumn ->
                        val isSelected = selectedEventId == eventColumn.event.id
                        Box(
                            modifier = Modifier
                                .width(eventColumnWidth)
                                .height(headerHeight)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = if (isSelected) selectedOrange else tealColor,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onEventSelect(eventColumn.event.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    val dateFormat = SimpleDateFormat("M/d", Locale.JAPANESE)
                                    val dateStr = dateFormat.format(Date(eventColumn.event.date))
                                    Text(
                                        text = "$dateStr [${eventColumn.event.allowanceIndex}]",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = eventColumn.event.eventName,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 右固定: 集計ヘッダー
            if (showSummary) {
                Row {
                    Box(
                        modifier = Modifier
                            .width(summaryColumnWidth)
                            .height(headerHeight)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = tealColor,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "出動\n回数",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(summaryColumnWidth)
                            .height(headerHeight)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = tealColor,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "手当\n指数",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }

        // データ行
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            Column {
                memberRows.forEach { memberRow ->
                    Row(
                        modifier = Modifier.background(rowBackground)
                    ) {
                        // 左固定: 氏名
                        Box(
                            modifier = Modifier
                                .width(nameColumnWidth)
                                .height(cellHeight)
                                .padding(2.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = fixedColumnGray,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = borderColor,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = memberRow.member.name,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 中央スクロール: 出動セル
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight)
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            Row {
                                eventColumns.forEach { eventColumn ->
                                    val attended = eventColumn.attendanceMap[memberRow.member.id] ?: false
                                    Box(
                                        modifier = Modifier
                                            .width(eventColumnWidth)
                                            .height(cellHeight)
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    color = if (attended) salmonColor else Color.White,
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = borderColor,
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (attended) {
                                                Text(
                                                    text = "出動",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 右固定: 集計
                        if (showSummary) {
                            val summary = attendanceSummaries[memberRow.member.id]
                            Row {
                                Box(
                                    modifier = Modifier
                                        .width(summaryColumnWidth)
                                        .height(cellHeight)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = fixedColumnGray,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = borderColor,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${summary?.attendanceCount ?: 0}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .width(summaryColumnWidth)
                                        .height(cellHeight)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = fixedColumnGray,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = borderColor,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${summary?.allowanceTotal ?: 0}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
