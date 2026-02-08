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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.ui.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val fiscalYear by viewModel.fiscalYear.collectAsState()
    val organizationName by viewModel.organizationName.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

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
                },
                onError = { error ->
                    Toast.makeText(context, "エラー: $error", Toast.LENGTH_LONG).show()
                }
            )
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
            // 集計年度設定
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "集計年度設定",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "会計年度（4月〜翌3月）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.setFiscalYear(fiscalYear - 1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text("-")
                        }
                        OutlinedTextField(
                            value = fiscalYear.toString(),
                            onValueChange = { newValue ->
                                newValue.toIntOrNull()?.let { viewModel.setFiscalYear(it) }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("年度") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Button(
                            onClick = { viewModel.setFiscalYear(fiscalYear + 1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text("+")
                        }
                    }
                    Text(
                        text = "令和${fiscalYear - 2018}年度",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                        text = "すべてのデータを削除します。この操作は取り消せません。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("データを初期化")
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

    // データ初期化確認ダイアログ
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("データ初期化") },
            text = { Text("すべてのデータを削除します。この操作は取り消せません。本当に実行しますか？") },
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
}
