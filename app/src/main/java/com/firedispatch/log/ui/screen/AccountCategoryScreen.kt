package com.firedispatch.log.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.firedispatch.log.data.entity.AccountCategory
import com.firedispatch.log.data.entity.AccountSubCategory
import com.firedispatch.log.ui.viewmodel.AccountCategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountCategoryScreen(
    navController: NavController,
    viewModel: AccountCategoryViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val incomeCategories by viewModel.incomeCategories.collectAsState(initial = emptyList())
    val expenseCategories by viewModel.expenseCategories.collectAsState(initial = emptyList())
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "科目設定",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "科目追加")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // タブ
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("収入科目") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("支出科目") }
                )
            }

            // 科目一覧
            val categories = if (selectedTab == 0) incomeCategories else expenseCategories

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    CategoryCard(
                        category = category,
                        viewModel = viewModel,
                        onUpdate = { newName ->
                            viewModel.updateCategory(category, newName)
                        },
                        onDelete = {
                            viewModel.deleteCategory(category)
                        }
                    )
                }
            }
        }

        // 科目追加ダイアログ
        if (showAddDialog) {
            CategoryAddDialog(
                isIncome = selectedTab == 0,
                onDismiss = { showAddDialog = false },
                onConfirm = { name ->
                    viewModel.addCategory(name, if (selectedTab == 0) 1 else 0)
                    showAddDialog = false
                }
            )
        }

        // エラーメッセージ
        errorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.clearErrorMessage() },
                title = { Text("エラー") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearErrorMessage() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun CategoryCard(
    category: AccountCategory,
    viewModel: AccountCategoryViewModel,
    onUpdate: (String) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddSubDialog by remember { mutableStateOf(false) }

    val subCategories by viewModel.getSubCategories(category.id).collectAsState(initial = emptyList())
    val isEditable = category.isEditable == 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEditable)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEditable)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    if (subCategories.isNotEmpty() || !isEditable) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "閉じる" else "展開"
                            )
                        }
                    }

                    // 補助科目追加ボタン
                    IconButton(onClick = { showAddSubDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "補助科目追加")
                    }

                    if (isEditable) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "編集")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "削除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // 補助科目一覧
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (subCategories.isEmpty()) {
                        Text(
                            text = "補助科目なし",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    } else {
                        subCategories.forEach { subCategory ->
                            SubCategoryItem(
                                subCategory = subCategory,
                                onUpdate = { newName ->
                                    viewModel.updateSubCategory(subCategory, newName)
                                },
                                onDelete = {
                                    viewModel.deleteSubCategory(subCategory)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 編集ダイアログ
    if (showEditDialog) {
        CategoryEditDialog(
            currentName = category.name,
            onDismiss = { showEditDialog = false },
            onConfirm = { newName ->
                onUpdate(newName)
                showEditDialog = false
            }
        )
    }

    // 削除確認ダイアログ
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("科目の削除") },
            text = { Text("「${category.name}」を削除しますか？\n補助科目も全て削除されます。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 補助科目追加ダイアログ
    if (showAddSubDialog) {
        SubCategoryAddDialog(
            categoryName = category.name,
            onDismiss = { showAddSubDialog = false },
            onConfirm = { name ->
                viewModel.addSubCategory(category.id, name)
                showAddSubDialog = false
            }
        )
    }
}

@Composable
fun SubCategoryItem(
    subCategory: AccountSubCategory,
    onUpdate: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "　• ${subCategory.name}",
            style = MaterialTheme.typography.bodyMedium
        )

        Row {
            IconButton(onClick = { showEditDialog = true }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "編集",
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // 編集ダイアログ
    if (showEditDialog) {
        CategoryEditDialog(
            currentName = subCategory.name,
            onDismiss = { showEditDialog = false },
            onConfirm = { newName ->
                onUpdate(newName)
                showEditDialog = false
            },
            title = "補助科目の編集"
        )
    }

    // 削除確認ダイアログ
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("補助科目の削除") },
            text = { Text("「${subCategory.name}」を削除しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
fun CategoryAddDialog(
    isIncome: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${if (isIncome) "収入" else "支出"}科目の追加") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("科目名") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("追加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun SubCategoryAddDialog(
    categoryName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("「$categoryName」の補助科目追加") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("補助科目名") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("追加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun CategoryEditDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String = "科目の編集"
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名前") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
