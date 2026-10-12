package com.moneytracker.app.data.repository

import com.moneytracker.app.data.db.AppDatabase
import com.moneytracker.app.data.entity.AccountEntity
import com.moneytracker.app.data.entity.CategoryEntity
import com.moneytracker.app.data.entity.ContactEntity
import com.moneytracker.app.data.entity.LoanEntity
import com.moneytracker.app.data.entity.LoanRepaymentEntity
import com.moneytracker.app.data.entity.TransactionEntity
import com.moneytracker.app.data.model.LoanStatus
import com.moneytracker.app.data.model.LoanType
import com.moneytracker.app.data.model.TransactionType
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class LoanWithDetails(
    val loan: LoanEntity,
    val contact: ContactEntity?,
    val account: AccountEntity?,
    val repayments: List<LoanRepaymentEntity>,
    val repaidAmount: Long,
    val remainingAmount: Long
)

data class ContactSummary(
    val contact: ContactEntity,
    val totalLendAmount: Long, // 總借出
    val totalLendRepaid: Long, // 借出已歸還
    val outstandingLend: Long, // 尚欠借出 (未收回)
    val totalBorrowAmount: Long, // 總借入
    val totalBorrowRepaid: Long, // 借入已歸還
    val outstandingBorrow: Long // 尚欠借入 (未償還)
)

data class AccountWithBalance(
    val account: AccountEntity,
    val currentBalance: Long
)

class AppRepository(private val db: AppDatabase) {

    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val contactDao = db.contactDao()
    private val loanDao = db.loanDao()
    private val loanRepaymentDao = db.loanRepaymentDao()
    private val settingsDao = db.settingsAndBackupDao()

    // -------------------------------------------------------------
    // Accounts
    // -------------------------------------------------------------
    fun getAllAccountsFlow(): Flow<List<AccountEntity>> = accountDao.getAllAccountsFlow()

    suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)

    suspend fun calculateAccountBalance(accountId: Long): Long {
        val account = accountDao.getAccountById(accountId) ?: return 0L
        val initial = account.initialBalance
        val income = transactionDao.getTotalIncomeForAccount(accountId)
        val expense = transactionDao.getTotalExpenseForAccount(accountId)
        val transferOut = transactionDao.getTotalTransferOutForAccount(accountId)
        val transferIn = transactionDao.getTotalTransferInForAccount(accountId)

        return initial + income - expense - transferOut + transferIn
    }

    fun getAllAccountsWithBalanceFlow(): Flow<List<AccountWithBalance>> {
        return accountDao.getAllAccountsFlow().combine(transactionDao.getAllTransactionsFlow()) { accounts, _ ->
            accounts.map { account ->
                AccountWithBalance(account, calculateAccountBalance(account.id))
            }
        }
    }

    suspend fun insertAccount(account: AccountEntity): Long = accountDao.insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)
    suspend fun softDeleteAccount(id: Long) = accountDao.softDeleteAccount(id)

    // -------------------------------------------------------------
    // Categories
    // -------------------------------------------------------------
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()
    fun getCategoriesByTypeFlow(type: TransactionType): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByTypeFlow(type)
    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)
    suspend fun softDeleteCategory(id: Long) = categoryDao.softDeleteCategory(id)

    // -------------------------------------------------------------
    // Transactions
    // -------------------------------------------------------------
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactionsFlow()
    suspend fun insertTransaction(transaction: TransactionEntity): Long = transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: TransactionEntity) = transactionDao.updateTransaction(transaction)
    suspend fun softDeleteTransaction(id: Long) = transactionDao.softDeleteTransaction(id)

    fun getIncomeSumBetweenFlow(startMillis: Long, endMillis: Long): Flow<Long> =
        transactionDao.getIncomeSumBetweenFlow(startMillis, endMillis)

    fun getExpenseSumBetweenFlow(startMillis: Long, endMillis: Long): Flow<Long> =
        transactionDao.getExpenseSumBetweenFlow(startMillis, endMillis)

    // -------------------------------------------------------------
    // Contacts
    // -------------------------------------------------------------
    fun getAllContactsFlow(): Flow<List<ContactEntity>> = contactDao.getAllContactsFlow()
    suspend fun getOrCreateContact(name: String, phone: String = ""): ContactEntity {
        val existing = contactDao.getContactByName(name.trim())
        if (existing != null) return existing
        val id = contactDao.insertContact(ContactEntity(name = name.trim(), phone = phone.trim()))
        return contactDao.getContactById(id)!!
    }

    fun getContactSummariesFlow(): Flow<List<ContactSummary>> {
        return combine(
            contactDao.getAllContactsFlow(),
            loanDao.getAllLoansFlow()
        ) { contacts, loans ->
            contacts.map { contact ->
                val contactLoans = loans.filter { it.contactId == contact.id }
                var totalLend = 0L
                var totalLendRepaid = 0L
                var totalBorrow = 0L
                var totalBorrowRepaid = 0L

                for (loan in contactLoans) {
                    val repaid = loanRepaymentDao.getTotalRepaidForLoan(loan.id)
                    if (loan.type == LoanType.LEND) {
                        totalLend += loan.amount
                        totalLendRepaid += repaid
                    } else {
                        totalBorrow += loan.amount
                        totalBorrowRepaid += repaid
                    }
                }

                ContactSummary(
                    contact = contact,
                    totalLendAmount = totalLend,
                    totalLendRepaid = totalLendRepaid,
                    outstandingLend = (totalLend - totalLendRepaid).coerceAtLeast(0L),
                    totalBorrowAmount = totalBorrow,
                    totalBorrowRepaid = totalBorrowRepaid,
                    outstandingBorrow = (totalBorrow - totalBorrowRepaid).coerceAtLeast(0L)
                )
            }
        }
    }

    // -------------------------------------------------------------
    // Loans & Repayments
    // -------------------------------------------------------------
    fun getAllLoansWithDetailsFlow(): Flow<List<LoanWithDetails>> {
        return combine(
            loanDao.getAllLoansFlow(),
            accountDao.getAllAccountsFlow(),
            contactDao.getAllContactsFlow()
        ) { loans, accounts, contacts ->
            loans.map { loan ->
                val contact = contacts.find { it.id == loan.contactId }
                val account = accounts.find { it.id == loan.accountId }
                val repayments = loanRepaymentDao.getRepaymentsByLoanId(loan.id)
                val repaid = repayments.sumOf { it.amount }
                val remaining = (loan.amount - repaid).coerceAtLeast(0L)

                LoanWithDetails(
                    loan = loan,
                    contact = contact,
                    account = account,
                    repayments = repayments,
                    repaidAmount = repaid,
                    remainingAmount = remaining
                )
            }
        }
    }

    suspend fun getLoanWithDetails(loanId: Long): LoanWithDetails? {
        val loan = loanDao.getLoanById(loanId) ?: return null
        val contact = contactDao.getContactById(loan.contactId)
        val account = accountDao.getAccountById(loan.accountId)
        val repayments = loanRepaymentDao.getRepaymentsByLoanId(loanId)
        val repaid = repayments.sumOf { it.amount }
        val remaining = (loan.amount - repaid).coerceAtLeast(0L)

        return LoanWithDetails(loan, contact, account, repayments, repaid, remaining)
    }

    /**
     * 新增借貸：
     * - 寫入 LoanEntity
     * - 建立金流 TransactionEntity（反映至帳戶餘額，但包含 loanId 排除於一般收支報表）
     *   - 借出 (LEND): 資金從 accountId 流出 (type = EXPENSE, loanId = loanId, sourceAccountId = accountId)
     *   - 借入 (BORROW): 資金流入 accountId (type = INCOME, loanId = loanId, sourceAccountId = accountId)
     */
    suspend fun createLoanTransaction(
        contactName: String,
        type: LoanType,
        amount: Long,
        dateMillis: Long,
        dueDateMillis: Long?,
        accountId: Long,
        note: String
    ): Long {
        return db.withTransaction {
            val contact = getOrCreateContact(contactName)

            val loan = LoanEntity(
                contactId = contact.id,
                type = type,
                amount = amount,
                dateMillis = dateMillis,
                dueDateMillis = dueDateMillis,
                accountId = accountId,
                note = note,
                status = LoanStatus.UNPAID
            )

            val loanId = loanDao.insertLoan(loan)

            val transactionType = if (type == LoanType.LEND) TransactionType.EXPENSE else TransactionType.INCOME
            val txNote = if (type == LoanType.LEND) "借出款項給 ${contact.name}" else "向 ${contact.name} 借入款項"

            transactionDao.insertTransaction(
                TransactionEntity(
                    type = transactionType,
                    amount = amount,
                    dateMillis = dateMillis,
                    sourceAccountId = accountId,
                    note = if (note.isBlank()) txNote else "$txNote - $note",
                    loanId = loanId
                )
            )

            loanId
        }
    }

    /**
     * 新增還款 (分次還款)：
     * - 檢查累計還款不得超過借貸款項本金！
     * - 寫入 LoanRepaymentEntity
     * - 建立金流 TransactionEntity (帶有 repaymentId，排除於一般收支)
     *   - 借給別人的還款 (LEND): 資金收回流入帳戶 (type = INCOME, sourceAccountId = accountId)
     *   - 向別人借的還款 (BORROW): 資金還出流出帳戶 (type = EXPENSE, sourceAccountId = accountId)
     * - 更新 Loan 狀態 (UNPAID / PARTIAL / PAID)
     */
    suspend fun addRepayment(
        loanId: Long,
        amount: Long,
        dateMillis: Long,
        accountId: Long,
        note: String
    ): Result<Long> {
        return try {
            val repaymentId = db.withTransaction {
                val loan = loanDao.getLoanById(loanId) ?: throw IllegalArgumentException("借貸紀錄不存在")
                val currentRepaid = loanRepaymentDao.getTotalRepaidForLoan(loanId)

                if (currentRepaid + amount > loan.amount) {
                    throw IllegalArgumentException("累積還款金額不能超過借貸本金 ($${loan.amount})")
                }

                val repayment = LoanRepaymentEntity(
                    loanId = loanId,
                    amount = amount,
                    dateMillis = dateMillis,
                    accountId = accountId,
                    note = note
                )
                val id = loanRepaymentDao.insertRepayment(repayment)

                val txType = if (loan.type == LoanType.LEND) TransactionType.INCOME else TransactionType.EXPENSE
                val contact = contactDao.getContactById(loan.contactId)
                val contactName = contact?.name ?: ""
                val txNote = if (loan.type == LoanType.LEND) "$contactName 還款" else "償還 $contactName"

                transactionDao.insertTransaction(
                    TransactionEntity(
                        type = txType,
                        amount = amount,
                        dateMillis = dateMillis,
                        sourceAccountId = accountId,
                        note = if (note.isBlank()) txNote else "$txNote - $note",
                        repaymentId = id
                    )
                )

                // 重新計算並更新狀態
                recalculateAndUpdateLoanStatus(loanId)

                id
            }
            Result.success(repaymentId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 刪除/修改還款
     */
    suspend fun deleteRepayment(repaymentId: Long) {
        db.withTransaction {
            val repayment = loanRepaymentDao.getRepaymentById(repaymentId) ?: return@withTransaction
            val loanId = repayment.loanId

            loanRepaymentDao.softDeleteRepayment(repaymentId)
            transactionDao.softDeleteTransactionsByRepaymentId(repaymentId)

            recalculateAndUpdateLoanStatus(loanId)
        }
    }

    /**
     * 刪除主借貸紀錄及相關還款與金流
     */
    suspend fun deleteLoan(loanId: Long) {
        db.withTransaction {
            val repayments = loanRepaymentDao.getRepaymentsByLoanId(loanId)

            loanDao.softDeleteLoan(loanId)
            loanRepaymentDao.softDeleteRepaymentsByLoanId(loanId)
            transactionDao.softDeleteTransactionsByLoanId(loanId)

            repayments.forEach {
                transactionDao.softDeleteTransactionsByRepaymentId(it.id)
            }
        }
    }

    suspend fun recalculateAndUpdateLoanStatus(loanId: Long) {
        val loan = loanDao.getLoanById(loanId) ?: return
        val totalRepaid = loanRepaymentDao.getTotalRepaidForLoan(loanId)

        val newStatus = when {
            totalRepaid >= loan.amount -> LoanStatus.PAID
            totalRepaid > 0 -> LoanStatus.PARTIAL
            else -> LoanStatus.UNPAID
        }

        if (loan.status != newStatus) {
            loanDao.updateLoan(loan.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
        }
    }

    // 尚未收回 (我借出) 與 尚未償還 (我借入)
    fun getOutstandingLendTotalFlow(): Flow<Long> {
        return getAllLoansWithDetailsFlow().map { list ->
            list.filter { it.loan.type == LoanType.LEND }.sumOf { it.remainingAmount }
        }
    }

    fun getOutstandingBorrowTotalFlow(): Flow<Long> {
        return getAllLoansWithDetailsFlow().map { list ->
            list.filter { it.loan.type == LoanType.BORROW }.sumOf { it.remainingAmount }
        }
    }
}
