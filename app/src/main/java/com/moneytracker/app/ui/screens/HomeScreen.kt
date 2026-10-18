package com.moneytracker.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.data.model.TransactionType
import com.moneytracker.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.moneytracker.app.ui.screens.AddTransactionDialog
import java.text.DecimalFormat

fun Long.toFormattedString(): String = DecimalFormat("#,###").format(this)

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val totalAccountBalance by viewModel.totalAccountBalance.collectAsState()
    val outstandingLend by viewModel.outstandingLendTotal.collectAsState()
    val outstandingBorrow by viewModel.outstandingBorrowTotal.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val loansWithDetails by viewModel.loansWithDetails.collectAsState()

    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN)

    var showAddDialog by remember { mutableStateOf(false) }

    val overdueLoans = loansWithDetails.filter {
        val dueDate = it.loan.dueDateMillis
        dueDate != null && dueDate < System.currentTimeMillis() && it.remainingAmount > 0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 本月概覽卡片

        item {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("快速記帳", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                val gradientBrush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                )
                Column(modifier = Modifier
                    .background(gradientBrush)
                    .padding(20.dp)
                    .fillMaxWidth()
                ) {
                    Text(text = "本月收支總覽", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("本月收入", fontSize = 14.sp)
                            }
                            Text(
                                text = "NT$ ${monthlyIncome.toFormattedString()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("本月支出", fontSize = 14.sp)
                            }
                            Text(
                                text = "NT$ ${monthlyExpense.toFormattedString()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Column {
                            Text("收支差額", fontSize = 14.sp)
                            val net = monthlyIncome - monthlyExpense
                            Text(
                                text = "NT$ ${net.toFormattedString()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (net >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // 資產與借貸統計
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("目前帳戶總餘額：", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "NT$ ${totalAccountBalance.toFormattedString()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("待收回借款 (借出)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("NT$ ${outstandingLend.toFormattedString()}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("待償還借款 (借入)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("NT$ ${outstandingBorrow.toFormattedString()}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 逾期提醒 Banner
        if (overdueLoans.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("借貸逾期提醒", fontWeight = FontWeight.Bold, color = Color.Red)
                            Text("您有 ${overdueLoans.size} 筆借貸紀錄已超過預計還款日期！", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 最近交易紀錄
        item {
            Text("最近交易紀錄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (transactions.isEmpty()) {
            item {
                Text("尚無交易紀錄", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
            }
        } else {
            items(transactions.take(10)) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (tx.note.isNotBlank()) tx.note else tx.type.label,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = dateFormat.format(Date(tx.dateMillis)),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                        Text(
                            text = "$prefix NT$ ${tx.amount.toFormattedString()}",
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
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
