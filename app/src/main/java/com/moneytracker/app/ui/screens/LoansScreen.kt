@file:OptIn(ExperimentalMaterial3Api::class)

package com.moneytracker.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.data.entity.ContactEntity
import com.moneytracker.app.data.model.LoanStatus
import com.moneytracker.app.data.model.LoanType
import com.moneytracker.app.data.repository.ContactSummary
import com.moneytracker.app.data.repository.LoanWithDetails
import com.moneytracker.app.ui.components.ConfirmDialog
import com.moneytracker.app.ui.components.DatePickerField
import com.moneytracker.app.ui.components.DropdownField
import com.moneytracker.app.ui.components.EmptyState
import com.moneytracker.app.ui.components.ListBottomPadding
import com.moneytracker.app.ui.components.SectionTitle
import com.moneytracker.app.ui.components.formatMoney
import com.moneytracker.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun todayNoonMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 12)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@Composable
fun LoansScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0: 借出, 1: 借入, 2: 對象
    val loansWithDetails by viewModel.loansWithDetails.collectAsState()
    val contactSummaries by viewModel.contactSummaries.collectAsState()

    var selectedLoanId by remember { mutableStateOf<Long?>(null) }
    var selectedContactId by remember { mutableStateOf<Long?>(null) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }

    // 詳細頁
    val loanDetail = selectedLoanId?.let { id -> loansWithDetails.find { it.loan.id == id } }
    if (selectedLoanId != null && loanDetail == null) selectedLoanId = null
    if (loanDetail != null) {
        LoanDetailView(loanWithDetails = loanDetail, viewModel = viewModel, onBack = { selectedLoanId = null })
        return
    }

    val contactDetail = selectedContactId?.let { id -> contactSummaries.find { it.contact.id == id } }
    if (selectedContactId != null && contactDetail == null) selectedContactId = null
    if (contactDetail != null) {
        ContactDetailView(
            contactSummary = contactDetail,
            viewModel = viewModel,
            onBack = { selectedContactId = null },
            onLoanClick = { selectedLoanId = it.loan.id }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (selectedTab == 2) {
                ExtendedFloatingActionButton(
                    onClick = { showAddContactDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("新增對象") }
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = { showAddLoanDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("新增借貸") }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("借出") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("借入") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("對象") })
            }

            when (selectedTab) {
                0, 1 -> {
                    val targetType = if (selectedTab == 0) LoanType.LEND else LoanType.BORROW
                    val filteredLoans = loansWithDetails.filter { it.loan.type == targetType }
                    val outstanding = if (selectedTab == 0) {
                        contactSummaries.sumOf { it.outstandingLend }
                    } else {
                        contactSummaries.sumOf { it.outstandingBorrow }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = ListBottomPadding),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        if (selectedTab == 0) "待收回總額" else "待償還總額",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        formatMoney(outstanding),
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        if (filteredLoans.isEmpty()) {
                            item { EmptyState("尚無相關借貸紀錄，點右下角「新增借貸」") }
                        } else {
                            items(filteredLoans, key = { it.loan.id }) { detail ->
                                LoanCard(loanDetail = detail, onClick = { selectedLoanId = detail.loan.id })
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = ListBottomPadding),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (contactSummaries.isEmpty()) {
                            item { EmptyState("尚無對象，點右下角「新增對象」") }
                        } else {
                            items(contactSummaries, key = { it.contact.id }) { summary ->
                                ContactSummaryCard(summary = summary, onClick = { selectedContactId = summary.contact.id })
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
            initialType = if (selectedTab == 1) LoanType.BORROW else LoanType.LEND,
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
}

@Composable
fun LoanCard(loanDetail: LoanWithDetails, onClick: () -> Unit) {
    val loan = loanDetail.loan
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN) }
    val isOverdue = loan.dueDateMillis != null && loan.dueDateMillis < System.currentTimeMillis() && loanDetail.remainingAmount > 0
    val progress = if (loan.amount > 0) (loanDetail.repaidAmount.toFloat() / loan.amount.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        loanDetail.contact?.name ?: "未知",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(status = loan.status, isOverdue = isOverdue)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("剩餘", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatMoney(loanDetail.remainingAmount),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (loanDetail.remainingAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "已還 ${formatMoney(loanDetail.repaidAmount)} / ${formatMoney(loan.amount)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            val dueText = loan.dueDateMillis?.let { "到期 ${dateFormat.format(Date(it))}" } ?: "無期限"
            Text(
                "${dateFormat.format(Date(loan.dateMillis))} · $dueText",
                fontSize = 12.sp,
                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusChip(status: LoanStatus, isOverdue: Boolean) {
    val (bgColor, textColor, label) = when {
        isOverdue -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "已逾期")
        status == LoanStatus.PAID -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "已結清")
        status == LoanStatus.PARTIAL -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "部分還款")
        else -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "未還款")
    }
    Surface(color = bgColor, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun ContactSummaryCard(summary: ContactSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.contact.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = listOf(summary.contact.phone, summary.contact.note).filter { it.isNotBlank() }.joinToString(" · ")
                    if (sub.isNotBlank()) {
                        Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("對方欠我", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(summary.outstandingLend), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("總借出 ${formatMoney(summary.totalLendAmount)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("我欠對方", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(summary.outstandingBorrow), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("總借入 ${formatMoney(summary.totalBorrowAmount)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun LoanDetailView(loanWithDetails: LoanWithDetails, viewModel: MainViewModel, onBack: () -> Unit) {
    var showRepaymentDialog by remember { mutableStateOf(false) }
    var showDeleteLoan by remember { mutableStateOf(false) }
    var repaymentToDelete by remember { mutableStateOf<Long?>(null) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN) }
    val loan = loanWithDetails.loan

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                Text(
                    "借貸明細",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                IconButton(onClick = { showDeleteLoan = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "刪除借貸", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        bottomBar = {
            if (loanWithDetails.remainingAmount > 0) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = { showRepaymentDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) { Text("新增還款") }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                loanWithDetails.contact?.name ?: "未知",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(loan.type.label, fontWeight = FontWeight.Bold)
                        }
                        Text("剩餘", fontSize = 12.sp)
                        Text(formatMoney(loanWithDetails.remainingAmount), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("本金 ${formatMoney(loan.amount)} · 已還 ${formatMoney(loanWithDetails.repaidAmount)}", fontSize = 13.sp)
                        Text(
                            "日期 ${dateFormat.format(Date(loan.dateMillis))} · 到期 ${loan.dueDateMillis?.let { dateFormat.format(Date(it)) } ?: "未設定"}",
                            fontSize = 13.sp
                        )
                        if (loan.note.isNotBlank()) Text("備註: ${loan.note}", fontSize = 13.sp)
                    }
                }
            }

            item { SectionTitle("還款紀錄", modifier = Modifier.padding(top = 8.dp)) }

            if (loanWithDetails.repayments.isEmpty()) {
                item { EmptyState("尚無還款紀錄") }
            } else {
                items(loanWithDetails.repayments.sortedByDescending { it.dateMillis }, key = { it.id }) { repayment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(formatMoney(repayment.amount), fontWeight = FontWeight.Bold)
                                Text(
                                    listOf(dateFormat.format(Date(repayment.dateMillis)), repayment.note)
                                        .filter { it.isNotBlank() }.joinToString(" · "),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { repaymentToDelete = repayment.id }) {
                                Icon(Icons.Default.Delete, contentDescription = "刪除還款", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRepaymentDialog) {
        AddRepaymentDialog(loanWithDetails = loanWithDetails, viewModel = viewModel, onDismiss = { showRepaymentDialog = false })
    }
    if (showDeleteLoan) {
        ConfirmDialog(
            title = "刪除借貸紀錄",
            message = "會一併刪除此筆借貸的所有還款與對應帳戶金流，確定嗎？",
            confirmText = "刪除",
            onConfirm = {
                viewModel.deleteLoan(loan.id)
                onBack()
            },
            onDismiss = { showDeleteLoan = false }
        )
    }
    repaymentToDelete?.let { id ->
        ConfirmDialog(
            title = "刪除還款",
            message = "確定要刪除這筆還款嗎？帳戶餘額會一併還原。",
            confirmText = "刪除",
            onConfirm = { viewModel.deleteLoanRepayment(id) },
            onDismiss = { repaymentToDelete = null }
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
    var showEdit by remember { mutableStateOf(false) }
    var showDisable by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                Text(
                    contactSummary.contact.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = { showEdit = true }) { Icon(Icons.Default.Edit, contentDescription = "編輯對象") }
                IconButton(onClick = { showDisable = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "停用對象", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { ContactSummaryCard(summary = contactSummary, onClick = {}) }
            item { SectionTitle("借貸紀錄", modifier = Modifier.padding(top = 8.dp)) }
            if (contactLoans.isEmpty()) {
                item { EmptyState("此對象尚無借貸紀錄") }
            } else {
                items(contactLoans, key = { it.loan.id }) { detail ->
                    LoanCard(loanDetail = detail, onClick = { onLoanClick(detail) })
                }
            }
        }
    }

    if (showEdit) {
        ContactManagementDialog(contact = contactSummary.contact, viewModel = viewModel, onDismiss = { showEdit = false })
    }
    if (showDisable) {
        ConfirmDialog(
            title = "停用借貸對象",
            message = "停用後歷史紀錄仍會保留，但無法再新增此對象的借貸。確定嗎？",
            confirmText = "停用",
            onConfirm = {
                viewModel.deleteContact(contactSummary.contact.id)
                onBack()
            },
            onDismiss = { showDisable = false }
        )
    }
}

@Composable
fun AddLoanDialog(
    viewModel: MainViewModel,
    contactSummaries: List<ContactSummary>,
    initialType: LoanType = LoanType.LEND,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedContactId by remember { mutableStateOf<Long?>(null) }
    var newContactName by remember { mutableStateOf("") } // 剛新增、清單尚未更新時的暫時顯示名稱
    var showAddContact by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val accounts by viewModel.accounts.collectAsState()
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    val selectedAccount = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()

    var dateMillis by remember { mutableStateOf(todayNoonMillis()) }
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }

    val contactName = contactSummaries.find { it.contact.id == selectedContactId }?.contact?.name ?: newContactName

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("新增借貸") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == LoanType.LEND,
                        onClick = { selectedType = LoanType.LEND },
                        label = { Text("借出 (我借別人)") }
                    )
                    FilterChip(
                        selected = selectedType == LoanType.BORROW,
                        onClick = { selectedType = LoanType.BORROW },
                        label = { Text("借入 (我向人借)") }
                    )
                }

                DropdownField(
                    label = "對象",
                    selectedText = contactName,
                    items = contactSummaries,
                    itemText = { it.contact.name + if (it.contact.phone.isNotBlank()) " (${it.contact.phone})" else "" },
                    onSelect = {
                        selectedContactId = it.contact.id
                        newContactName = it.contact.name
                        errorMessage = null
                    },
                    isError = errorMessage == "請選擇對象",
                    extraLabel = "＋ 新增對象",
                    onExtra = { showAddContact = true }
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { v -> if (v.length <= 12 && v.all { it.isDigit() }) amountText = v },
                    label = { Text("金額 (TWD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                DropdownField(
                    label = if (selectedType == LoanType.LEND) "從哪個帳戶借出" else "存入哪個帳戶",
                    selectedText = selectedAccount?.name ?: "",
                    items = accounts,
                    itemText = { it.name },
                    onSelect = { selectedAccountId = it.id },
                    isError = selectedAccount == null
                )

                DatePickerField(label = "借貸日期", millis = dateMillis, placeholder = "選擇日期", onPick = { dateMillis = it })
                DatePickerField(
                    label = "預計還款日 (選填)",
                    millis = dueDateMillis,
                    placeholder = "未設定",
                    onPick = { dueDateMillis = it },
                    onClear = { dueDateMillis = null }
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註 (選填)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    val acc = selectedAccount
                    val due = dueDateMillis
                    when {
                        selectedContactId == null -> errorMessage = "請選擇對象"
                        amount <= 0 -> errorMessage = "金額必須大於 0"
                        acc == null -> errorMessage = "請選擇帳戶"
                        due != null && due < dateMillis -> errorMessage = "預計還款日不可早於借貸日期"
                        else -> {
                            errorMessage = null
                            isSaving = true
                            viewModel.addLoan(
                                contactName = contactName,
                                type = selectedType,
                                amount = amount,
                                dateMillis = dateMillis,
                                dueDateMillis = due,
                                accountId = acc.id,
                                note = note.trim(),
                                contactId = selectedContactId,
                                onSuccess = onDismiss,
                                onError = { err ->
                                    isSaving = false
                                    errorMessage = err
                                }
                            )
                        }
                    }
                }
            ) { Text(if (isSaving) "儲存中..." else "儲存") }
        },
        dismissButton = { TextButton(enabled = !isSaving, onClick = onDismiss) { Text("取消") } }
    )

    if (showAddContact) {
        ContactManagementDialog(
            contact = null,
            viewModel = viewModel,
            onDismiss = { showAddContact = false },
            onSaveSuccess = { id, name ->
                selectedContactId = id
                newContactName = name
                errorMessage = null
            }
        )
    }
}

@Composable
fun AddRepaymentDialog(loanWithDetails: LoanWithDetails, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val accounts by viewModel.accounts.collectAsState()
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    val selectedAccount = accounts.find { it.id == selectedAccountId }
        ?: accounts.find { it.id == loanWithDetails.loan.accountId }
        ?: accounts.firstOrNull()
    var dateMillis by remember { mutableStateOf(todayNoonMillis()) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("新增還款") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "剩餘尚未歸還 ${formatMoney(loanWithDetails.remainingAmount)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { v ->
                        if (v.length <= 12 && v.all { it.isDigit() }) {
                            amountText = v
                            errorMessage = null
                        }
                    },
                    label = { Text("還款金額 (TWD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )

                TextButton(onClick = { amountText = loanWithDetails.remainingAmount.toString() }) { Text("全額還清") }

                DropdownField(
                    label = "資金流向帳戶",
                    selectedText = selectedAccount?.name ?: "",
                    items = accounts,
                    itemText = { it.name },
                    onSelect = { selectedAccountId = it.id },
                    isError = selectedAccount == null
                )

                DatePickerField(label = "還款日期", millis = dateMillis, placeholder = "選擇日期", onPick = { dateMillis = it })

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註 (選填)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    val acc = selectedAccount
                    when {
                        amount <= 0 -> errorMessage = "還款金額必須大於 0"
                        acc == null -> errorMessage = "請選擇帳戶"
                        amount > loanWithDetails.remainingAmount ->
                            errorMessage = "還款金額不能超過剩餘欠款 (${formatMoney(loanWithDetails.remainingAmount)})"
                        else -> {
                            errorMessage = null
                            isSaving = true
                            viewModel.addLoanRepayment(
                                loanId = loanWithDetails.loan.id,
                                amount = amount,
                                dateMillis = dateMillis,
                                accountId = acc.id,
                                note = note.trim(),
                                onSuccess = onDismiss,
                                onError = { err ->
                                    isSaving = false
                                    errorMessage = err
                                }
                            )
                        }
                    }
                }
            ) { Text(if (isSaving) "儲存中..." else "確認還款") }
        },
        dismissButton = { TextButton(enabled = !isSaving, onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ContactManagementDialog(
    contact: ContactEntity?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSaveSuccess: ((Long, String) -> Unit)? = null
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var note by remember { mutableStateOf(contact?.note ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val isNew = contact == null || contact.id == 0L

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (isNew) "新增對象" else "編輯對象") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("姓名 (必填)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )
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
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isBlank()) {
                        errorMessage = "姓名不能為空"
                    } else {
                        errorMessage = null
                        isSaving = true
                        if (isNew) {
                            viewModel.addContact(cleanName, phone.trim(), note.trim(), onSuccess = { id ->
                                onSaveSuccess?.invoke(id, cleanName)
                                onDismiss()
                            }, onError = { err ->
                                isSaving = false
                                errorMessage = err
                            })
                        } else {
                            viewModel.updateContact(
                                contact!!.copy(name = cleanName, phone = phone.trim(), note = note.trim()),
                                onSuccess = {
                                    onSaveSuccess?.invoke(contact.id, cleanName)
                                    onDismiss()
                                },
                                onError = { err ->
                                    isSaving = false
                                    errorMessage = err
                                }
                            )
                        }
                    }
                }
            ) { Text(if (isSaving) "儲存中..." else "儲存") }
        },
        dismissButton = { TextButton(enabled = !isSaving, onClick = onDismiss) { Text("取消") } }
    )
}
