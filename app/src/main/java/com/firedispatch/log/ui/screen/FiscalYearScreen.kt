package com.firedispatch.log.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.data.entity.FiscalYear
import com.firedispatch.log.ui.viewmodel.FiscalYearViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiscalYearScreen(
    navController: NavController,
    viewModel: FiscalYearViewModel = viewModel()
) {
    val fiscalYears by viewModel.allFiscalYears.collectAsState(initial = emptyList())
    val activeFiscalYear by viewModel.activeFiscalYear.collectAsState(initial = null)
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "年度・繰越金設定",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "年度追加")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (fiscalYears.isEmpty()) {
                // 年度未設定の場合
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "年度が設定されていません。\n右下の＋ボタンから年度を追加してください。",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 年度一覧
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(fiscalYears) { fiscalYear ->
                        FiscalYearCard(
                            fiscalYear = fiscalYear,
                            isActive = fiscalYear.id == activeFiscalYear?.id,
                            onSetActive = { viewModel.setActiveFiscalYear(fiscalYear.id) },
                            onUpdateCarryOver = { newAmount ->
                                viewModel.updateCarryOver(fiscalYear, newAmount)
                            },
                            onDelete = { viewModel.deleteFiscalYear(fiscalYear) }
                        )
                    }
                }
            }
        }

        // 年度追加ダイアログ
        if (showAddDialog) {
            FiscalYearAddDialog(
                onDismiss = { viewModel.hideAddDialog() },
                onConfirm = { year, startDate, endDate, carryOver ->
                    viewModel.addFiscalYear(year, startDate, endDate, carryOver)
                },
                viewModel = viewModel
            )
        }

        // エラーメッセージ
        errorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.clearErrorMessage() },
                title = { Text("エラー") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearErrorMessage() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun FiscalYearCard(
    fiscalYear: FiscalYear,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onUpdateCarryOver: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var showCarryOverDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPANESE)
    val numberFormat = NumberFormat.getNumberInstance(Locale.JAPANESE)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${fiscalYear.year}年度",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isActive) {
                    AssistChip(
                        onClick = {},
                        label = { Text("現在") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "期間: ${dateFormat.format(Date(fiscalYear.startDate))} 〜 ${dateFormat.format(Date(fiscalYear.endDate))}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "繰越金: ¥${numberFormat.format(fiscalYear.carryOver)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { showCarryOverDialog = true }) {
                    Text("編集")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isActive) {
                    Button(
                        onClick = onSetActive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("現在の年度に設定")
                    }
                }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "削除")
                }
            }
        }
    }

    // 繰越金編集ダイアログ
    if (showCarryOverDialog) {
        CarryOverEditDialog(
            currentAmount = fiscalYear.carryOver,
            onDismiss = { showCarryOverDialog = false },
            onConfirm = { newAmount ->
                onUpdateCarryOver(newAmount)
                showCarryOverDialog = false
            }
        )
    }

    // 削除確認ダイアログ
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("年度の削除") },
            text = { Text("${fiscalYear.year}年度を削除しますか？\nこの年度の取引記録も全て削除されます。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
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
fun FiscalYearAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (year: Int, startDate: Long, endDate: Long, carryOver: Int) -> Unit,
    viewModel: FiscalYearViewModel
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var year by remember { mutableStateOf(currentYear.toString()) }
    var carryOver by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("年度の追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("年度（例: 2025）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "期間: 4月1日 〜 翌年3月31日",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = carryOver,
                    onValueChange = { carryOver = it },
                    label = { Text("繰越金（円）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val yearInt = year.toIntOrNull() ?: currentYear
                    val carryOverInt = carryOver.toIntOrNull() ?: 0
                    val (startDate, endDate) = viewModel.createDefaultFiscalYearDates(yearInt)
                    onConfirm(yearInt, startDate, endDate, carryOverInt)
                }
            ) {
                Text("追加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun CarryOverEditDialog(
    currentAmount: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amount by remember { mutableStateOf(currentAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("繰越金の編集") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("繰越金（円）") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newAmount = amount.toIntOrNull() ?: 0
                    onConfirm(newAmount)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
