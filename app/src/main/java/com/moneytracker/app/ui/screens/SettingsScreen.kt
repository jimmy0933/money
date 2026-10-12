package com.moneytracker.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.moneytracker.app.data.model.AccountType
import com.moneytracker.app.data.model.TransactionType
import com.moneytracker.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val googleAccountEmail by viewModel.googleAccountEmail.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val cloudBackups by viewModel.cloudBackups.collectAsState()

    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showAccountDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showBackupPasswordDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("設定與雲端備份", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        // Google 雲端備份狀態卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Google Drive 備份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (googleAccountEmail.isNull_or_empty()) {
                                    showLoginDialog = true
                                } else {
                                    viewModel.setGoogleAccount(null)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (googleAccountEmail.isNull_or_empty()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        ) {
                            Text(if (googleAccountEmail.isNull_or_empty()) "登入 Google" else "切換/登出")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("連結帳號: ${googleAccountEmail ?: "未連結 (請登入以啟動自動雲端備份)"}", fontSize = 13.sp)

                    val timeStr = if (lastBackupTime != null) dateFormat.format(Date(lastBackupTime!!)) else "尚無備份"
                    Text("上次成功備份時間: $timeStr", fontSize = 13.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showBackupPasswordDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = !googleAccountEmail.isNull_or_empty()
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("立即備份")
                        }

                        Button(
                            onClick = {
                                viewModel.fetchCloudBackups()
                                showRestoreDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !googleAccountEmail.isNull_or_empty()
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("雲端還原")
                        }
                    }
                }
            }
        }

        // 帳戶管理
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("帳戶管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showAccountDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "新增帳戶")
                }
            }
        }

        items(accounts) { account ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${account.name} (${account.type.label})", fontWeight = FontWeight.Bold)
                    }
                    Text("初始: NT$ ${account.initialBalance}", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }

        // 分類管理
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("收支分類管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showCategoryDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "新增分類")
                }
            }
        }

        items(categories) { category ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Category, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${category.name} (${category.type.label})")
                    }
                    if (category.isDefault) {
                        Text("預設", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }

    if (showLoginDialog) {
        var emailInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text("登入 Google 帳號") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("請輸入欲同步備份之 Google 帳號 Email：", fontSize = 13.sp)
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Google Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.isNotBlank()) {
                            viewModel.setGoogleAccount(emailInput.trim())
                            showLoginDialog = false
                        }
                    }
                ) {
                    Text("確認登入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) { Text("取消") }
            }
        )
    }

    if (showBackupPasswordDialog) {
        var pwdInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBackupPasswordDialog = false },
            title = { Text("自訂備份密碼 (可不填)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("設定自訂密碼以 AES 加密備份檔案。如設定密碼，還原時必須輸入該密碼；若忘記密碼將無法還原！", fontSize = 12.sp, color = Color.Red)
                    OutlinedTextField(
                        value = pwdInput,
                        onValueChange = { pwdInput = it },
                        label = { Text("備份加密密碼 (可留空)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.performBackup(pwdInput.ifBlank { null })
                        showBackupPasswordDialog = false
                    }
                ) {
                    Text("開始備份")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupPasswordDialog = false }) { Text("取消") }
            }
        )
    }

    if (showRestoreDialog) {
        var pwdInput by remember { mutableStateOf("") }
        var selectedFileId by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("選擇 Google Drive 備份還原") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("警告：還原雲端資料將完全覆蓋本機目前的記帳與借貸資料！", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = pwdInput,
                        onValueChange = { pwdInput = it },
                        label = { Text("備份解密密碼 (如未設定請留空)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("雲端備份歷史列表：", fontWeight = FontWeight.Bold)

                    if (cloudBackups.isEmpty()) {
                        Text("未在 Google Drive 找到任何備份檔", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        LazyColumn(modifier = Modifier.height(150.dp)) {
                            items(cloudBackups) { file ->
                                val isSelected = selectedFileId == file.fileId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { selectedFileId = file.fileId },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(file.fileName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("日期: ${dateFormat.format(Date(file.createdTimeMillis))}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileId = selectedFileId ?: cloudBackups.firstOrNull()?.fileId
                        if (fileId != null) {
                            viewModel.restoreCloudBackup(fileId, pwdInput.ifBlank { null })
                            showRestoreDialog = false
                        }
                    },
                    enabled = cloudBackups.isNotEmpty()
                ) {
                    Text("確認還原")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("取消") }
            }
        )
    }

    if (showAccountDialog) {
        var accName by remember { mutableStateOf("") }
        var accType by remember { mutableStateOf(AccountType.CASH) }
        var initBalText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("新增帳戶") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        label = { Text("帳戶名稱") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = initBalText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) initBalText = it },
                        label = { Text("初始餘額 (TWD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (accName.isNotBlank()) {
                            val bal = initBalText.toLongOrNull() ?: 0L
                            viewModel.addAccount(accName.trim(), accType, bal)
                            showAccountDialog = false
                        }
                    }
                ) {
                    Text("儲存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) { Text("取消") }
            }
        )
    }

    if (showCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        var catType by remember { mutableStateOf(TransactionType.EXPENSE) }

        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("新增分類") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("分類名稱") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            viewModel.addCategory(catName.trim(), catType)
                            showCategoryDialog = false
                        }
                    }
                ) {
                    Text("儲存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) { Text("取消") }
            }
        )
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
