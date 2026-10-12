package com.moneytracker.app.data.model

enum class AccountType(val label: String) {
    CASH("現金"),
    BANK("銀行帳戶"),
    E_PAYMENT("電子支付"),
    CREDIT_CARD("信用卡")
}

enum class TransactionType(val label: String) {
    EXPENSE("支出"),
    INCOME("收入"),
    TRANSFER("轉帳")
}

enum class LoanType(val label: String) {
    LEND("借出"),  // 我借給別人
    BORROW("借入") // 我向別人借
}

enum class LoanStatus(val label: String) {
    UNPAID("未還款"),
    PARTIAL("部分還款"),
    PAID("已結清")
}
