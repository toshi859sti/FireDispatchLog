package com.firedispatch.log.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firedispatch.log.data.entity.BackgroundColorPreset
import com.firedispatch.log.ui.theme.TailwindColors
import com.firedispatch.log.ui.viewmodel.BackgroundColorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundColorSettingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPresetEditor: (Long?) -> Unit,
    viewModel: BackgroundColorViewModel = viewModel()
) {
    val presets by viewModel.presets.collectAsState()
    val mappings by viewModel.mappings.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<BackgroundColorPreset?>(null) }
    var showScreenMappingDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("背景色設定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                }
            )
        },
        floatingActionButton = {
            if (presets.size < 10) {
                FloatingActionButton(onClick = { onNavigateToPresetEditor(null) }) {
                    Icon(Icons.Default.Add, "新規プリセット")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 画面マッピング設定ボタン
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showScreenMappingDialog = true }
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
                                text = "画面ごとの背景色設定",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${mappings.size} 画面に設定済み",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // プリセット一覧
            item {
                Text(
                    text = "プリセット一覧 (${presets.size}/10)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(presets) { preset ->
                PresetCard(
                    preset = preset,
                    onEdit = { onNavigateToPresetEditor(preset.id) },
                    onDelete = { showDeleteDialog = preset },
                    usedByScreens = mappings.count { it.presetId == preset.id }
                )
            }

            if (presets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "プリセットがありません\n右下の + ボタンから追加してください",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // 削除確認ダイアログ
    showDeleteDialog?.let { preset ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("プリセット削除") },
            text = { Text("「${preset.name}」を削除しますか？\nこのプリセットを使用している画面の設定も解除されます。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePreset(preset)
                        showDeleteDialog = null
                    }
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 画面マッピング設定ダイアログ
    if (showScreenMappingDialog) {
        ScreenMappingDialog(
            presets = presets,
            mappings = mappings,
            onSetMapping = { screen, presetId -> viewModel.setScreenPreset(screen, presetId) },
            onRemoveMapping = { screen -> viewModel.removeScreenPreset(screen) },
            onDismiss = { showScreenMappingDialog = false }
        )
    }
}

@Composable
fun PresetCard(
    preset: BackgroundColorPreset,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    usedByScreens: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (usedByScreens > 0) "$usedByScreens 画面で使用中" else "未使用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "削除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // プレビュー
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                TailwindColors.getColor(preset.color1),
                                TailwindColors.getColor(preset.color2),
                                TailwindColors.getColor(preset.color3)
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // カラー名表示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ColorChip(preset.color1)
                ColorChip(preset.color2)
                ColorChip(preset.color3)
            }
        }
    }
}

@Composable
fun ColorChip(colorName: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = colorName,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenMappingDialog(
    presets: List<BackgroundColorPreset>,
    mappings: List<com.firedispatch.log.data.entity.ScreenBackgroundMapping>,
    onSetMapping: (String, Long) -> Unit,
    onRemoveMapping: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val screens = listOf(
        "menu" to "メニュー",
        "member_list" to "団員一覧",
        "dispatch_table" to "出動表",
        "event_edit" to "行事編集",
        "settings" to "設定",
        "accounting_menu" to "会計メニュー",
        "transaction_entry" to "取引記入",
        "pdf_export" to "PDF出力"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("画面ごとの背景色設定") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(screens) { (screenName, displayName) ->
                    val currentMapping = mappings.find { it.screenName == screenName }
                    val currentPreset = presets.find { it.id == currentMapping?.presetId }

                    var expanded by remember { mutableStateOf(false) }

                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleSmall
                        )
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = currentPreset?.name ?: "未設定",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("未設定") },
                                    onClick = {
                                        onRemoveMapping(screenName)
                                        expanded = false
                                    }
                                )
                                presets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = { Text(preset.name) },
                                        onClick = {
                                            onSetMapping(screenName, preset.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}
