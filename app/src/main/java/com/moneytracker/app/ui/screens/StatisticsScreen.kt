package com.moneytracker.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.data.model.TransactionType
import com.moneytracker.app.ui.viewmodel.MainViewModel

@Composable
fun StatisticsScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0: 本月分類統計, 1: 收支比較
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE && it.loanId == null && it.repaymentId == null }
    val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME && it.loanId == null && it.repaymentId == null }

    val totalExpense = expenseTransactions.sumOf { it.amount }
    val totalIncome = incomeTransactions.sumOf { it.amount }

    val categoryExpenseMap = expenseTransactions.groupBy { it.categoryId }
        .map { (catId, txList) ->
            val catName = categories.find { it.id == catId }?.name ?: "未分類"
            val sum = txList.sumOf { it.amount }
            catName to sum
        }.sortedByDescending { it.second }

    val pieColors = listOf(
        Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF5C6BC0),
        Color(0xFF26A69A), Color(0xFFFFCA28), Color(0xFFFF7043),
        Color(0xFF8D6E63), Color(0xFF78909C)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("統計報表", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("支出分類統計", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("收支長條圖比較", modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> {
                if (totalExpense == 0L) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("尚無支出數據可呈現", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("總支出: NT$ $totalExpense", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Canvas 繪製圓餅圖
                                    Canvas(modifier = Modifier.size(180.dp)) {
                                        var startAngle = -90f
                                        categoryExpenseMap.forEachIndexed { index, pair ->
                                            val sweepAngle = (pair.second.toFloat() / totalExpense.toFloat()) * 360f
                                            val color = pieColors[index % pieColors.size]
                                            drawArc(
                                                color = color,
                                                startAngle = startAngle,
                                                sweepAngle = sweepAngle,
                                                useCenter = true,
                                                size = Size(size.width, size.height)
                                            )
                                            startAngle += sweepAngle
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("分類排行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        items(categoryExpenseMap.toList().mapIndexed { index, pair -> index to pair }) { (index, pair) ->
                            val catName = pair.first
                            val sum = pair.second
                            val color = pieColors[index % pieColors.size]
                            val percent = if (totalExpense > 0) (sum.toDouble() / totalExpense.toDouble() * 100).toInt() else 0
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(color, RoundedCornerShape(4.dp))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(catName, fontWeight = FontWeight.Medium)
                                    }
                                    Text("NT$ $sum ($percent%)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("總收入 vs 總支出長條圖", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        val maxVal = maxOf(totalIncome, totalExpense, 1L)
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val errorColor = MaterialTheme.colorScheme.error

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // 收入柱狀
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val heightRatio = (totalIncome.toFloat() / maxVal.toFloat()).coerceAtLeast(0.05f)
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height((160 * heightRatio).dp)
                                        .padding(bottom = 8.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawRect(color = primaryColor)
                                    }
                                }
                                Text("收入", fontSize = 12.sp)
                                Text("NT$ $totalIncome", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // 支出柱狀
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val heightRatio = (totalExpense.toFloat() / maxVal.toFloat()).coerceAtLeast(0.05f)
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height((160 * heightRatio).dp)
                                        .padding(bottom = 8.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawRect(color = errorColor)
                                    }
                                }
                                Text("支出", fontSize = 12.sp)
                                Text("NT$ $totalExpense", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
