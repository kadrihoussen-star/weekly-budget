package com.example.weeklybudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.weeklybudget.ui.AppRoot
import com.example.weeklybudget.ui.BudgetViewModel
import com.example.weeklybudget.ui.WeeklyBudgetTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BudgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeeklyBudgetTheme {
                // الواجهة عربية بالكامل، فنفرض الاتجاه من اليمين إلى اليسار.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppRoot(viewModel)
                }
            }
        }
    }
}
