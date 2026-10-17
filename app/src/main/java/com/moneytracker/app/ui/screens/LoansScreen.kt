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
    var showContactManagementDialog by remember { mutableStateOf<ContactEntity?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }

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
                                Text("NT$ $totalLendOutstanding", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("總待償還", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("NT$ $totalBorrowOutstanding", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            }
                        }
                    }

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
                    Text(summary.contact.phone, fontSize = 12.sp, color = Color.Gray)
                }
            }
            if (summary.contact.note.isNotBlank()) {
                Text(summary.contact.note, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("借出 (待收回)", fontSize = 12.sp, color = Color.Gray)
                    Text("NT$ ${summary.outstandingLend}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("借入 (待償還)", fontSize = 12.sp, color = Color.Gray)
                    Text("NT$ ${summary.outstandingBorrow}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
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
                    color = if (loanWithDetails.loan.type == LoanType.LEND) Color(0xFF2E7D32) else Color(0xFFC62828),
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
                Text("剩餘金額: NT$ ${loanWithDetails.remainingAmount}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (loanWithDetails.remainingAmount == 0L) Color(0xFF2E7D32) else Color(0xFFC62828))
                if (loanWithDetails.loan.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
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
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var contactName by remember { mutableStateOf("") }
    var selectedTypeIndex by remember { mutableStateOf(0) } // 0: 借出, 1: 借入
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val accounts by viewModel.accounts.collectAsState()
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }

    val contactSummaries by viewModel.contactSummaries.collectAsState()
    val existingContacts = contactSummaries.map { it.contact }

    var expanded by remember { mutableStateOf(false) }
    var selectedContactId by remember { mutableStateOf<Long?>(null) }

    val localCalendar = Calendar.getInstance()
    val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR, localCalendar.get(Calendar.YEAR))
        set(Calendar.MONTH, localCalendar.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, localCalendar.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    var dateMillis by remember { mutableStateOf(calendar.timeInMillis) }
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }

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

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = {
                            contactName = it
                            selectedContactId = existingContacts.find { c -> c.name == it.trim() }?.id
                            expanded = true
                        },
                        label = { Text("對象姓名") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    val filteredContacts = existingContacts.filter { !it.isDeleted && it.name.contains(contactName, ignoreCase = true) }
                    var showQuickAddContactDialog by remember { mutableStateOf(false) }

                    if (expanded) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            filteredContacts.forEach { contact ->
                                DropdownMenuItem(
                                    text = { Text(contact.name) },
                                    onClick = {
                                        contactName = contact.name
                                        selectedContactId = contact.id
                                        expanded = false
                                    }
                                )
                            }
                            if (filteredContacts.none { it.name == contactName.trim() } && contactName.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("新增對象: $contactName", color = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showQuickAddContactDialog = true
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (showQuickAddContactDialog) {
                        ContactManagementDialog(
                            contact = ContactEntity(name = contactName),
                            viewModel = viewModel,
                            onDismiss = {
                                showQuickAddContactDialog = false
                            },
                            onSaveSuccess = { newContactId ->
                                selectedContactId = newContactId
                                showQuickAddContactDialog = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("借貸金額 (TWD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("帳戶: ${selectedAccount?.name ?: "選擇帳戶"}", fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = dateFormat.format(Date(dateMillis)),
                    onValueChange = { },
                    label = { Text("借貸發生日期") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = "選擇日期")
                        }
                    }
                )

                OutlinedTextField(
                    value = dueDateMillis?.let { dateFormat.format(Date(it)) } ?: "未設定",
                    onValueChange = { },
                    label = { Text("預計還款日 (選填)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDueDatePicker = true }) {
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
            var isSaving by remember { mutableStateOf(false) }
            Button(
                enabled = !isSaving,
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    val acc = selectedAccount
                    val existingMatch = existingContacts.find { c -> c.name == contactName.trim() }
                    if (existingMatch != null && selectedContactId == null) {
                        selectedContactId = existingMatch.id
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
                            contactId = selectedContactId
                        )
                        onDismiss()
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) {
                    Text("確定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis ?: dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDateMillis = datePickerState.selectedDateMillis
                    showDueDatePicker = false
                }) {
                    Text("確定")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    dueDateMillis = null
                    showDueDatePicker = false
                }) { Text("清除") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val localCalendar = Calendar.getInstance()
    val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR, localCalendar.get(Calendar.YEAR))
        set(Calendar.MONTH, localCalendar.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, localCalendar.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    var dateMillis by remember { mutableStateOf(calendar.timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增還款紀錄") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
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

                OutlinedTextField(
                    value = dateFormat.format(Date(dateMillis)),
                    onValueChange = { },
                    label = { Text("還款日期") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = "選擇日期")
                        }
                    }
                )

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
            var isSaving by remember { mutableStateOf(false) }
            Button(
                enabled = !isSaving,
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
                            note = note
                        )
                        onDismiss()
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) {
                    Text("確定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
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
            Button(
                enabled = !isSaving,
                onClick = {
                    if (name.trim().isBlank()) {
                        errorMessage = "姓名不能為空"
                    } else {
                        isSaving = true
                        if (contact == null || contact.id == 0L) {
                            viewModel.addContact(name.trim(), phone.trim(), note.trim()) { id ->
                                onSaveSuccess?.invoke(id)
                                onDismiss()
                            }
                        } else {
                            viewModel.updateContact(contact.copy(name = name.trim(), phone = phone.trim(), note = note.trim())) {
                                onSaveSuccess?.invoke(contact.id)
                                onDismiss()
                            }
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
