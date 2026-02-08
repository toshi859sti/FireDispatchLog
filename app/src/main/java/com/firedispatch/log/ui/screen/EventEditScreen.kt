package com.firedispatch.log.ui.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.JAPANESE)

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
            // 日付
            Column {
                Text(
                    text = "日付",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = date

                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val newCalendar = Calendar.getInstance()
                                newCalendar.set(year, month, dayOfMonth)
                                viewModel.setDate(newCalendar.timeInMillis)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(dateFormat.format(Date(date)))
                }
            }

            // 行事名
            OutlinedTextField(
                value = eventName,
                onValueChange = { viewModel.setEventName(it) },
                label = { Text("行事名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 手当指数
            Column {
                Text(
                    text = "手当指数",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.setAllowanceIndex(allowanceIndex - 1) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("-")
                    }
                    OutlinedTextField(
                        value = allowanceIndex.toString(),
                        onValueChange = { newValue ->
                            newValue.toIntOrNull()?.let { viewModel.setAllowanceIndex(it) }
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Button(
                        onClick = { viewModel.setAllowanceIndex(allowanceIndex + 1) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("+")
                    }
                }
                Text(
                    text = "（1〜4の範囲）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 出席者
            Column {
                Text(
                    text = "出席者",
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 4
                ) {
                    attendanceMembers.forEach { attendanceMember ->
                        FilterChip(
                            selected = attendanceMember.attended,
                            onClick = { viewModel.toggleAttendance(attendanceMember.member.id) },
                            label = { Text(attendanceMember.member.name) },
                            modifier = Modifier.size(width = 90.dp, height = 40.dp)
                        )
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
    }
}
