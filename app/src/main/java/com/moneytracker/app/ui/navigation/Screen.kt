package com.moneytracker.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "首頁", Icons.Default.Home)
    object Transactions : Screen("transactions", "記帳", Icons.Default.AccountBalanceWallet)
    object Loans : Screen("loans", "借貸", Icons.Default.Handshake)
    object Statistics : Screen("statistics", "統計", Icons.Default.BarChart)
    object Settings : Screen("settings", "設定", Icons.Default.Settings)
}

val navItems = listOf(
    Screen.Home,
    Screen.Transactions,
    Screen.Loans,
    Screen.Statistics,
    Screen.Settings
)
