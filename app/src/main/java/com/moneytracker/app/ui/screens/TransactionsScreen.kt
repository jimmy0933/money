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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增記帳")
            }
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { selectedTypeFilter = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTypeFilter == null) MaterialTheme.colorScheme.primary else Color.LightGray
                    )
                ) {
                    Text("全部")
                }
                Button(
                    onClick = { selectedTypeFilter = TransactionType.EXPENSE },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTypeFilter == TransactionType.EXPENSE) MaterialTheme.colorScheme.primary else Color.LightGray
                    )
                ) {
                    Text("支出")
                }
                Button(
                    onClick = { selectedTypeFilter = TransactionType.INCOME },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTypeFilter == TransactionType.INCOME) MaterialTheme.colorScheme.primary else Color.LightGray
                    )
                ) {
                    Text("收入")
                }
                Button(
                    onClick = { selectedTypeFilter = TransactionType.TRANSFER },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTypeFilter == TransactionType.TRANSFER) MaterialTheme.colorScheme.primary else Color.LightGray
                    )
                ) {
                    Text("轉帳")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 交易列表
            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("查無符合紀錄", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredTransactions) { tx ->
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
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = account?.name ?: "帳戶",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        if (tx.type == TransactionType.TRANSFER && targetAccount != null) {
                                            Text(
                                                text = " ➔ ${targetAccount.name}",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    if (tx.note.isNotBlank()) {
                                        Text(text = tx.note, fontSize = 13.sp)
                                    }
                                    Text(
                                        text = dateFormat.format(Date(tx.dateMillis)),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val color = when (tx.type) {
                                        TransactionType.INCOME -> Color(0xFF2E7D32)
                                        TransactionType.EXPENSE -> Color(0xFFC62828)
                                        TransactionType.TRANSFER -> Color.DarkGray
                                    }
                                    val prefix = when (tx.type) {
                                        TransactionType.INCOME -> "+"
                                        TransactionType.EXPENSE -> "-"
                                        TransactionType.TRANSFER -> ""
                                    }
                                    Text(
                                        text = "$prefix NT$ ${tx.amount}",
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        fontSize = 16.sp
                                    )
                                    IconButton(onClick = { viewModel.deleteTransaction(tx.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "刪除", tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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

    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    val currentType = when (selectedTypeIndex) {
        0 -> TransactionType.EXPENSE
        1 -> TransactionType.INCOME
        else -> TransactionType.TRANSFER
    }

    val typeCategories = categories.filter { it.type == currentType }
    var selectedCategory by remember(currentType) { mutableStateOf(typeCategories.firstOrNull()) }
    var selectedSourceAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var selectedTargetAccount by remember { mutableStateOf(accounts.getOrNull(1) ?: accounts.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增記帳紀錄") },
        text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TabRow(selectedTabIndex = selectedTypeIndex) {
                    Tab(selected = selectedTypeIndex == 0, onClick = { selectedTypeIndex = 0 }) {
                        Text("支出", modifier = Modifier.padding(8.dp))
                    }
                    Tab(selected = selectedTypeIndex == 1, onClick = { selectedTypeIndex = 1 }) {
                        Text("收入", modifier = Modifier.padding(8.dp))
                    }
                    Tab(selected = selectedTypeIndex == 2, onClick = { selectedTypeIndex = 2 }) {
                        Text("轉帳", modifier = Modifier.padding(8.dp))
                    }
                }

                // 金額數字顯示
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("金額 (TWD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 內建數字小鍵盤快速按鍵
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    items((1..9).toList() + listOf(0)) { num ->
                        Button(
                            onClick = { amountText += num.toString() },
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(num.toString())
                        }
                    }
                    item {
                        Button(
                            onClick = { if (amountText.isNotEmpty()) amountText = amountText.dropLast(1) },
                            modifier = Modifier.padding(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                        ) {
                            Text("⌫")
                        }
                    }
                    item {
                        Button(
                            onClick = { amountText = "" },
                            modifier = Modifier.padding(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                        ) {
                            Text("C")
                        }
                    }
                }

                // 帳戶選擇
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("來源帳戶：", modifier = Modifier.align(Alignment.CenterVertically))
                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = {}
                    ) {
                        Text(
                            text = selectedSourceAccount?.name ?: "選擇帳戶",
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (currentType == TransactionType.TRANSFER) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("轉入帳戶：", modifier = Modifier.align(Alignment.CenterVertically))
                        Text(
                            text = selectedTargetAccount?.name ?: "選擇帳戶",
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // 分類選擇
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("分類：", modifier = Modifier.align(Alignment.CenterVertically))
                        Text(
                            text = selectedCategory?.name ?: "選擇分類",
                            modifier = Modifier.padding(8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    val sourceAcc = selectedSourceAccount
                    if (amount > 0 && sourceAcc != null) {
                        viewModel.addTransaction(
                            type = currentType,
                            amount = amount,
                            dateMillis = System.currentTimeMillis(),
                            sourceAccountId = sourceAcc.id,
                            targetAccountId = if (currentType == TransactionType.TRANSFER) selectedTargetAccount?.id else null,
                            categoryId = if (currentType != TransactionType.TRANSFER) selectedCategory?.id else null,
                            note = note
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("儲存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
