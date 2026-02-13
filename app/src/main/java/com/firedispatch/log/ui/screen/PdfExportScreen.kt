package com.firedispatch.log.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.ui.components.AllowanceInputDialog
import com.firedispatch.log.ui.viewmodel.PdfExportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfExportScreen(
    navController: NavController,
    viewModel: PdfExportViewModel = viewModel()
) {
    val context = LocalContext.current
    var showAllowanceDialog by remember { mutableStateOf(false) }
    var showAccountingTypeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PDF出力",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 団員名簿PDF
                Button(
                    onClick = {
                        viewModel.generateMemberListPdf(
                            onSuccess = { intent ->
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "PDFビューアーが見つかりません", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onError = { error ->
                                Toast.makeText(context, "エラー: $error", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(width = 280.dp, height = 72.dp)
                ) {
                    Text(
                        text = "団員名簿",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 出動表PDF
                Button(
                    onClick = {
                        viewModel.generateAttendanceTablePdf(
                            onSuccess = { intent ->
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "PDFビューアーが見つかりません", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onError = { error ->
                                Toast.makeText(context, "エラー: $error", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(width = 280.dp, height = 72.dp)
                ) {
                    Text(
                        text = "出動表",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 出動手当PDF
                Button(
                    onClick = {
                        showAllowanceDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(width = 280.dp, height = 72.dp)
                ) {
                    Text(
                        text = "出動手当",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 会計帳簿PDF
                Button(
                    onClick = {
                        showAccountingTypeDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(width = 280.dp, height = 72.dp)
                ) {
                    Text(
                        text = "会計帳簿",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // 出動手当の支給額入力ダイアログ
    if (showAllowanceDialog) {
        AllowanceInputDialog(
            onConfirm = { allowancePerAttendance ->
                showAllowanceDialog = false
                viewModel.generateAllowancePdf(
                    allowancePerAttendance = allowancePerAttendance,
                    onSuccess = { intent ->
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "PDFビューアーが見つかりません", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onError = { error ->
                        Toast.makeText(context, "エラー: $error", Toast.LENGTH_LONG).show()
                    }
                )
            },
            onDismiss = {
                showAllowanceDialog = false
            }
        )
    }

    // 会計帳簿種類選択ダイアログ
    if (showAccountingTypeDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAccountingTypeDialog = false },
            title = { androidx.compose.material3.Text("出力する帳簿を選択") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Text("出力する帳簿の種類を選択してください")

                    androidx.compose.material3.Button(
                        onClick = {
                            showAccountingTypeDialog = false
                            viewModel.generateAccountingSummaryPdf(
                                onSuccess = { intent ->
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "PDFビューアーが見つかりません", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onError = { error ->
                                    Toast.makeText(context, "エラー: $error", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Text("集計表")
                    }

                    androidx.compose.material3.Button(
                        onClick = {
                            showAccountingTypeDialog = false
                            viewModel.generateTransactionListPdf(
                                onSuccess = { intent ->
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "PDFビューアーが見つかりません", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onError = { error ->
                                    Toast.makeText(context, "エラー: $error", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Text("取引一覧")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showAccountingTypeDialog = false }) {
                    androidx.compose.material3.Text("キャンセル")
                }
            }
        )
    }
}
