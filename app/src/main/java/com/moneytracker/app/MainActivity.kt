package com.moneytracker.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moneytracker.app.ui.theme.MoneyTrackerTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moneytracker.app.ui.navigation.Screen
import com.moneytracker.app.ui.navigation.navItems
import com.moneytracker.app.ui.screens.HomeScreen
import com.moneytracker.app.ui.screens.LoansScreen
import com.moneytracker.app.ui.screens.SettingsScreen
import com.moneytracker.app.ui.screens.StatisticsScreen
import com.moneytracker.app.ui.screens.TransactionsScreen
import com.moneytracker.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MoneyTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppLayout(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val uiMessage by viewModel.uiMessage.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUiMessage()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                navItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(viewModel = viewModel)
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen(viewModel = viewModel)
            }
            composable(Screen.Loans.route) {
                LoansScreen(viewModel = viewModel)
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
