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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.HorizontalDivider
import java.util.Calendar
import com.moneytracker.app.data.entity.ContactEntity
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.Composable
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

    var selectedLoanId by remember { mutableStateOf<Long?>(null) }
    var selectedContactId by remember { mutableStateOf<Long?>(null) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var showContactManagementDialog by remember { mutableStateOf<ContactEntity?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)

    if (selectedLoanId != null) {
        val currentLoanDetail = loansWithDetails.find { it.loan.id == selectedLoanId }
        if (currentLoanDetail != null) {
            LoanDetailView(
                loanWithDetails = currentLoanDetail,
                viewModel = viewModel,
                onBack = { selectedLoanId = null }
            )
            return
        } else {
            selectedLoanId = null
        }
    }

    if (selectedContactId != null) {
        val currentContactSummary = contactSummaries.find { it.contact.id == selectedContactId }
        if (currentContactSummary != null) {
            ContactDetailView(
                contactSummary = currentContactSummary,
                viewModel = viewModel,
                onBack = { selectedContactId = null },
                onLoanClick = { selectedLoanId = it.loan.id }
            )
            return
        } else {
            selectedContactId = null
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 2) {
                FloatingActionButton(
                    onClick = { showAddContactDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新增對象")
                }
            } else {
                FloatingActionButton(
                    onClick = { showAddLoanDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新增借貸")
                }
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

                    val totalLendOutstanding = contactSummaries.sumOf { it.outstandingLend }
                    val totalBorrowOutstanding = contactSummaries.sumOf { it.outstandingBorrow }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("總待收回", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("NT$ $totalLendOutstanding", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("總待償還", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("NT$ $totalBorrowOutstanding", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (filteredLoans.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("尚無相關借貸紀錄", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    onClick = { selectedLoanId = loanDetail.loan.id },
                                    onDelete = { viewModel.deleteLoan(loanDetail.loan.id) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("對象總覽", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Button(onClick = { viewModel.setUiMessage("管理對象") }) {
                                Text("新增對象")
                            }
                        }
                        if (contactSummaries.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("尚無借貸對象紀錄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(contactSummaries) { summary ->
                                    ContactSummaryCard(
                                        summary = summary,
                                        onClick = { selectedContactId = summary.contact.id }
                                    )
                                }
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
            contactSummaries = contactSummaries,
            onDismiss = { showAddLoanDialog = false }
        )
    }

    if (showAddContactDialog) {
        ContactManagementDialog(
            contact = null,
            viewModel = viewModel,
            onDismiss = { showAddContactDialog = false }
        )
    }

    if (showContactManagementDialog != null) {
        ContactManagementDialog(
            contact = showContactManagementDialog,
            viewModel = viewModel,
            onDismiss = { showContactManagementDialog = null }
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    Text("原始金額: NT$ ${loan.amount}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("已還金額: NT$ ${loanDetail.repaidAmount}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("剩餘未還", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "NT$ ${loanDetail.remainingAmount}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (loanDetail.remainingAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
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
                Text("借出日期: ${dateFormat.format(Date(loan.dateMillis))} | 到期日: $dueStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: LoanStatus, isOverdue: Boolean) {
    val (bgColor, textColor, label) = when {
        isOverdue -> Triple(Color(0xFFFFEBEE), Color.Red, "已逾期")
        status == LoanStatus.PAID -> Triple(Color(0xFFE8F5E9), MaterialTheme.colorScheme.primary, "已結清")
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
                    Text(summary.contact.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                if (summary.contact.phone.isNotBlank()) {
                    Text(summary.contact.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (summary.contact.note.isNotBlank()) {
                Text(summary.contact.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("我借給對方", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val formattedTotalLend = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(summary.totalLendAmount)
                    val formattedOutstandingLend = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(summary.outstandingLend)
                    Text("總借出: NT$ $formattedTotalLend", fontSize = 13.sp)
                    Text("尚欠未收: NT$ $formattedOutstandingLend", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    val formattedTotalBorrow = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(summary.totalBorrowAmount)
                    val formattedOutstandingBorrow = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(summary.outstandingBorrow)
                    Text("我向對方借", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("總借入: NT$ $formattedTotalBorrow", fontSize = 13.sp)
                    Text("尚欠未還: NT$ $formattedOutstandingBorrow", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                Text(
                    "借貸類型: ${loanWithDetails.loan.type.label}",
                    fontSize = 14.sp,
                    color = if (loanWithDetails.loan.type == LoanType.LEND) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val dateFormatUtc = SimpleDateFormat("yyyy/MM/dd", java.util.Locale.TAIWAN).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                Text("借貸發生日期: ${dateFormatUtc.format(Date(loanWithDetails.loan.dateMillis))}", fontSize = 14.sp)
                Text("預計還款日: ${loanWithDetails.loan.dueDateMillis?.let { dateFormatUtc.format(Date(it)) } ?: "未設定"}", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("原始本金: NT$ ${loanWithDetails.loan.amount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("累計已還: NT$ ${loanWithDetails.repaidAmount}", fontSize = 16.sp)
                Text("剩餘金額: NT$ ${loanWithDetails.remainingAmount}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (loanWithDetails.remainingAmount == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                if (loanWithDetails.loan.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("備註: ${loanWithDetails.loan.note}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("尚無還款紀錄", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(loanWithDetails.repayments.sortedByDescending { it.dateMillis }) { repayment ->
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
                                val formattedRepayment = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(repayment.amount)
                                Text("還款金額: NT$ $formattedRepayment", fontWeight = FontWeight.Bold)
                                Text(dateFormat.format(Date(repayment.dateMillis)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (repayment.note.isNotBlank()) {
                                    Text(repayment.note, fontSize = 13.sp)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteLoanRepayment(repayment.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "刪除還款", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
        var showEditContactDialog by remember { mutableStateOf(false) }
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text("${contactSummary.contact.name} - 借貸歷史", style = MaterialTheme.typography.titleLarge)
            }
            Row {
                Button(onClick = { showEditContactDialog = true }) {
                    Text("編輯")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("停用")
                }
            }
        }

        if (showEditContactDialog) {
            ContactManagementDialog(
                contact = contactSummary.contact,
                viewModel = viewModel,
                onDismiss = { showEditContactDialog = false }
            )
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("停用借貸對象") },
                text = { Text("確定要停用此對象嗎？停用後歷史紀錄仍會保留，但無法再新增此對象的借貸。") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteContact(contactSummary.contact.id)
                        showDeleteConfirmDialog = false
                        onBack()
                    }) {
                        Text("確定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("取消") }
                }
            )
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddLoanDialog(viewModel: MainViewModel, contactSummaries: List<ContactSummary>, onDismiss: () -> Unit) {
    var contactName by remember { mutableStateOf("") }
    var selectedTypeIndex by remember { mutableStateOf(0) } // 0: 借出, 1: 借入
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val accounts by viewModel.accounts.collectAsState()
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    val dateFormatter = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.TAIWAN)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            dateMillis = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val dueDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            dueDateMillis = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增借貸紀錄") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                TabRow(selectedTabIndex = selectedTypeIndex) {
                    Tab(selected = selectedTypeIndex == 0, onClick = { selectedTypeIndex = 0 }) {
                        Text("借出 (我借別人)", modifier = Modifier.padding(8.dp))
                    }
                    Tab(selected = selectedTypeIndex == 1, onClick = { selectedTypeIndex = 1 }) {
                        Text("借入 (我向人借)", modifier = Modifier.padding(8.dp))
                    }
                }

                var contactExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = contactExpanded,
                    onExpandedChange = { contactExpanded = !contactExpanded }
                ) {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = {
                            contactName = it
                            contactExpanded = true
                        },
                        label = { Text("對象姓名 (可輸入新增或選取)") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contactExpanded) }
                    )

                    val filteredContacts = contactSummaries.filter { it.contact.name.contains(contactName, ignoreCase = true) }.take(5)

                    if (filteredContacts.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = contactExpanded,
                            onDismissRequest = { contactExpanded = false }
                        ) {
                            filteredContacts.forEach { summary ->
                                DropdownMenuItem(
                                    text = { Text(summary.contact.name) },
                                    onClick = {
                                        contactName = summary.contact.name
                                        contactExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("借貸金額 (TWD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "選擇帳戶",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("帳戶") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        isError = selectedAccount == null
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccount = account
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dateFormatter.format(java.util.Date(dateMillis)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("發生日期") },
                    modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = dueDateMillis?.let { dateFormatter.format(java.util.Date(it)) } ?: "選填",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("到期日") },
                    modifier = Modifier.fillMaxWidth().clickable { dueDatePickerDialog.show() },
                    enabled = false,
                    trailingIcon = {
                        if (dueDateMillis != null) {
                            androidx.compose.material3.IconButton(onClick = { dueDateMillis = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除到期日")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.TAIWAN).format(Date(dateMillis)),
                    onValueChange = { },
                    label = { Text("借貸發生日期") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.Add, contentDescription = "選擇日期")
                        }
                    }
                )

                OutlinedTextField(
                    value = dueDateMillis?.let { java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.TAIWAN).format(Date(it)) } ?: "未設定",
                    onValueChange = { },
                    label = { Text("預計還款日 (選填)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { dueDatePickerDialog.show() }) {
                            Icon(Icons.Default.Add, contentDescription = "選擇日期")
                        }
                    }
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val isLoading = viewModel.isLoading.collectAsState()
            var isSaving by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            Button(
                enabled = !isLoading.value && !isSaving,
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    val acc = selectedAccount
                    val existingMatch = contactSummaries.find { c -> c.contact.name == contactName.trim() }?.contact
                    if (existingMatch != null) {
                        /* selectedContactId is not accessible here, nor do we need to set it during adding a loan */
                    }

                    if (contactName.isNotBlank() && amount > 0 && acc != null) {
                        isSaving = true
                        viewModel.addLoan(
                            contactName = contactName.trim(),
                            type = if (selectedTypeIndex == 0) LoanType.LEND else LoanType.BORROW,
                            amount = amount,
                            dateMillis = dateMillis,
                            dueDateMillis = dueDateMillis,
                            accountId = acc.id,
                            note = note,
                            onSuccess = onDismiss,
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
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val dateFormatter = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.TAIWAN)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            dateMillis = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增還款紀錄") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text("剩餘尚未歸還: NT$ ${loanWithDetails.remainingAmount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)

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

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "選擇帳戶",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("資金流向帳戶") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        isError = selectedAccount == null
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccount = account
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dateFormatter.format(java.util.Date(dateMillis)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("還款日期") },
                    modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val isLoading = viewModel.isLoading.collectAsState()
            var isSaving by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            Button(
                enabled = !isLoading.value && !isSaving,
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    val acc = selectedAccount
                    if (amount > loanWithDetails.remainingAmount) {
                        errorMessage = "還款金額不能超過剩餘欠款 (NT$ ${loanWithDetails.remainingAmount})"
                    } else if (amount > 0 && acc != null) {
                        isSaving = true
                        viewModel.addLoanRepayment(
                            loanId = loanWithDetails.loan.id,
                            amount = amount,
                            dateMillis = dateMillis,
                            accountId = acc.id,
                            note = note,
                            onSuccess = onDismiss,
                            onError = { err ->
                                isSaving = false
                                errorMessage = err
                            }
                        )
                    }
                }
            ) {
                Text(if (isSaving) "儲存中..." else "確認還款")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

}

@Composable
fun ContactManagementDialog(
    contact: ContactEntity?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSaveSuccess: ((Long) -> Unit)? = null
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var note by remember { mutableStateOf(contact?.note ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (contact == null || contact.id == 0L) "新增借貸對象" else "編輯借貸對象") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("姓名 (必填)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )
                if (errorMessage != null) {
                    Text(errorMessage!!, color = Color.Red, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("電話 (選填)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註 (選填)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            var isSaving by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            Button(
                enabled = !isSaving,
                onClick = {
                    if (name.trim().isBlank()) {
                        errorMessage = "姓名不能為空"
                    } else {
                        isSaving = true
                        if (contact == null || contact.id == 0L) {
                            viewModel.addContact(name.trim(), phone.trim(), note.trim(), onSuccess = { id ->
                                onSaveSuccess?.invoke(id)
                                onDismiss()
                            }, onError = { err ->
                                isSaving = false
                                errorMessage = err
                            })
                        } else {
                            viewModel.updateContact(contact.copy(name = name.trim(), phone = phone.trim(), note = note.trim()), onSuccess = {
                                onSaveSuccess?.invoke(contact.id)
                                onDismiss()
                            }, onError = { err ->
                                isSaving = false
                                errorMessage = err
                            })
                        }
                    }
                }
            ) {
                Text(if (isSaving) "儲存中..." else "儲存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
