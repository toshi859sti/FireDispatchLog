package com.firedispatch.log.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.ui.viewmodel.EventEditViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventEditScreen(
    navController: NavController,
    eventId: Long,
    viewModel: EventEditViewModel = viewModel()
) {
    val context = LocalContext.current
    val date by viewModel.date.collectAsState()
    val eventName by viewModel.eventName.collectAsState()
    val allowanceIndex by viewModel.allowanceIndex.collectAsState()
    val attendanceMembers by viewModel.attendanceMembers.collectAsState()
    val fiscalYearStart by viewModel.fiscalYearStart.collectAsState()
    val fiscalYearEnd by viewModel.fiscalYearEnd.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis in fiscalYearStart..fiscalYearEnd
            }
        }
    )

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    val dateFormat = SimpleDateFormat("M月d日(E)", Locale.JAPANESE)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (eventId == -1L) "行事追加" else "行事編集") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1行目：日付と手当指数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 日付
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "日付",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        OutlinedTextField(
                            value = dateFormat.format(Date(date)),
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "カレンダー"
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 手当指数
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "手当指数",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        (1..3).forEach { index ->
                            SegmentedButton(
                                selected = allowanceIndex == index,
                                onClick = { viewModel.setAllowanceIndex(index) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index - 1,
                                    count = 3
                                )
                            ) {
                                Text(
                                    text = "$index",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (allowanceIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // 2行目：行事名
            Column {
                Text(
                    text = "行事名",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = eventName,
                    onValueChange = { viewModel.setEventName(it) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // 出席者（3列均等幅表示）
            Column {
                Text(
                    text = "出席者",
                    style = MaterialTheme.typography.titleMedium
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    attendanceMembers.chunked(3).forEach { rowMembers ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowMembers.forEach { attendanceMember ->
                                val isSelected = attendanceMember.attended
                                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                                    // ボタン幅とテキスト長に応じて動的にフォントサイズを決定
                                    val memberName = attendanceMember.member.name
                                    val fontSize = when {
                                        // テキストが短い（3文字以下）かつ幅が広い場合
                                        memberName.length <= 3 && maxWidth > 100.dp -> 20.sp
                                        // テキストが4-5文字の場合
                                        memberName.length <= 5 -> 18.sp
                                        // テキストが長い場合
                                        else -> 16.sp
                                    }

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.toggleAttendance(attendanceMember.member.id) },
                                        label = {
                                            Text(
                                                memberName,
                                                fontSize = fontSize,
                                                color = if (isSelected) Color.White else Color.Black,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                            containerColor = Color.LightGray,
                                            selectedContainerColor = Color(0xFFFF6B6B),
                                            labelColor = Color.Black,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                            // 最後の行で3列に満たない場合は空白で埋める
                            repeat(3 - rowMembers.size) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // ボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("中止")
                }
                Button(
                    onClick = {
                        viewModel.saveEvent {
                            navController.navigateUp()
                        }
                    },
                    enabled = viewModel.isValid(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("決定")
                }
            }
        }

        // Material3 DatePicker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { selectedDate ->
                                if (selectedDate in fiscalYearStart..fiscalYearEnd) {
                                    viewModel.setDate(selectedDate)
                                }
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("キャンセル")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    title = {
                        Text(
                            text = "日付を選択",
                            modifier = Modifier.padding(16.dp)
                        )
                    },
                    headline = {
                        Text(
                            text = datePickerState.selectedDateMillis?.let {
                                SimpleDateFormat("yyyy年M月d日(E)", Locale.JAPANESE).format(Date(it))
                            } ?: "日付を選択してください",
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        )
                    }
                )
            }
        }
    }
}
