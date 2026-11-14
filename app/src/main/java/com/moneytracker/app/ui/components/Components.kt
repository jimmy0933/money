package com.moneytracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 列表底部保留空間，避免最後一筆資料被浮動按鈕/導覽列蓋住。 */
val ListBottomPadding = 96.dp

fun formatMoney(value: Long): String =
    "NT$ " + NumberFormat.getNumberInstance(Locale.US).format(value)

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "確定",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (action != null) action()
    }
}

/** 唯讀日期欄位，點右側日曆圖示選擇日期；一律使用手機本地時區。 */
@Composable
fun DatePickerField(
    label: String,
    millis: Long?,
    placeholder: String,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val formatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN) }

    fun showPicker() {
        val cal = Calendar.getInstance()
        if (millis != null) cal.timeInMillis = millis
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance()
                picked.set(year, month, day, 12, 0, 0)
                picked.set(Calendar.MILLISECOND, 0)
                onPick(picked.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    OutlinedTextField(
        value = millis?.let { formatter.format(Date(it)) } ?: "",
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = modifier.fillMaxWidth(),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (millis != null && onClear != null) {
                    IconButton(onClick = onClear) { Icon(Icons.Default.Clear, contentDescription = "清除") }
                }
                IconButton(onClick = { showPicker() }) {
                    Icon(Icons.Default.DateRange, contentDescription = "選擇日期")
                }
            }
        }
    )
}

/** 唯讀下拉選單；可選擇在清單最後附加一個動作項目 (例如「＋ 新增對象」)。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    selectedText: String,
    items: List<T>,
    itemText: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    extraLabel: String? = null,
    onExtra: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            isError = isError,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemText(item)) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
            if (extraLabel != null && onExtra != null) {
                DropdownMenuItem(
                    text = { Text(extraLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                    onClick = {
                        expanded = false
                        onExtra()
                    }
                )
            }
        }
    }
}


/** 全 App 統一的卡片：無陰影、細邊框，深淺色都清楚。 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable () -> Unit
) {
    val base = if (onClick != null) modifier.fillMaxWidth().clickable { onClick() } else modifier.fillMaxWidth()
    Card(
        modifier = base,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

/** 頁面標題列：標題靠左、右側可放操作。所有頁面共用，確保位置與字級一致。 */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        if (action != null) action()
    }
}

/** 上小標籤、下大數字，欄位對齊用。 */
@Composable
fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    align: TextAlign = TextAlign.Start,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    val horizontal = when (align) {
        TextAlign.End -> Alignment.End
        TextAlign.Center -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }
    Column(modifier = modifier, horizontalAlignment = horizontal) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = align, maxLines = 1)
        Text(value, style = valueStyle, color = valueColor, textAlign = align, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 圓形圖示底。 */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.size(40.dp), shape = CircleShape, color = tint.copy(alpha = 0.14f)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

/** 交易/明細列：左圖示、中標題+副標、右金額。首頁與記帳頁共用。 */
@Composable
fun TransactionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    amountText: String,
    amountColor: Color,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 12.dp, end = if (trailing != null) 4.dp else 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, tint = amountColor)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(amountText, style = MaterialTheme.typography.titleSmall, color = amountColor, maxLines = 1)
        if (trailing != null) trailing()
    }
}
