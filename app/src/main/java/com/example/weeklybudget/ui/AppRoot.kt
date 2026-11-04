package com.example.weeklybudget.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.weeklybudget.security.LockManager
import java.time.format.TextStyle
import java.util.Locale

private enum class AppTab(val label: String, val icon: ImageVector) {
    Home("الرئيسية", Icons.Filled.Home),
    Expenses("المصاريف", Icons.Filled.Payments),
    Bills("الفواتير", Icons.Filled.ReceiptLong),
    Savings("ادخار وديون", Icons.Filled.AccountBalanceWallet),
    Settings("الإعدادات", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: BudgetViewModel, lock: LockManager) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tabs = AppTab.values()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        state.period.ym.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ar")) +
                            " " + state.period.ym.year,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = vm::previousMonth) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "الشهر السابق")
                    }
                },
                actions = {
                    IconButton(onClick = vm::nextMonth) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "الشهر التالي")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            when (tabs[selected]) {
                AppTab.Home -> HomeScreen(state, vm, onOpenSettings = { selected = AppTab.Settings.ordinal })
                AppTab.Expenses -> ExpensesScreen(state, vm)
                AppTab.Bills -> BillsScreen(state, vm)
                AppTab.Savings -> SavingsDebtsScreen(state, vm)
                AppTab.Settings -> SettingsScreen(state, vm, lock)
            }
        }
    }
}
