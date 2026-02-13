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
    val organizationName by viewModel.organizationName.collectAsState()
    val allowPhoneCall by viewModel.allowPhoneCall.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showResetEventDialog by remember { mutableStateOf(false) }
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
            // 年度設定への案内
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
                    Text(
                        text = "年度設定",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "年度の設定は「会計」→「年度・繰越金設定」から行ってください。\nPDF出力時にはアクティブな年度が使用されます。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                            onClick = { showResetDialog = true },
                            modifier = Modifier.weight(1f)
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
}
