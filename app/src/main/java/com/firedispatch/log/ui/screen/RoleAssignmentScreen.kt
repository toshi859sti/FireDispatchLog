package com.firedispatch.log.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.data.model.RoleType
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("役職割り当て") },
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
        ) {
            // 上部ボタン
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Screen.RoleMemberCountSetting.route) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("一般団員数設定")
                }
                Button(
                    onClick = {
                        if (viewModel.canSave()) {
                            navController.navigateUp()
                        } else {
                            showSaveError = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("登録")
                }
            }

            // 役職リスト
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = slot.roleType.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.4f)
            )
            Text(
                text = slot.assignedMember?.name ?: "未割当",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(0.6f),
                color = if (slot.assignedMember == null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
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
