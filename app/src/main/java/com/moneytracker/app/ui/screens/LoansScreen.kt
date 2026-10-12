package com.moneytracker.app.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.moneytracker.app.data.model.LoanStatus
import com.moneytracker.app.data.model.LoanType
import com.moneytracker.app.data.repository.ContactSummary
import com.moneytracker.app.data.repository.LoanWithDetails
import com.moneytracker.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0: 借出 (我借給別人), 1: 借入 (我向別人借), 2: 對象彙整
    val loansWithDetails by viewModel.loansWithDetails.collectAsState()
    val contactSummaries by viewModel.contactSummaries.collectAsState()

    var selectedLoanDetail by remember { mutableStateOf<LoanWithDetails?>(null) }
    var selectedContactSummary by remember { mutableStateOf<ContactSummary?>(null) }
    var showAddLoanDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)

    if (selectedLoanDetail != null) {
        LoanDetailView(
            loanWithDetails = selectedLoanDetail!!,
            viewModel = viewModel,
            onBack = { selectedLoanDetail = null }
        )
        return
    }

    if (selectedContactSummary != null) {
        ContactDetailView(
            contactSummary = selectedContactSummary!!,
            viewModel = viewModel,
            onBack = { selectedContactSummary = null },
            onLoanClick = { selectedLoanDetail = it }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddLoanDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增借貸")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("借出 (我借別人)", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("借入 (我向人借)", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("對象總覽", modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0, 1 -> {
                    val targetType = if (selectedTab == 0) LoanType.LEND else LoanType.BORROW
                    val filteredLoans = loansWithDetails.filter { it.loan.type == targetType }

                    if (filteredLoans.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("尚無相關借貸紀錄", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredLoans) { loanDetail ->
                                LoanCard(
                                    loanDetail = loanDetail,
                                    dateFormat = dateFormat,
                                    onClick = { selectedLoanDetail = loanDetail },
                                    onDelete = { viewModel.deleteLoan(loanDetail.loan.id) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    if (contactSummaries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("尚無借貸對象紀錄", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(contactSummaries) { summary ->
                                ContactSummaryCard(
                                    summary = summary,
                                    onClick = { selectedContactSummary = summary }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddLoanDialog) {
        AddLoanDialog(
            viewModel = viewModel,
            onDismiss = { showAddLoanDialog = false }
        )
    }
}

@Composable
fun LoanCard(
    loanDetail: LoanWithDetails,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val loan = loanDetail.loan
    val contactName = loanDetail.contact?.name ?: "未知"
    val isOverdue = loan.dueDateMillis != null && loan.dueDateMillis < System.currentTimeMillis() && loanDetail.remainingAmount > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(contactName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                StatusChip(status = loan.status, isOverdue = isOverdue)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("原始金額: NT$ ${loan.amount}", fontSize = 13.sp, color = Color.Gray)
                    Text("已還金額: NT$ ${loanDetail.repaidAmount}", fontSize = 13.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("剩餘未還", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "NT$ ${loanDetail.remainingAmount}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (loanDetail.remainingAmount > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dueStr = if (loan.dueDateMillis != null) dateFormat.format(Date(loan.dueDateMillis)) else "無期限"
                Text("借出日期: ${dateFormat.format(Date(loan.dateMillis))} | 到期日: $dueStr", fontSize = 11.sp, color = Color.Gray)

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "刪除", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: LoanStatus, isOverdue: Boolean) {
    val (bgColor, textColor, label) = when {
        isOverdue -> Triple(Color(0xFFFFEBEE), Color.Red, "已逾期")
        status == LoanStatus.PAID -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "已結清")
        status == LoanStatus.PARTIAL -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "部分還款")
        else -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "未還款")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ContactSummaryCard(summary: ContactSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(summary.contact.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("我借給對方", fontSize = 12.sp, color = Color.Gray)
                    Text("總借出: NT$ ${summary.totalLendAmount}", fontSize = 13.sp)
                    Text("尚欠未收: NT$ ${summary.outstandingLend}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("我向對方借", fontSize = 12.sp, color = Color.Gray)
                    Text("總借入: NT$ ${summary.totalBorrowAmount}", fontSize = 13.sp)
                    Text("尚欠未還: NT$ ${summary.outstandingBorrow}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                }
            }
        }
    }
}

@Composable
fun LoanDetailView(
    loanWithDetails: LoanWithDetails,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var showRepaymentDialog by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Text("借貸詳細內容", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("對象: ${loanWithDetails.contact?.name ?: "未知"}", style = MaterialTheme.typography.titleMedium)
                Text("借貸類型: ${loanWithDetails.loan.type.label}", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("原始本金: NT$ ${loanWithDetails.loan.amount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("累計已還: NT$ ${loanWithDetails.repaidAmount}", fontSize = 16.sp)
                Text("剩餘金額: NT$ ${loanWithDetails.remainingAmount}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                if (loanWithDetails.loan.note.isNotBlank()) {
                    Text("備註: ${loanWithDetails.loan.note}", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("分次還款紀錄歷史", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (loanWithDetails.remainingAmount > 0) {
                Button(onClick = { showRepaymentDialog = true }) {
                    Text("新增還款")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (loanWithDetails.repayments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("尚無還款紀錄", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(loanWithDetails.repayments) { repayment ->
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
                            Column {
                                Text("還款金額: NT$ ${repayment.amount}", fontWeight = FontWeight.Bold)
                                Text(dateFormat.format(Date(repayment.dateMillis)), fontSize = 12.sp, color = Color.Gray)
                                if (repayment.note.isNotBlank()) {
                                    Text(repayment.note, fontSize = 13.sp)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteLoanRepayment(repayment.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "刪除還款", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRepaymentDialog) {
        AddRepaymentDialog(
            loanWithDetails = loanWithDetails,
            viewModel = viewModel,
            onDismiss = { showRepaymentDialog = false }
        )
    }
}

@Composable
fun ContactDetailView(
    contactSummary: ContactSummary,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLoanClick: (LoanWithDetails) -> Unit
) {
    val loansWithDetails by viewModel.loansWithDetails.collectAsState()
    val contactLoans = loansWithDetails.filter { it.loan.contactId == contactSummary.contact.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Text("${contactSummary.contact.name} - 借貸歷史", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(contactLoans) { loanDetail ->
                LoanCard(
                    loanDetail = loanDetail,
                    dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN),
                    onClick = { onLoanClick(loanDetail) },
                    onDelete = { viewModel.deleteLoan(loanDetail.loan.id) }
                )
            }
        }
    }
}

@Composable
fun AddLoanDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var contactName by remember { mutableStateOf("") }
    var selectedTypeIndex by remember { mutableStateOf(0) } // 0: 借出, 1: 借入
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val accounts by viewModel.accounts.collectAsState()
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增借貸紀錄") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TabRow(selectedTabIndex = selectedTypeIndex) {
                    Tab(selected = selectedTypeIndex == 0, onClick = { selectedTypeIndex = 0 }) {
                        Text("借出 (我借別人)", modifier = Modifier.padding(8.dp))
                    }
                    Tab(selected = selectedTypeIndex == 1, onClick = { selectedTypeIndex = 1 }) {
                        Text("借入 (我向人借)", modifier = Modifier.padding(8.dp))
                    }
                }

                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("對象姓名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("借貸金額 (TWD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("帳戶: ${selectedAccount?.name ?: "選擇帳戶"}", fontWeight = FontWeight.Bold)

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
                    val acc = selectedAccount
                    if (contactName.isNotBlank() && amount > 0 && acc != null) {
                        viewModel.addLoan(
                            contactName = contactName.trim(),
                            type = if (selectedTypeIndex == 0) LoanType.LEND else LoanType.BORROW,
                            amount = amount,
                            dateMillis = System.currentTimeMillis(),
                            dueDateMillis = null,
                            accountId = acc.id,
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

@Composable
fun AddRepaymentDialog(
    loanWithDetails: LoanWithDetails,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val accounts by viewModel.accounts.collectAsState()
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增還款紀錄") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("剩餘尚未歸還: NT$ ${loanWithDetails.remainingAmount}", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            amountText = it
                            errorMessage = null
                        }
                    },
                    label = { Text("還款金額 (TWD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = Color.Red, fontSize = 12.sp)
                }

                Text("資金流向帳戶: ${selectedAccount?.name ?: "選擇帳戶"}", fontWeight = FontWeight.Bold)

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
                    val acc = selectedAccount
                    if (amount > loanWithDetails.remainingAmount) {
                        errorMessage = "還款金額不能超過剩餘欠款 (NT$ ${loanWithDetails.remainingAmount})"
                    } else if (amount > 0 && acc != null) {
                        viewModel.addLoanRepayment(
                            loanId = loanWithDetails.loan.id,
                            amount = amount,
                            dateMillis = System.currentTimeMillis(),
                            accountId = acc.id,
                            note = note
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("確認還款")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
