package com.moneytracker.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneytracker.app.data.model.TransactionType
import com.moneytracker.app.ui.components.AppCard
import com.moneytracker.app.ui.components.EmptyState
import com.moneytracker.app.ui.components.ListBottomPadding
import com.moneytracker.app.ui.components.ScreenHeader
import com.moneytracker.app.ui.components.SectionTitle
import com.moneytracker.app.ui.components.formatMoney
import com.moneytracker.app.ui.viewmodel.MainViewModel
import java.util.Calendar

private val ChartColors = listOf(
    Color(0xFF0B7A55), Color(0xFF3B82F6), Color(0xFFF59E0B), Color(0xFFEF4444),
    Color(0xFF8B5CF6), Color(0xFF14B8A6), Color(0xFFEC4899), Color(0xFF78909C)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: MainViewModel) {
    var monthOnly by remember { mutableStateOf(true) }
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val monthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // 排除借貸金流，只算真實收支
    val real = transactions.filter {
        it.loanId == null && it.repaymentId == null && (!monthOnly || it.dateMillis >= monthStart)
    }
    val expenseTx = real.filter { it.type == TransactionType.EXPENSE }
    val totalExpense = expenseTx.sumOf { it.amount }
    val totalIncome = real.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    val byCategory = expenseTx.groupBy { it.categoryId }
        .map { (catId, list) -> (categories.find { it.id == catId }?.name ?: "未分類") to list.sumOf { it.amount } }
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = ListBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("統計") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = monthOnly, onClick = { monthOnly = true }, label = { Text("本月") })
                FilterChip(selected = !monthOnly, onClick = { monthOnly = false }, label = { Text("全部期間") })
            }
        }

        // 收入 vs 支出
        item {
            AppCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("收入與支出", style = MaterialTheme.typography.titleMedium)
                    val maxVal = maxOf(totalIncome, totalExpense, 1L).toFloat()
                    BarRow("收入", totalIncome, totalIncome / maxVal, MaterialTheme.colorScheme.primary)
                    BarRow("支出", totalExpense, totalExpense / maxVal, MaterialTheme.colorScheme.error)
                }
            }
        }

        // 支出分類
        item { SectionTitle("支出分類") }

        if (totalExpense == 0L) {
            item { EmptyState("尚無支出資料") }
        } else {
            item {
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 28.dp.toPx()
                                val arcSize = Size(size.width - stroke, size.height - stroke)
                                val topLeft = Offset(stroke / 2, stroke / 2)
                                drawArc(
                                    color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                                    topLeft = topLeft, size = arcSize, style = Stroke(width = stroke)
                                )
                                var start = -90f
                                byCategory.forEachIndexed { index, pair ->
                                    val sweep = pair.second.toFloat() / totalExpense.toFloat() * 360f
                                    drawArc(
                                        color = ChartColors[index % ChartColors.size],
                                        startAngle = start,
                                        sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = stroke)
                                    )
                                    start += sweep
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("總支出", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatMoney(totalExpense), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }

            item {
                AppCard {
                    Column {
                        byCategory.forEachIndexed { index, (name, sum) ->
                            val percent = sum.toFloat() / totalExpense.toFloat()
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ChartColors[index % ChartColors.size]))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(formatMoney(sum), style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${(percent * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(percent.coerceIn(0.02f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp))
                                            .background(ChartColors[index % ChartColors.size])
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarRow(label: String, amount: Long, fraction: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMoney(amount), style = MaterialTheme.typography.titleSmall, color = color)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).height(10.dp).clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
        }
    }
}
