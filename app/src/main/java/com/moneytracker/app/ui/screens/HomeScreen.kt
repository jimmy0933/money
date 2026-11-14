package com.moneytracker.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moneytracker.app.data.model.TransactionType
import com.moneytracker.app.ui.components.AppCard
import com.moneytracker.app.ui.components.EmptyState
import com.moneytracker.app.ui.components.LabeledValue
import com.moneytracker.app.ui.components.ListBottomPadding
import com.moneytracker.app.ui.components.ScreenHeader
import com.moneytracker.app.ui.components.SectionTitle
import com.moneytracker.app.ui.components.TransactionRow
import com.moneytracker.app.ui.components.formatMoney
import com.moneytracker.app.ui.viewmodel.MainViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val categories by viewModel.categories.collectAsState()

    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.TAIWAN) }
    var showAddDialog by remember { mutableStateOf(false) }

    val overdueLoans = loansWithDetails.filter {
        val dueDate = it.loan.dueDateMillis
        dueDate != null && dueDate < System.currentTimeMillis() && it.remainingAmount > 0
    }
    val net = monthlyIncome - monthlyExpense

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = ListBottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScreenHeader("首頁") }

            // 資產總覽
            item {
                AppCard(containerColor = MaterialTheme.colorScheme.primaryContainer, borderColor = MaterialTheme.colorScheme.primaryContainer) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("帳戶總餘額", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            formatMoney(totalAccountBalance),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 本月收支
            item {
                AppCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("本月收支", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LabeledValue("收入", formatMoney(monthlyIncome), Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                            LabeledValue("支出", formatMoney(monthlyExpense), Modifier.weight(1f), MaterialTheme.colorScheme.error, TextAlign.Center)
                            LabeledValue(
                                "結餘", formatMoney(net), Modifier.weight(1f),
                                if (net >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                TextAlign.End
                            )
                        }
                    }
                }
            }

            // 借貸
            item {
                AppCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("借貸概況", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LabeledValue("待收回 (借出)", formatMoney(outstandingLend), Modifier.weight(1f))
                            LabeledValue("待償還 (借入)", formatMoney(outstandingBorrow), Modifier.weight(1f), align = TextAlign.End)
                        }
                    }
                }
            }

            if (overdueLoans.isNotEmpty()) {
                item {
                    AppCard(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        borderColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("借貸逾期提醒", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text(
                                    "有 ${overdueLoans.size} 筆借貸已超過預計還款日",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            item { SectionTitle("最近交易", modifier = Modifier.padding(top = 4.dp)) }

            if (transactions.isEmpty()) {
                item { EmptyState("尚無交易紀錄") }
            } else {
                item {
                    AppCard {
                        Column {
                            val recent = transactions.take(10)
                            recent.forEachIndexed { index, tx ->
                                val color = when (tx.type) {
                                    TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                                    TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                val icon = when (tx.type) {
                                    TransactionType.INCOME -> Icons.Default.ArrowUpward
                                    TransactionType.EXPENSE -> Icons.Default.ArrowDownward
                                    TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                                }
                                val prefix = when (tx.type) {
                                    TransactionType.INCOME -> "+"
                                    TransactionType.EXPENSE -> "-"
                                    TransactionType.TRANSFER -> ""
                                }
                                val categoryName = categories.find { it.id == tx.categoryId }?.name
                                TransactionRow(
                                    icon = icon,
                                    title = tx.note.ifBlank { categoryName ?: tx.type.label },
                                    subtitle = listOfNotNull(categoryName, dateFormat.format(Date(tx.dateMillis))).joinToString(" · "),
                                    amountText = "$prefix${formatMoney(tx.amount)}",
                                    amountColor = color
                                )
                                if (index < recent.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("記一筆") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    if (showAddDialog) {
        AddTransactionDialog(viewModel = viewModel, onDismiss = { showAddDialog = false })
    }
}
