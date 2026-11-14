package com.moneytracker.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.data.model.TransactionType
import com.moneytracker.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: MainViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN)

    val filteredTransactions = transactions.filter { tx ->
        val matchesQuery = searchQuery.isBlank() || tx.note.contains(searchQuery, ignoreCase = true)
        val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
        matchesQuery && matchesType
    }

    var txToDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("新增記帳") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 搜尋與篩選
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜尋備註關鍵字...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(50)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.FilterChip(
                    selected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null },
                    label = { Text("全部") }
                )
                androidx.compose.material3.FilterChip(
                    selected = selectedTypeFilter == TransactionType.EXPENSE,
                    onClick = { selectedTypeFilter = TransactionType.EXPENSE },
                    label = { Text("支出") }
                )
                androidx.compose.material3.FilterChip(
                    selected = selectedTypeFilter == TransactionType.INCOME,
                    onClick = { selectedTypeFilter = TransactionType.INCOME },
                    label = { Text("收入") }
                )
                androidx.compose.material3.FilterChip(
                    selected = selectedTypeFilter == TransactionType.TRANSFER,
                    onClick = { selectedTypeFilter = TransactionType.TRANSFER },
                    label = { Text("轉帳") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 交易列表
            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("查無符合紀錄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = com.moneytracker.app.ui.components.ListBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        val category = categories.find { it.id == tx.categoryId }
                        val account = accounts.find { it.id == tx.sourceAccountId }
                        val targetAccount = accounts.find { it.id == tx.targetAccountId }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = category?.name ?: tx.type.label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = account?.name ?: "帳戶",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (tx.type == TransactionType.TRANSFER && targetAccount != null) {
                                            Text(
                                                text = " ➔ ${targetAccount.name}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (tx.note.isNotBlank()) {
                                        Text(text = tx.note, fontSize = 13.sp)
                                    }
                                    Text(
                                        text = dateFormat.format(Date(tx.dateMillis)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val color = when (tx.type) {
                                        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                                        TransactionType.TRANSFER -> Color.DarkGray
                                    }
                                    val prefix = when (tx.type) {
                                        TransactionType.INCOME -> "+"
                                        TransactionType.EXPENSE -> "-"
                                        TransactionType.TRANSFER -> ""
                                    }
                                    val formattedAmount = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(tx.amount)
                                    Text(
                                        text = "$prefix NT$ $formattedAmount",
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        fontSize = 16.sp
                                    )
                                    if (tx.loanId == null && tx.repaymentId == null) {
                                        IconButton(onClick = { txToDelete = tx.id }) {
                                            Icon(Icons.Default.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    } else {
                                        IconButton(onClick = { viewModel.setUiMessage("借貸金流請至借貸管理頁面修改") }) {
                                            Icon(Icons.Default.Info, contentDescription = "無法直接刪除", tint = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    txToDelete?.let { id ->
        com.moneytracker.app.ui.components.ConfirmDialog(
            title = "刪除交易",
            message = "確定要刪除這筆交易嗎？",
            confirmText = "刪除",
            onConfirm = { viewModel.deleteTransaction(id) },
            onDismiss = { txToDelete = null }
        )
    }

    if (showAddDialog) {
        AddTransactionDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var selectedTypeIndex by remember { mutableStateOf(0) } // 0: 支出, 1: 收入, 2: 轉帳
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    val currentType = when (selectedTypeIndex) {
        0 -> TransactionType.EXPENSE
        1 -> TransactionType.INCOME
        else -> TransactionType.TRANSFER
    }

    val typeCategories = categories.filter { it.type == currentType }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedSourceId by remember { mutableStateOf<Long?>(null) }
    var selectedTargetId by remember { mutableStateOf<Long?>(null) }

    val selectedCategory = typeCategories.find { it.id == selectedCategoryId } ?: typeCategories.firstOrNull()
    val selectedSource = accounts.find { it.id == selectedSourceId } ?: accounts.firstOrNull()
    val selectedTarget = accounts.find { it.id == selectedTargetId }
        ?: accounts.firstOrNull { it.id != selectedSource?.id }
        ?: accounts.firstOrNull()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("新增記帳") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = selectedTypeIndex == 0,
                        onClick = { selectedTypeIndex = 0 },
                        label = { Text("支出") }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = selectedTypeIndex == 1,
                        onClick = { selectedTypeIndex = 1 },
                        label = { Text("收入") }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = selectedTypeIndex == 2,
                        onClick = { selectedTypeIndex = 2 },
                        label = { Text("轉帳") }
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { v -> if (v.length <= 12 && v.all { it.isDigit() }) amountText = v },
                    label = { Text("金額 (TWD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )

                com.moneytracker.app.ui.components.DropdownField(
                    label = if (currentType == TransactionType.TRANSFER) "轉出帳戶" else "帳戶",
                    selectedText = selectedSource?.name ?: "",
                    items = accounts,
                    itemText = { it.name },
                    onSelect = { selectedSourceId = it.id },
                    isError = selectedSource == null
                )

                if (currentType == TransactionType.TRANSFER) {
                    com.moneytracker.app.ui.components.DropdownField(
                        label = "轉入帳戶",
                        selectedText = selectedTarget?.name ?: "",
                        items = accounts,
                        itemText = { it.name },
                        onSelect = { selectedTargetId = it.id },
                        isError = selectedTarget == null
                    )
                } else {
                    com.moneytracker.app.ui.components.DropdownField(
                        label = "分類",
                        selectedText = selectedCategory?.name ?: "",
                        items = typeCategories,
                        itemText = { it.name },
                        onSelect = { selectedCategoryId = it.id }
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註 (選填)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    val sourceAcc = selectedSource
                    val targetAcc = selectedTarget
                    if (amount <= 0) {
                        errorMessage = "金額必須大於 0"
                    } else if (sourceAcc == null) {
                        errorMessage = "請選擇帳戶"
                    } else if (currentType == TransactionType.TRANSFER && targetAcc == null) {
                        errorMessage = "請選擇轉入帳戶"
                    } else if (currentType == TransactionType.TRANSFER && sourceAcc.id == targetAcc?.id) {
                        errorMessage = "轉出與轉入帳戶不能相同"
                    } else {
                        errorMessage = null
                        isSaving = true
                        viewModel.addTransaction(
                            type = currentType,
                            amount = amount,
                            dateMillis = System.currentTimeMillis(),
                            sourceAccountId = sourceAcc.id,
                            targetAccountId = if (currentType == TransactionType.TRANSFER) targetAcc?.id else null,
                            categoryId = if (currentType != TransactionType.TRANSFER) selectedCategory?.id else null,
                            note = note.trim(),
                            onSuccess = {
                                isSaving = false
                                onDismiss()
                            },
                            onError = { err ->
                                isSaving = false
                                errorMessage = err
                            }
                        )
                    }
                }
            ) {
                Text(if (isSaving) "儲存中..." else "儲存")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) { Text("取消") }
        }
    )
}
