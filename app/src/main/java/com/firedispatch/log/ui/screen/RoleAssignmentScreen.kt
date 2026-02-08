package com.firedispatch.log.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.data.model.RoleType
import com.firedispatch.log.ui.components.LiquidGlassBackground
import com.firedispatch.log.ui.navigation.Screen
import com.firedispatch.log.ui.viewmodel.RoleAssignmentViewModel
import com.firedispatch.log.ui.viewmodel.RoleSlot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleAssignmentScreen(
    navController: NavController,
    viewModel: RoleAssignmentViewModel = viewModel()
) {
    val roleSlots by viewModel.roleSlots.collectAsState()
    var selectedSlot by remember { mutableStateOf<RoleSlot?>(null) }
    var showSaveError by remember { mutableStateOf(false) }

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
                            "役職割り当て",
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
                // 上部ボタン
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { navController.navigate(Screen.RoleMemberCountSetting.route) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("一般団員数設定", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            if (viewModel.canSave()) {
                                navController.navigateUp()
                            } else {
                                showSaveError = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("登録", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                // 役職リスト
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(roleSlots) { slot ->
                        RoleSlotItem(
                            slot = slot,
                            onClick = { selectedSlot = slot }
                        )
                    }
                }
            }
        }
    }

    // 団員選択ダイアログ
    selectedSlot?.let { slot ->
        MemberSelectionDialog(
            slot = slot,
            viewModel = viewModel,
            onDismiss = { selectedSlot = null }
        )
    }

    // 保存エラーダイアログ
    if (showSaveError) {
        AlertDialog(
            onDismissRequest = { showSaveError = false },
            title = { Text("登録エラー") },
            text = { Text("すべての団員に役職を割り当ててください。") },
            confirmButton = {
                TextButton(onClick = { showSaveError = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun RoleSlotItem(
    slot: RoleSlot,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
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
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
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
                    text = slot.roleType.displayName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 団員名エリア
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Color(0xFFFFF3E8).copy(alpha = 0.85f)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = slot.assignedMember?.name ?: "未割当",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (slot.assignedMember == null) {
                        Color(0xFFE11D48)
                    } else {
                        Color.Black
                    }
                )
            }
        }
    }
}

@Composable
fun MemberSelectionDialog(
    slot: RoleSlot,
    viewModel: RoleAssignmentViewModel,
    onDismiss: () -> Unit
) {
    val availableMembers by viewModel.availableMembers.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${slot.roleType.displayName} - 団員選択") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableMembers) { member ->
                    val isAssigned = viewModel.isMemberAssigned(member.id) &&
                            slot.assignedMember?.id != member.id

                    OutlinedButton(
                        onClick = {
                            viewModel.assignMember(slot.roleType, slot.position, member.id)
                            onDismiss()
                        },
                        enabled = !isAssigned,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(member.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        },
        dismissButton = {
            if (slot.assignedMember != null) {
                TextButton(
                    onClick = {
                        viewModel.clearAssignment(slot.roleType, slot.position)
                        onDismiss()
                    }
                ) {
                    Text("クリア")
                }
            }
        }
    )
}
