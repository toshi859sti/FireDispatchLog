package com.firedispatch.log.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.data.entity.AccountCategory
import com.firedispatch.log.data.entity.AccountSubCategory
import com.firedispatch.log.data.entity.FiscalYear
import com.firedispatch.log.data.entity.Transaction
import com.firedispatch.log.ui.viewmodel.TransactionEntryViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryScreen(
    navController: NavController,
    viewModel: TransactionEntryViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: 収入, 1: 支出, 2: 旅行関連
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showTravelPeriodDialog by remember { mutableStateOf(false) }
    var selectedTransactionId by remember { mutableStateOf<Long?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val activeFiscalYear by viewModel.activeFiscalYear.collectAsState(initial = null)
    val incomeCategories by viewModel.incomeCategories.collectAsState(initial = emptyList())
    val expenseCategories by viewModel.expenseCategories.collectAsState(initial = emptyList())
    val errorMessage by viewModel.errorMessage.collectAsState()

    val travelStartDate = activeFiscalYear?.travelStartDate

    val transactions by viewModel.getTransactionsByFiscalYear(
        activeFiscalYear?.id ?: 0L
    ).collectAsState(initial = emptyList())

    // 旅行関連の科目ID
    val travelCategoryId = expenseCategories.find { it.name == "旅行関連" }?.id

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "取引記入",
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // タブ
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        selectedTransactionId = null
                    },
                    text = { Text("収入") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        selectedTransactionId = null
                    },
                    text = { Text("支出") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        selectedTransactionId = null
                    },
                    text = { Text("旅行関連") }
                )
            }

            // 新規/編集/削除ボタン
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新規")
                }
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = selectedTransactionId != null
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("編集")
                }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = selectedTransactionId != null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("削除")
                }
            }

            // 旅行期間設定ボタン（旅行関連タブのみ）
            if (selectedTab == 2) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "旅行期間",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (travelStartDate != null) {
                                val dateFormat = SimpleDateFormat("MM/dd", Locale.JAPAN)
                                val day1 = dateFormat.format(Date(travelStartDate!!))
                                val day2 = dateFormat.format(Date(travelStartDate!! + 86400000L)) // +1日
                                Text(
                                    text = "$day1 - $day2",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Text(
                                    text = "未設定",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Button(onClick = { showTravelPeriodDialog = true }) {
                            Text(if (travelStartDate != null) "変更" else "設定")
                        }
                    }
                }
            }

            // 取引一覧
            val filteredTransactions = when (selectedTab) {
                0 -> transactions.filter { it.isIncome == 1 } // 収入
                1 -> transactions.filter { it.isIncome == 0 && it.categoryId != travelCategoryId } // 支出（旅行関連以外）
                2 -> transactions.filter { it.isIncome == 0 && it.categoryId == travelCategoryId } // 旅行関連
                else -> emptyList()
            }

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "取引がありません",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // グリッドヘッダー
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF4ECDC4))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "日付",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        text = "科目",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(2.5f)
                    )
                    Text(
                        text = "金額",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredTransactions) { transaction ->
                        TransactionGridRow(
                            transaction = transaction,
                            categories = if (selectedTab == 0) incomeCategories else expenseCategories,
                            viewModel = viewModel,
                            isSelected = transaction.id == selectedTransactionId,
                            onClick = {
                                selectedTransactionId = if (selectedTransactionId == transaction.id) {
                                    null // 選択解除
                                } else {
                                    transaction.id // 選択
                                }
                            }
                        )
                    }
                }
            }
        }

        // 取引追加ダイアログ
        if (showAddDialog && activeFiscalYear != null) {
            if (selectedTab == 2 && travelCategoryId != null) {
                // 旅行関連専用ダイアログ
                TravelTransactionAddDialog(
                    fiscalYear = activeFiscalYear!!,
                    travelCategoryId = travelCategoryId,
                    travelStartDate = travelStartDate,
                    viewModel = viewModel,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { date, subCategoryId, amount, memo, continueAdding ->
                        viewModel.addTransaction(
                            fiscalYearId = activeFiscalYear!!.id,
                            isIncome = false,
                            date = date,
                            categoryId = travelCategoryId,
                            subCategoryId = subCategoryId,
                            amount = amount,
                            memo = memo
                        )
                        if (!continueAdding) {
                            showAddDialog = false
                        }
                    }
                )
            } else {
                // 通常の取引追加ダイアログ
                TransactionAddDialog(
                    fiscalYear = activeFiscalYear!!,
                    isIncome = selectedTab == 0,
                    categories = if (selectedTab == 0) incomeCategories else expenseCategories,
                    viewModel = viewModel,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { date, categoryId, subCategoryId, amount, memo ->
                        viewModel.addTransaction(
                            fiscalYearId = activeFiscalYear!!.id,
                            isIncome = selectedTab == 0,
                            date = date,
                            categoryId = categoryId,
                            subCategoryId = subCategoryId,
                            amount = amount,
                            memo = memo
                        )
                        showAddDialog = false
                    }
                )
            }
        }

        // 旅行期間設定ダイアログ
        if (showTravelPeriodDialog && activeFiscalYear != null) {
            TravelPeriodDialog(
                fiscalYear = activeFiscalYear!!,
                currentStartDate = travelStartDate,
                onDismiss = { showTravelPeriodDialog = false },
                onConfirm = { startDate ->
                    viewModel.setTravelStartDate(activeFiscalYear!!.id, startDate)
                    showTravelPeriodDialog = false
                }
            )
        }

        // 編集ダイアログ
        if (showEditDialog && activeFiscalYear != null && selectedTransactionId != null) {
            val selectedTransaction = transactions.find { it.id == selectedTransactionId }
            selectedTransaction?.let { transaction ->
                TransactionEditDialog(
                    fiscalYear = activeFiscalYear!!,
                    transaction = transaction,
                    categories = if (selectedTab == 0) incomeCategories else expenseCategories,
                    viewModel = viewModel,
                    onDismiss = { showEditDialog = false },
                    onConfirm = { date, categoryId, subCategoryId, amount, memo ->
                        viewModel.updateTransaction(
                            transaction.copy(
                                date = date,
                                categoryId = categoryId,
                                subCategoryId = subCategoryId,
                                amount = amount,
                                memo = memo
                            )
                        )
                        showEditDialog = false
                        selectedTransactionId = null
                    }
                )
            }
        }

        // 削除確認ダイアログ
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("取引の削除") },
                text = { Text("選択した取引を削除しますか？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            transactions.find { it.id == selectedTransactionId }?.let { transaction ->
                                viewModel.deleteTransaction(transaction)
                                selectedTransactionId = null
                            }
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
fun TransactionGridRow(
    transaction: Transaction,
    categories: List<AccountCategory>,
    viewModel: TransactionEntryViewModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val category = categories.find { it.id == transaction.categoryId }
    val subCategories by viewModel.getSubCategories(transaction.categoryId).collectAsState(initial = emptyList())
    val subCategory = subCategories.find { it.id == transaction.subCategoryId }

    val dateFormat = SimpleDateFormat("MM/dd", Locale.JAPAN)
    val numberFormat = NumberFormat.getNumberInstance(Locale.JAPAN)

    // 科目名（補助科目がある場合は結合）
    val categoryText = if (subCategory != null) {
        "${category?.name ?: "不明"} - ${subCategory.name}"
    } else {
        category?.name ?: "不明"
    }

    val hasMemo = transaction.memo.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isSelected) Color(0xFFFF9F43).copy(alpha = 0.3f)
                else Color.White
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = 12.dp,
                vertical = if (hasMemo) 8.dp else 12.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateFormat.format(Date(transaction.date)),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.8f)
        )

        // 科目列（メモがある場合は縦に並べる）
        Column(
            modifier = Modifier.weight(2.5f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = categoryText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            if (hasMemo) {
                Text(
                    text = transaction.memo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.9f
                )
            }
        }

        Text(
            text = "¥${numberFormat.format(transaction.amount)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (transaction.isIncome == 1)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionAddDialog(
    fiscalYear: FiscalYear,
    isIncome: Boolean,
    categories: List<AccountCategory>,
    viewModel: TransactionEntryViewModel,
    onDismiss: () -> Unit,
    onConfirm: (date: Long, categoryId: Long, subCategoryId: Long?, amount: Int, memo: String) -> Unit
) {
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedSubCategoryId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedSubCategory by remember { mutableStateOf(false) }

    val subCategories by viewModel.getSubCategories(
        selectedCategoryId ?: 0L
    ).collectAsState(initial = emptyList())

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${if (isIncome) "収入" else "支出"}の追加") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 日付選択
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "日付: ${dateFormat.format(Date(selectedDate))}")
                }

                // 科目選択
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("科目") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    selectedSubCategoryId = null
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                // 補助科目選択（補助科目がある場合のみ表示）
                if (subCategories.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedSubCategory,
                        onExpandedChange = { expandedSubCategory = !expandedSubCategory }
                    ) {
                        OutlinedTextField(
                            value = subCategories.find { it.id == selectedSubCategoryId }?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("補助科目（任意）") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubCategory) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSubCategory,
                            onDismissRequest = { expandedSubCategory = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("（なし）") },
                                onClick = {
                                    selectedSubCategoryId = null
                                    expandedSubCategory = false
                                }
                            )
                            subCategories.forEach { subCategory ->
                                DropdownMenuItem(
                                    text = { Text(subCategory.name) },
                                    onClick = {
                                        selectedSubCategoryId = subCategory.id
                                        expandedSubCategory = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 金額入力
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() } },
                    label = { Text("金額") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // メモ入力
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("メモ（任意）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedCategoryId != null && amount.isNotEmpty()) {
                        onConfirm(
                            selectedDate,
                            selectedCategoryId!!,
                            selectedSubCategoryId,
                            amount.toIntOrNull() ?: 0,
                            memo
                        )
                    }
                },
                enabled = selectedCategoryId != null && amount.isNotEmpty()
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

    // DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // 年度の範囲内のみ選択可能
                    return utcTimeMillis >= fiscalYear.startDate &&
                           utcTimeMillis <= fiscalYear.endDate
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = it
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
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditDialog(
    fiscalYear: FiscalYear,
    transaction: Transaction,
    categories: List<AccountCategory>,
    viewModel: TransactionEntryViewModel,
    onDismiss: () -> Unit,
    onConfirm: (date: Long, categoryId: Long, subCategoryId: Long?, amount: Int, memo: String) -> Unit
) {
    var selectedDate by remember { mutableStateOf(transaction.date) }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var selectedSubCategoryId by remember { mutableStateOf(transaction.subCategoryId) }
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var memo by remember { mutableStateOf(transaction.memo) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedSubCategory by remember { mutableStateOf(false) }

    val subCategories by viewModel.getSubCategories(
        selectedCategoryId
    ).collectAsState(initial = emptyList())

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("取引の編集") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 日付選択
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "日付: ${dateFormat.format(Date(selectedDate))}")
                }

                // 科目選択
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("科目") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    selectedSubCategoryId = null
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                // 補助科目選択（補助科目がある場合のみ表示）
                if (subCategories.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedSubCategory,
                        onExpandedChange = { expandedSubCategory = !expandedSubCategory }
                    ) {
                        OutlinedTextField(
                            value = subCategories.find { it.id == selectedSubCategoryId }?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("補助科目（任意）") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubCategory) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSubCategory,
                            onDismissRequest = { expandedSubCategory = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("（なし）") },
                                onClick = {
                                    selectedSubCategoryId = null
                                    expandedSubCategory = false
                                }
                            )
                            subCategories.forEach { subCategory ->
                                DropdownMenuItem(
                                    text = { Text(subCategory.name) },
                                    onClick = {
                                        selectedSubCategoryId = subCategory.id
                                        expandedSubCategory = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 金額入力
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() } },
                    label = { Text("金額") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // メモ入力
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("メモ（任意）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (amount.isNotEmpty()) {
                        onConfirm(
                            selectedDate,
                            selectedCategoryId,
                            selectedSubCategoryId,
                            amount.toIntOrNull() ?: 0,
                            memo
                        )
                    }
                },
                enabled = amount.isNotEmpty()
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

    // DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // 年度の範囲内のみ選択可能
                    return utcTimeMillis >= fiscalYear.startDate &&
                           utcTimeMillis <= fiscalYear.endDate
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = it
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
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelPeriodDialog(
    fiscalYear: FiscalYear,
    currentStartDate: Long?,
    onDismiss: () -> Unit,
    onConfirm: (startDate: Long) -> Unit
) {
    var selectedDate by remember { mutableStateOf(currentStartDate ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("旅行期間設定") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "旅行開始日（1日目）を選択してください",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "開始日: ${dateFormat.format(Date(selectedDate))}")
                }
                Text(
                    text = "2日目: ${dateFormat.format(Date(selectedDate + 86400000L))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedDate) }) {
                Text("設定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )

    // DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= fiscalYear.startDate &&
                           utcTimeMillis <= fiscalYear.endDate
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = it
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
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelTransactionAddDialog(
    fiscalYear: FiscalYear,
    travelCategoryId: Long,
    travelStartDate: Long?,
    viewModel: TransactionEntryViewModel,
    onDismiss: () -> Unit,
    onConfirm: (date: Long, subCategoryId: Long?, amount: Int, memo: String, continueAdding: Boolean) -> Unit
) {
    var selectedDateType by remember { mutableStateOf(0) } // 0: 1日目, 1: 2日目, 2: 別の日付
    var customDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedSubCategoryId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedSubCategory by remember { mutableStateOf(false) }

    val subCategories by viewModel.getSubCategories(travelCategoryId).collectAsState(initial = emptyList())

    val dateFormat = SimpleDateFormat("MM/dd", Locale.JAPAN)

    // 選択された日付を計算
    val selectedDate = when (selectedDateType) {
        0 -> travelStartDate ?: System.currentTimeMillis()
        1 -> (travelStartDate ?: System.currentTimeMillis()) + 86400000L
        else -> customDate
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("旅行取引の追加") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 日付選択（ラジオボタン）
                Text(
                    text = "日付",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (travelStartDate != null) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDateType == 0,
                                onClick = { selectedDateType = 0 }
                            )
                            Text(
                                text = "1日目（${dateFormat.format(Date(travelStartDate))}）",
                                modifier = Modifier.clickable { selectedDateType = 0 }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDateType == 1,
                                onClick = { selectedDateType = 1 }
                            )
                            Text(
                                text = "2日目（${dateFormat.format(Date(travelStartDate + 86400000L))}）",
                                modifier = Modifier.clickable { selectedDateType = 1 }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "旅行期間を先に設定してください",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedButton(
                    onClick = {
                        selectedDateType = 2
                        showDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selectedDateType == 2) "別の日付: ${dateFormat.format(Date(customDate))}" else "別の日付を選択")
                }

                Divider()

                // 補助科目選択
                ExposedDropdownMenuBox(
                    expanded = expandedSubCategory,
                    onExpandedChange = { expandedSubCategory = !expandedSubCategory }
                ) {
                    OutlinedTextField(
                        value = subCategories.find { it.id == selectedSubCategoryId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("補助科目 *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSubCategory,
                        onDismissRequest = { expandedSubCategory = false }
                    ) {
                        subCategories.forEach { subCategory ->
                            DropdownMenuItem(
                                text = { Text(subCategory.name) },
                                onClick = {
                                    selectedSubCategoryId = subCategory.id
                                    expandedSubCategory = false
                                }
                            )
                        }
                    }
                }

                // 金額入力
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() } },
                    label = { Text("金額 *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // メモ入力
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("メモ（任意）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        if (selectedSubCategoryId != null && amount.isNotEmpty() && travelStartDate != null) {
                            onConfirm(
                                selectedDate,
                                selectedSubCategoryId,
                                amount.toIntOrNull() ?: 0,
                                memo,
                                true // 連続入力
                            )
                            // フォームをリセット
                            selectedSubCategoryId = null
                            amount = ""
                            memo = ""
                        }
                    },
                    enabled = selectedSubCategoryId != null && amount.isNotEmpty() && travelStartDate != null
                ) {
                    Text("保存して次へ")
                }
                TextButton(
                    onClick = {
                        if (selectedSubCategoryId != null && amount.isNotEmpty() && travelStartDate != null) {
                            onConfirm(
                                selectedDate,
                                selectedSubCategoryId,
                                amount.toIntOrNull() ?: 0,
                                memo,
                                false // 終了
                            )
                        }
                    },
                    enabled = selectedSubCategoryId != null && amount.isNotEmpty() && travelStartDate != null
                ) {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )

    // DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= fiscalYear.startDate &&
                           utcTimeMillis <= fiscalYear.endDate
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            customDate = it
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
            DatePicker(state = datePickerState)
        }
    }
}
