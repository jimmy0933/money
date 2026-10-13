package com.moneytracker.app.data.backup

object BackupPayloadValidator {

    class ValidationException(message: String) : IllegalArgumentException(message)

    fun validate(payload: BackupPayload) {
        if (payload.schemaVersion < 1 || payload.schemaVersion > 1) {
            throw ValidationException("不支援的備份版本: ${payload.schemaVersion}")
        }

        val accountIds = mutableSetOf<Long>()
        for (account in payload.accounts) {
            if (account.name.isBlank()) {
                throw ValidationException("帳戶資料錯誤: 帳戶名稱不可為空")
            }
            if (!accountIds.add(account.id)) {
                throw ValidationException("帳戶資料錯誤: 存在重複的帳戶 ID (${account.id})")
            }
        }

        val categoryIds = mutableSetOf<Long>()
        for (category in payload.categories) {
            if (category.name.isBlank()) {
                throw ValidationException("分類資料錯誤: 分類名稱不可為空")
            }
            if (!categoryIds.add(category.id)) {
                throw ValidationException("分類資料錯誤: 存在重複的分類 ID (${category.id})")
            }
        }

        val contactIds = mutableSetOf<Long>()
        for (contact in payload.contacts) {
            if (contact.name.isBlank()) {
                throw ValidationException("聯絡人資料錯誤: 姓名不可為空")
            }
            if (!contactIds.add(contact.id)) {
                throw ValidationException("聯絡人資料錯誤: 存在重複的聯絡人 ID (${contact.id})")
            }
        }

        val loanIds = mutableSetOf<Long>()
        for (loan in payload.loans) {
            if (loan.amount < 0) {
                throw ValidationException("借貸資料錯誤: 金額不可為負值")
            }
            if (!contactIds.contains(loan.contactId)) {
                throw ValidationException("借貸資料錯誤: 找不到對應的聯絡人 ID (${loan.contactId})")
            }
            if (!loanIds.add(loan.id)) {
                throw ValidationException("借貸資料錯誤: 存在重複的借貸 ID (${loan.id})")
            }
        }

        val repaymentIds = mutableSetOf<Long>()
        for (repayment in payload.loanRepayments) {
            if (repayment.amount < 0) {
                throw ValidationException("還款資料錯誤: 金額不可為負值")
            }
            if (!loanIds.contains(repayment.loanId)) {
                throw ValidationException("還款資料錯誤: 找不到對應的借貸紀錄 ID (${repayment.loanId})")
            }
            if (!accountIds.contains(repayment.accountId)) {
                throw ValidationException("還款資料錯誤: 找不到對應的帳戶 ID (${repayment.accountId})")
            }
            if (!repaymentIds.add(repayment.id)) {
                throw ValidationException("還款資料錯誤: 存在重複的還款 ID (${repayment.id})")
            }
        }

        val transactionIds = mutableSetOf<Long>()
        for (tx in payload.transactions) {
            if (tx.amount < 0) {
                throw ValidationException("交易資料錯誤: 金額不可為負值")
            }
            if (!accountIds.contains(tx.sourceAccountId)) {
                throw ValidationException("交易資料錯誤: 來源帳戶 ID (${tx.sourceAccountId}) 不存在")
            }
            if (tx.targetAccountId != null && !accountIds.contains(tx.targetAccountId)) {
                throw ValidationException("交易資料錯誤: 目標帳戶 ID (${tx.targetAccountId}) 不存在")
            }
            if (tx.categoryId != null && !categoryIds.contains(tx.categoryId)) {
                throw ValidationException("交易資料錯誤: 分類 ID (${tx.categoryId}) 不存在")
            }
            if (!transactionIds.add(tx.id)) {
                throw ValidationException("交易資料錯誤: 存在重複的交易 ID (${tx.id})")
            }
        }
    }
}
