package com.firedispatch.log.ui.screen

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.ui.navigation.Screen
import com.firedispatch.log.ui.viewmodel.MemberListViewModel
import com.firedispatch.log.ui.viewmodel.MemberWithRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberListScreen(
    navController: NavController,
    viewModel: MemberListViewModel = viewModel()
) {
    val membersWithRoles by viewModel.membersWithRoles.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("団員名簿") },
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
                    onClick = { navController.navigate(Screen.MemberEdit.route) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("名簿編集")
                }
                Button(
                    onClick = { navController.navigate(Screen.RoleAssignment.route) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("役職割り当て")
                }
            }

            // 団員リスト
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(membersWithRoles) { memberWithRole ->
                    MemberListItem(memberWithRole)
                }
            }
        }
    }
}

@Composable
fun MemberListItem(memberWithRole: MemberWithRole) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 役職
            Text(
                text = memberWithRole.roleType?.displayName ?: "未割当",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.3f),
                color = if (memberWithRole.roleType == null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            // 氏名
            Text(
                text = memberWithRole.member.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(0.3f)
            )

            // 電話番号
            Text(
                text = memberWithRole.member.phoneNumber.ifEmpty { "-" },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.4f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
