package com.firedispatch.log.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.ui.components.LiquidGlassBackground
import com.firedispatch.log.ui.viewmodel.RoleCountItem
import com.firedispatch.log.ui.viewmodel.RoleMemberCountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleMemberCountSettingScreen(
    navController: NavController,
    viewModel: RoleMemberCountViewModel = viewModel()
) {
    val roleCounts by viewModel.roleCounts.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val targetCount by viewModel.targetCount.collectAsState()
    val isValid by viewModel.isValid.collectAsState()
    var showErrorDialog by remember { mutableStateOf(false) }

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
                            "一般団員数設定",
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
                // 合計表示（ガラススタイル）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isValid) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        Color.White.copy(alpha = 0.6f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFE11D48).copy(alpha = 0.3f),
                                        Color(0xFFE11D48).copy(alpha = 0.5f)
                                    )
                                )
                            }
                        )
                        .graphicsLayer {
                            shadowElevation = 10.dp.toPx()
                            shape = RoundedCornerShape(20.dp)
                            ambientShadowColor = Color.Black.copy(alpha = 0.15f)
                            spotShadowColor = Color.Black.copy(alpha = 0.2f)
                        }
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "合計",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "$totalCount / $targetCount",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        // 目標数設定
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "目標:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Button(
                                onClick = { viewModel.setTargetCount(targetCount - 1) },
                                modifier = Modifier.size(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE11D48),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                enabled = targetCount > 5
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "$targetCount",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.width(50.dp),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.setTargetCount(targetCount + 1) },
                                modifier = Modifier.size(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE11D48),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                enabled = targetCount < 11
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!isValid) {
                            Text(
                                text = "合計を${targetCount}にしてください",
                                fontSize = 14.sp,
                                color = Color(0xFFE11D48)
                            )
                        }
                    }
                }

            // 団員数設定リスト
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(roleCounts) { item ->
                    RoleCountItem(
                        item = item,
                        onCountChange = { newCount ->
                            viewModel.updateCount(item.roleType, newCount)
                        }
                    )
                }
            }

                // 登録ボタン（ガラススタイル）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isValid) {
                                Color.White.copy(alpha = 0.5f)
                            } else {
                                Color.Gray.copy(alpha = 0.3f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Button(
                        onClick = {
                            viewModel.saveCounts(
                                onSuccess = { navController.navigateUp() },
                                onError = { showErrorDialog = true }
                            )
                        },
                        enabled = isValid,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("登録", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("登録エラー") },
            text = { Text("合計を11にしてから登録してください。") },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun RoleCountItem(
    item: RoleCountItem,
    onCountChange: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.55f)
                    )
                )
            )
            .graphicsLayer {
                shadowElevation = 10.dp.toPx()
                shape = RoundedCornerShape(20.dp)
                ambientShadowColor = Color.Black.copy(alpha = 0.15f)
                spotShadowColor = Color.Black.copy(alpha = 0.2f)
            }
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(0f, size.height - stroke),
                    end = Offset(size.width, size.height - stroke),
                    strokeWidth = stroke
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 役職バッジ
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE11D48))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.roleType.displayName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // カウンター部分（ベージュ背景）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFF3E8).copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onCountChange(item.count - 1) },
                        modifier = Modifier.size(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE11D48),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = item.count.toString(),
                        onValueChange = { newValue ->
                            newValue.toIntOrNull()?.let { onCountChange(it) }
                        },
                        modifier = Modifier
                            .size(width = 70.dp, height = 56.dp)
                            .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(8.dp)),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.95f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.95f)
                        )
                    )

                    Button(
                        onClick = { onCountChange(item.count + 1) },
                        modifier = Modifier.size(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE11D48),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
