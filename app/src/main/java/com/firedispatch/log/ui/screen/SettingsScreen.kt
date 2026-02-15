package com.firedispatch.log.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.data.entity.FiscalYear
import com.firedispatch.log.ui.navigation.Screen
import com.firedispatch.log.ui.viewmodel.FiscalYearViewModel
import com.firedispatch.log.ui.viewmodel.SettingsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(),
    fiscalYearViewModel: FiscalYearViewModel = viewModel()
) {
    val context = LocalContext.current
    val organizationName by viewModel.organizationName.collectAsState()
    val allowPhoneCall by viewModel.allowPhoneCall.collectAsState()
    val fiscalYears by fiscalYearViewModel.allFiscalYears.collectAsState(initial = emptyList())
    val activeFiscalYear by fiscalYearViewModel.activeFiscalYear.collectAsState(initial = null)

    var showResetDialog by remember { mutableStateOf(false) }
    var showResetEventDialog by remember { mutableStateOf(false) }
    var showResetTransactionDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showMoveToNextYearDialog by remember { mutableStateOf(false) }
    var showMoveToNextYearConfirmDialog by remember { mutableStateOf(false) }
    var shouldSaveBeforeMove by remember { mutableStateOf(false) }
    var showAddYearDialog by remember { mutableStateOf(false) }
    var selectedYearForEdit by remember { mutableStateOf<FiscalYear?>(null) }
    var showCarryOverDialog by remember { mutableStateOf(false) }
    var showDeleteYearDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    val timestamp = dateFormat.format(Date())

    // エクスポート用ファイルピッカー
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportBackup(
                uri = it,
                onSuccess = {
                    Toast.makeText(context, "バックアップを保存しました", Toast.LENGTH_SHORT).show()
                    // 次年度移行前のバックアップの場合、確認ダイアログを表示
                    if (shouldSaveBeforeMove) {
                        showMoveToNextYearConfirmDialog = true
                    }
                },
                onError = { error ->
                    Toast.makeText(context, "エラー: $error", Toast.LENGTH_LONG).show()
                    // エラーが発生した場合はフラグをリセット
                    shouldSaveBeforeMove = false
                }
            )
        } ?: run {
            // キャンセルされた場合はフラグをリセット
            shouldSaveBeforeMove = false
        }
    }

    // インポート用ファイルピッカー
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importBackup(
                uri = it,
                onSuccess = {
                    Toast.makeText(context, "バックアップを復元しました", Toast.LENGTH_SHORT).show()
                    navController.popBackStack(navController.graph.startDestinationId, false)
                },
                onError = { error ->
                    Toast.makeText(context, "エラー: $error", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
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
            // 年度設定
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "年度設定",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        TextButton(onClick = { showAddYearDialog = true }) {
                            Text("年度追加")
                        }
                    }

                    if (fiscalYears.isEmpty()) {
                        Text(
                            text = "年度が設定されていません。\n「年度追加」から年度を追加してください。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        fiscalYears.forEach { fiscalYear ->
                            FiscalYearCompactCard(
                                fiscalYear = fiscalYear,
                                isActive = fiscalYear.id == activeFiscalYear?.id,
                                onSetActive = { fiscalYearViewModel.setActiveFiscalYear(fiscalYear.id) },
                                onEditCarryOver = {
                                    selectedYearForEdit = fiscalYear
                                    showCarryOverDialog = true
                                },
                                onDelete = {
                                    selectedYearForEdit = fiscalYear
                                    showDeleteYearDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // 分団名設定
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "分団名設定",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "PDF出力時のタイトルに使用されます",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = organizationName,
                        onValueChange = { viewModel.setOrganizationName(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("分団名") },
                        placeholder = { Text("例: ○○市消防団第1分団") },
                        singleLine = true
                    )
                }
            }

            // 背景色設定
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "背景色設定",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "画面ごとの背景色をカスタマイズできます",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { navController.navigate(Screen.BackgroundColorSetting.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("背景色を設定")
                    }
                }
            }

            // 電話発信許可設定
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "名簿から電話発信を許可",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "団員名簿の電話番号をタップして電話をかけられます",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = allowPhoneCall,
                        onCheckedChange = { viewModel.setAllowPhoneCall(it) }
                    )
                }
            }

            // 次年度に移行
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "次年度に移行",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "現在の年度を終了し、次年度に移行します。\n差引残高が次年度の繰越金として設定されます。\n団員名簿と科目設定は保持されます。\n※移行前に現在のデータをJSON形式でバックアップできます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Button(
                        onClick = { showMoveToNextYearDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("次年度に移行")
                    }
                }
            }

            // バックアップ
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "バックアップ",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportLauncher.launch("消防団記録_backup_$timestamp.json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("エクスポート")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("インポート")
                        }
                    }
                }
            }

            // データ初期化
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "データ初期化",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "選択したデータを削除します。この操作は取り消せません。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showResetEventDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("行事データ初期化")
                            }
                            Button(
                                onClick = { showResetTransactionDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("取引データ初期化")
                            }
                        }
                        Button(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("全データ初期化")
                        }
                    }
                }
            }

            // 保存ボタン
            Button(
                onClick = { showSaveDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("設定を保存")
            }
        }
    }

    // 保存確認ダイアログ
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("設定を保存") },
            text = { Text("設定を保存しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveSettings {
                            showSaveDialog = false
                            navController.navigateUp()
                        }
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 全データ初期化確認ダイアログ
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("全データ初期化") },
            text = { Text("すべてのデータ（団員・行事・出席記録）を削除します。この操作は取り消せません。本当に実行しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllData {
                            showResetDialog = false
                            navController.popBackStack(navController.graph.startDestinationId, false)
                        }
                    }
                ) {
                    Text("初期化する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 行事データ初期化確認ダイアログ
    if (showResetEventDialog) {
        AlertDialog(
            onDismissRequest = { showResetEventDialog = false },
            title = { Text("行事データ初期化") },
            text = { Text("すべての行事データと出席記録を削除します。団員データは保持されます。この操作は取り消せません。本当に実行しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetEventData {
                            showResetEventDialog = false
                            Toast.makeText(context, "行事データを初期化しました", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("初期化する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetEventDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 取引データ初期化確認ダイアログ
    if (showResetTransactionDialog) {
        AlertDialog(
            onDismissRequest = { showResetTransactionDialog = false },
            title = { Text("取引データ初期化") },
            text = { Text("すべての取引データを削除します。年度、科目設定、団員データは保持されます。この操作は取り消せません。本当に実行しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetTransactionData {
                            showResetTransactionDialog = false
                            Toast.makeText(context, "取引データを初期化しました", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("初期化する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetTransactionDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 次年度移行ダイアログ（バックアップ確認）
    if (showMoveToNextYearDialog) {
        AlertDialog(
            onDismissRequest = { showMoveToNextYearDialog = false },
            title = { Text("次年度に移行") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("次年度への移行を開始します。")
                    Text("現在のデータをJSON形式でバックアップ保存しますか？")
                    Text(
                        text = "※バックアップには全ての団員、行事、取引データが含まれます。\n※保存しない場合でも、差引残高は次年度の繰越金として引き継がれます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        shouldSaveBeforeMove = true
                        showMoveToNextYearDialog = false
                        // JSONバックアップを保存
                        exportLauncher.launch("消防団記録_年度移行前_$timestamp.json")
                    }
                ) {
                    Text("保存して移行")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showMoveToNextYearDialog = false }) {
                        Text("キャンセル")
                    }
                    TextButton(
                        onClick = {
                            shouldSaveBeforeMove = false
                            showMoveToNextYearDialog = false
                            showMoveToNextYearConfirmDialog = true
                        }
                    ) {
                        Text("保存せず移行")
                    }
                }
            }
        )
    }

    // 次年度移行確認ダイアログ
    if (showMoveToNextYearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showMoveToNextYearConfirmDialog = false },
            title = { Text("次年度移行の確認") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("以下の処理が実行されます：")
                    Text("• 差引残高を計算し、次年度の繰越金に設定")
                    Text("• 次年度をアクティブに設定")
                    Text("• 出動表データをクリア")
                    Text("• 取引データをクリア")
                    Text("• 団員名簿と科目設定は保持")
                    Text(
                        text = "\nこの操作は取り消せません。実行しますか？",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.moveToNextFiscalYear(
                            deletePreviousYearData = true,
                            onSuccess = { newYearId ->
                                showMoveToNextYearConfirmDialog = false
                                shouldSaveBeforeMove = false
                                Toast.makeText(context, "次年度への移行が完了しました", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                showMoveToNextYearConfirmDialog = false
                                shouldSaveBeforeMove = false
                                Toast.makeText(context, "エラー: $error", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Text("実行", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMoveToNextYearConfirmDialog = false
                        shouldSaveBeforeMove = false
                    }
                ) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 年度追加ダイアログ
    if (showAddYearDialog) {
        FiscalYearAddDialog(
            onDismiss = { showAddYearDialog = false },
            onConfirm = { year, startDate, endDate, carryOver ->
                fiscalYearViewModel.addFiscalYear(year, startDate, endDate, carryOver)
                showAddYearDialog = false
            },
            viewModel = fiscalYearViewModel
        )
    }

    // 繰越金編集ダイアログ
    if (showCarryOverDialog && selectedYearForEdit != null) {
        CarryOverEditDialog(
            currentAmount = selectedYearForEdit!!.carryOver,
            onDismiss = {
                showCarryOverDialog = false
                selectedYearForEdit = null
            },
            onConfirm = { newAmount ->
                fiscalYearViewModel.updateCarryOver(selectedYearForEdit!!, newAmount)
                showCarryOverDialog = false
                selectedYearForEdit = null
            }
        )
    }

    // 年度削除確認ダイアログ
    if (showDeleteYearDialog && selectedYearForEdit != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteYearDialog = false
                selectedYearForEdit = null
            },
            title = { Text("年度の削除") },
            text = { Text("${selectedYearForEdit!!.year}年度を削除しますか？\nこの年度の取引記録も全て削除されます。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        fiscalYearViewModel.deleteFiscalYear(selectedYearForEdit!!)
                        showDeleteYearDialog = false
                        selectedYearForEdit = null
                    }
                ) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteYearDialog = false
                        selectedYearForEdit = null
                    }
                ) {
                    Text("キャンセル")
                }
            }
        )
    }
}

/**
 * コンパクトな年度カード（設定画面用）
 */
@Composable
fun FiscalYearCompactCard(
    fiscalYear: FiscalYear,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onEditCarryOver: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPANESE)
    val numberFormat = NumberFormat.getNumberInstance(Locale.JAPANESE)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${fiscalYear.year}年度",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (isActive) {
                    AssistChip(
                        onClick = {},
                        label = { Text("現在", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.size(width = 60.dp, height = 28.dp)
                    )
                }
            }

            Text(
                text = "${dateFormat.format(Date(fiscalYear.startDate))} 〜 ${dateFormat.format(Date(fiscalYear.endDate))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "繰越金: ¥${numberFormat.format(fiscalYear.carryOver)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onEditCarryOver) {
                        Text("編集", style = MaterialTheme.typography.labelSmall)
                    }
                    if (!isActive) {
                        TextButton(onClick = onSetActive) {
                            Text("使用", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "削除",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
