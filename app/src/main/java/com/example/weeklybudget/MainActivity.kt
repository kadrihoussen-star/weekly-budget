package com.example.weeklybudget

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.fragment.app.FragmentActivity
import com.example.weeklybudget.security.LockManager
import com.example.weeklybudget.ui.AppRoot
import com.example.weeklybudget.ui.BudgetViewModel
import com.example.weeklybudget.ui.LockScreen
import com.example.weeklybudget.ui.WeeklyBudgetTheme

/** FragmentActivity (وليس ComponentActivity) لأن نافذة البصمة تحتاجها. */
class MainActivity : FragmentActivity() {

    private val viewModel: BudgetViewModel by viewModels()
    private lateinit var lock: LockManager

    private var locked by mutableStateOf(false)
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lock = LockManager(this)
        locked = lock.isEnabled
        enableEdgeToEdge()
        setContent {
            WeeklyBudgetTheme {
                // الواجهة عربية بالكامل، فنفرض الاتجاه من اليمين إلى اليسار.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(Modifier.fillMaxSize()) {
                        AppRoot(viewModel, lock)
                        if (locked) {
                            LockScreen(lock, promptKey = resumeTick) { locked = false }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        resumeTick++
    }

    override fun onPause() {
        super.onPause()
        // يخفي محتوى التطبيق في قائمة التطبيقات الأخيرة عند تفعيل القفل (دون منع لقطة الشاشة أثناء الاستعمال).
        if (lock.isEnabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onStop() {
        super.onStop()
        if (lock.isEnabled) locked = true
    }
}
