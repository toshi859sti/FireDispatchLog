package com.firedispatch.log.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.firedispatch.log.ui.components.LiquidGlassBackground
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.ui.viewmodel.MemberEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberEditScreen(
    navController: NavController,
    viewModel: MemberEditViewModel = viewModel()
) {
    val editRows by viewModel.editRows.collectAsState()
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
                // 不透明なTopAppBar
                TopAppBar(
                    title = {
                        Text(
                            "名簿編集",
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
            // 登録ボタン（ガラススタイル）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.5f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Button(
                    onClick = {
                        if (viewModel.hasInvalidRows()) {
                            showErrorDialog = true
                        } else {
                            viewModel.saveMembers {
                                navController.navigateUp()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("登録", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            // ヘッダー
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "氏名",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "電話番号",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
            }

            // 編集グリッド
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(editRows) { index, row ->
                    MemberEditRow(
                        index = index,
                        name = row.name,
                        phoneNumber = row.phoneNumber,
                        onNameChange = { name ->
                            viewModel.updateRow(index, name, row.phoneNumber)
                        },
                        onPhoneNumberChange = { phoneNumber ->
                            viewModel.updateRow(index, row.name, phoneNumber)
                        }
                    )
                }
            }
        }
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("入力エラー") },
            text = { Text("電話番号のみの行は登録できません。氏名を入力してください。") },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun MemberEditRow(
    index: Int,
    name: String,
    phoneNumber: String,
    onNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier
                .weight(1f)
                .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(8.dp)),
            singleLine = true,
            placeholder = { Text("${index + 1}", fontSize = 20.sp) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.95f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.95f)
            )
        )
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            modifier = Modifier
                .weight(1f)
                .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(8.dp)),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            placeholder = { Text("-", fontSize = 20.sp) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.95f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.95f)
            )
        )
    }
}
