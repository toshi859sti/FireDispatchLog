package com.firedispatch.log.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firedispatch.log.data.entity.BackgroundColorPreset
import com.firedispatch.log.ui.theme.TailwindColors
import com.firedispatch.log.ui.viewmodel.BackgroundColorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEditorScreen(
    presetId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: BackgroundColorViewModel = viewModel()
) {
    val presets by viewModel.presets.collectAsState()
    val existingPreset = presets.find { it.id == presetId }

    var presetName by remember { mutableStateOf(existingPreset?.name ?: "") }
    var color1 by remember { mutableStateOf(existingPreset?.color1 ?: "blue-500") }
    var color2 by remember { mutableStateOf(existingPreset?.color2 ?: "purple-500") }
    var color3 by remember { mutableStateOf(existingPreset?.color3 ?: "pink-500") }

    var editingColor by remember { mutableStateOf<Int?>(null) } // 1, 2, 3

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (presetId == null) "新規プリセット" else "プリセット編集") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (presetName.isNotBlank()) {
                                if (presetId == null) {
                                    viewModel.createPreset(presetName, color1, color2, color3)
                                } else {
                                    viewModel.updatePreset(
                                        BackgroundColorPreset(
                                            id = presetId,
                                            name = presetName,
                                            color1 = color1,
                                            color2 = color2,
                                            color3 = color3
                                        )
                                    )
                                }
                                onNavigateBack()
                            }
                        },
                        enabled = presetName.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, "保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 背景プレビュー（大きく表示）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                TailwindColors.getColor(color1),
                                TailwindColors.getColor(color2),
                                TailwindColors.getColor(color3)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "プレビュー",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }

            // プリセット名入力
            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text("プリセット名") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 色選択ボタン
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColorButton(
                    label = "色1",
                    colorName = color1,
                    isSelected = editingColor == 1,
                    onClick = { editingColor = if (editingColor == 1) null else 1 },
                    modifier = Modifier.weight(1f)
                )
                ColorButton(
                    label = "色2",
                    colorName = color2,
                    isSelected = editingColor == 2,
                    onClick = { editingColor = if (editingColor == 2) null else 2 },
                    modifier = Modifier.weight(1f)
                )
                ColorButton(
                    label = "色3",
                    colorName = color3,
                    isSelected = editingColor == 3,
                    onClick = { editingColor = if (editingColor == 3) null else 3 },
                    modifier = Modifier.weight(1f)
                )
            }

            // カラーピッカー
            if (editingColor != null) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "カラーを選択",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                ColorPicker(
                    selectedColor = when (editingColor) {
                        1 -> color1
                        2 -> color2
                        3 -> color3
                        else -> "blue-500"
                    },
                    onColorSelected = { newColor ->
                        when (editingColor) {
                            1 -> color1 = newColor
                            2 -> color2 = newColor
                            3 -> color3 = newColor
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
fun ColorButton(
    label: String,
    colorName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        border = if (!isSelected) ButtonDefaults.outlinedButtonBorder() else null
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TailwindColors.getColor(colorName))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = colorName,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(selectedColor.substringBefore("-")) }

    LazyColumn(modifier = modifier) {
        // カテゴリ選択
        item {
            Text(
                text = "カラーカテゴリ",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(TailwindColors.colorCategories) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = TailwindColors.getCategoryDisplayName(category),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // シェード選択
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "シェード",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(TailwindColors.colorShades.chunked(5)) { shadeRow ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shadeRow.forEach { shade ->
                    val colorName = "$selectedCategory-$shade"
                    val isSelected = colorName == selectedColor

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TailwindColors.getColor(colorName))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Gray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onColorSelected(colorName) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "選択中",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = shade,
                            color = if (shade.toInt() >= 500) Color.White else Color.Black,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}
