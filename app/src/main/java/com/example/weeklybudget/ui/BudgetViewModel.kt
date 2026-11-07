package com.example.weeklybudget.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.weeklybudget.data.AppDatabase
import com.example.weeklybudget.data.BackupJson
import com.example.weeklybudget.data.Bill
import com.example.weeklybudget.data.Category
import com.example.weeklybudget.data.Debt
import com.example.weeklybudget.data.DebtPayment
import com.example.weeklybudget.data.EmergencyEntry
import com.example.weeklybudget.data.EmergencySettings
import com.example.weeklybudget.data.Expense
import com.example.weeklybudget.data.MonthBudget
import com.example.weeklybudget.data.Profile
import com.example.weeklybudget.data.SavingsEntry
import com.example.weeklybudget.data.SavingsGoal
import com.example.weeklybudget.data.Snapshot
import com.example.weeklybudget.data.WeekAdjustment
import com.example.weeklybudget.domain.Period
import com.example.weeklybudget.domain.WeekSummary
import com.example.weeklybudget.domain.computeWeeks
import com.example.weeklybudget.domain.periodOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/** معرّف وجهة «صندوق الطوارئ» عند ادخار فائض أسبوع. */
const val EMERGENCY_ID = -1L

/** [loaded] = false حتى تُقرأ قاعدة البيانات، لئلا تومض شاشة التسجيل لمن سجّل سابقًا. */
data class ProfileState(val loaded: Boolean = false, val profile: Profile? = null)

data class DebtUi(val debt: Debt, val paid: Double, val history: List<DebtPayment>) {
    val remaining: Double get() = (debt.total - paid).coerceAtLeast(0.0)
    val progress: Float
        get() = if (debt.total <= 0) 0f else (paid / debt.total).toFloat().coerceIn(0f, 1f)
    /** القسط المخطط خصمه من الدخل هذه الفترة. */
    val monthlyDue: Double get() = minOf(debt.monthlyInstallment, remaining)
}

data class GoalUi(val goal: SavingsGoal, val saved: Double, val history: List<SavingsEntry>) {
    val remaining: Double get() = (goal.target - saved).coerceAtLeast(0.0)
    val progress: Float
        get() = if (goal.target <= 0) 0f else (saved / goal.target).toFloat().coerceIn(0f, 1f)
    val monthlyPlan: Double get() = minOf(goal.monthlyContribution, remaining)
}

data class EmergencyUi(
    val target: Double = 0.0,
    val monthlyContribution: Double = 0.0,
    val balance: Double = 0.0,
    val entries: List<EmergencyEntry> = emptyList()
) {
    val remaining: Double get() = if (target > 0) (target - balance).coerceAtLeast(0.0) else 0.0
    val progress: Float
        get() = if (target <= 0) 0f else (balance / target).toFloat().coerceIn(0f, 1f)
    /** المساهمة الشهرية المخصومة من الدخل (تتوقف عند بلوغ الهدف). */
    val monthlyPlan: Double
        get() = if (target > 0) minOf(monthlyContribution, remaining) else monthlyContribution
}

data class UiState(
    val period: Period = periodOf(YearMonth.now(), 1),
    val hasBudget: Boolean = false,
    val income: Double = 0.0,
    val startDay: Int = 1,
    val rollover: Boolean = true,
    val categories: List<Category> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val debts: List<DebtUi> = emptyList(),
    val goals: List<GoalUi> = emptyList(),
    val emergency: EmergencyUi = EmergencyUi(),
    val billsTotal: Double = 0.0,
    val debtPlanned: Double = 0.0,
    val savingsPlanned: Double = 0.0,
    val emergencyPlanned: Double = 0.0,
    val weeks: List<WeekSummary> = emptyList(),
    val totalSpent: Double = 0.0,
    val currentWeek: Int? = null
) {
    val obligations: Double get() = billsTotal + debtPlanned + savingsPlanned + emergencyPlanned
    val distributable: Double get() = income - obligations
    val totalSaved: Double get() = weeks.sumOf { it.saved }
    /** مجموع العجز المسجّل في أسابيع هذه الفترة. */
    val deficitTotal: Double get() = weeks.sumOf { it.deficit }
    val remaining: Double get() = distributable - totalSpent - totalSaved
    val progress: Float
        get() = when {
            distributable <= 0.0 -> if (totalSpent > 0) 1f else 0f
            else -> ((totalSpent + totalSaved) / distributable).toFloat().coerceIn(0f, 1f)
        }
}

private data class Core(
    val budget: MonthBudget?,
    val latest: MonthBudget?,
    val expenses: List<Expense>,
    val bills: List<Bill>,
    val categories: List<Category>
)

private data class Commitments(
    val debts: List<Debt>,
    val debtPayments: List<DebtPayment>,
    val goals: List<SavingsGoal>,
    val entries: List<SavingsEntry>,
    val emergencySettings: EmergencySettings?
)

private data class Extras(
    val adjustments: List<WeekAdjustment>,
    val emergencyEntries: List<EmergencyEntry>
)

class BudgetViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val dao = db.budgetDao()
    private val month = MutableStateFlow(YearMonth.now())

    val profileState: StateFlow<ProfileState> = dao.observeProfile()
        .map { ProfileState(loaded = true, profile = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProfileState())

    init {
        // عملة العرض تتبع الملف الشخصي.
        viewModelScope.launch {
            profileState.collect { Currency.label = it.profile?.currency ?: DEFAULT_CURRENCY }
        }
    }

    init {
        viewModelScope.launch {
            if (dao.countCategories() == 0) {
                dao.insertCategory(Category(name = "النقل", iconKey = "bus", sortOrder = 0))
                dao.insertCategory(Category(name = "الغداء", iconKey = "food", sortOrder = 1))
                dao.insertCategory(Category(name = "مصروف شخصي", iconKey = "person", sortOrder = 2))
            }
            // إن كان اليوم قبل بداية فترة هذا الشهر، فالفترة الجارية هي فترة الشهر السابق.
            dao.latestBudget()?.let { latest ->
                val today = LocalDate.now()
                val start = periodOf(YearMonth.from(today), latest.startDay).start
                if (today.isBefore(start)) month.value = YearMonth.from(today).minusMonths(1)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<UiState> = month.flatMapLatest { ym ->
        val key = ym.toString()
        val core = combine(
            dao.observeBudget(key),
            dao.observeLatestBudget(),
            dao.observeExpenses(key),
            dao.observeBills(key),
            dao.observeCategories()
        ) { b, l, e, bl, c -> Core(b, l, e, bl, c) }
        val commitments = combine(
            dao.observeDebts(),
            dao.observeDebtPayments(),
            dao.observeGoals(),
            dao.observeSavingsEntries(),
            dao.observeEmergencySettings()
        ) { d, dp, g, se, es -> Commitments(d, dp, g, se, es) }
        val extras = combine(
            dao.observeAdjustments(key),
            dao.observeEmergencyEntries()
        ) { a, ee -> Extras(a, ee) }
        combine(core, commitments, extras) { c, k, x -> build(ym, c, k, x) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private fun build(ym: YearMonth, c: Core, k: Commitments, x: Extras): UiState {
        val income = c.budget?.income ?: 0.0
        val startDay = c.budget?.startDay ?: c.latest?.startDay ?: 1
        val rollover = c.budget?.rollover ?: true
        val period = periodOf(ym, startDay)

        val debts = k.debts.map { d ->
            val h = k.debtPayments.filter { it.debtId == d.id }
            DebtUi(d, h.sumOf { it.amount }, h)
        }
        val goals = k.goals.map { g ->
            val h = k.entries.filter { it.goalId == g.id }
            GoalUi(g, h.sumOf { it.amount }, h)
        }
        val emergency = EmergencyUi(
            target = k.emergencySettings?.target ?: 0.0,
            monthlyContribution = k.emergencySettings?.monthlyContribution ?: 0.0,
            balance = x.emergencyEntries.sumOf { it.amount },
            entries = x.emergencyEntries
        )

        val billsTotal = c.bills.sumOf { it.amount }
        val debtPlanned = debts.sumOf { it.monthlyDue }
        val savingsPlanned = goals.sumOf { it.monthlyPlan }
        val emergencyPlanned = emergency.monthlyPlan
        val distributable = income - billsTotal - debtPlanned - savingsPlanned - emergencyPlanned

        val today = LocalDate.now()
        return UiState(
            period = period,
            hasBudget = c.budget != null,
            income = income,
            startDay = startDay,
            rollover = rollover,
            categories = c.categories,
            expenses = c.expenses,
            bills = c.bills,
            debts = debts,
            goals = goals,
            emergency = emergency,
            billsTotal = billsTotal,
            debtPlanned = debtPlanned,
            savingsPlanned = savingsPlanned,
            emergencyPlanned = emergencyPlanned,
            weeks = computeWeeks(period, distributable, rollover, c.expenses, c.categories, x.adjustments),
            totalSpent = c.expenses.sumOf { it.amount },
            currentWeek = if (period.contains(today)) period.weekIndexOf(today) else null
        )
    }

    // ---- التنقل بين الفترات ----
    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }

    // ---- الدخل والفترة ----
    fun saveBudget(income: Double, startDay: Int, rollover: Boolean) = viewModelScope.launch {
        dao.upsertBudget(MonthBudget(month.value.toString(), income, startDay.coerceIn(1, 31), rollover))
    }

    // ---- المصاريف ----
    fun addExpense(amount: Double, categoryId: Long, note: String, date: LocalDate) = viewModelScope.launch {
        dao.insertExpense(
            Expense(
                monthKey = month.value.toString(),
                epochDay = date.toEpochDay(),
                amount = amount,
                categoryId = categoryId,
                note = note
            )
        )
    }

    fun deleteExpense(e: Expense) = viewModelScope.launch { dao.deleteExpense(e) }

    // ---- الفئات ----
    fun addCategory(name: String, iconKey: String, weeklyLimit: Double) = viewModelScope.launch {
        val order = state.value.categories.size
        dao.insertCategory(Category(name = name, iconKey = iconKey, weeklyLimit = weeklyLimit, sortOrder = order))
    }

    fun updateCategoryLimit(c: Category, limit: Double) = viewModelScope.launch {
        dao.updateCategory(c.copy(weeklyLimit = limit))
    }

    fun deleteCategory(c: Category) = viewModelScope.launch { dao.deleteCategory(c) }

    // ---- الفواتير ----
    fun addBill(name: String, amount: Double, iconKey: String, dueDay: Int) = viewModelScope.launch {
        dao.insertBill(Bill(monthKey = month.value.toString(), name = name, amount = amount, iconKey = iconKey, dueDay = dueDay))
    }

    fun toggleBillPaid(b: Bill) = viewModelScope.launch { dao.updateBill(b.copy(paid = !b.paid)) }
    fun deleteBill(b: Bill) = viewModelScope.launch { dao.deleteBill(b) }

    fun copyBillsFromPreviousMonth() = viewModelScope.launch {
        val previous = dao.billsOf(month.value.minusMonths(1).toString())
        previous.forEach { dao.insertBill(it.copy(id = 0, monthKey = month.value.toString(), paid = false)) }
    }

    // ---- الديون ----
    fun addDebt(name: String, total: Double, installment: Double) = viewModelScope.launch {
        dao.insertDebt(Debt(name = name, total = total, monthlyInstallment = installment))
    }

    fun deleteDebt(id: Long) = viewModelScope.launch {
        dao.deleteDebtPayments(id)
        dao.deleteDebt(id)
    }

    fun addDebtPayment(debtId: Long, amount: Double) = viewModelScope.launch {
        dao.insertDebtPayment(DebtPayment(debtId = debtId, epochDay = LocalDate.now().toEpochDay(), amount = amount))
    }

    fun deleteDebtPayment(p: DebtPayment) = viewModelScope.launch { dao.deleteDebtPayment(p) }

    // ---- الادخار ----
    fun addGoal(name: String, target: Double, monthly: Double) = viewModelScope.launch {
        dao.insertGoal(SavingsGoal(name = name, target = target, monthlyContribution = monthly))
    }

    fun deleteGoal(id: Long) = viewModelScope.launch {
        dao.deleteSavingsEntries(id)
        dao.deleteGoal(id)
    }

    fun addSavingsEntry(goalId: Long, amount: Double) = viewModelScope.launch {
        dao.insertSavingsEntry(SavingsEntry(goalId = goalId, epochDay = LocalDate.now().toEpochDay(), amount = amount))
    }

    fun deleteSavingsEntry(e: SavingsEntry) = viewModelScope.launch { dao.deleteSavingsEntry(e) }

    // ---- ادخار فائض الأسبوع وتغطية عجزه ----
    /** ينقل [amount] من فائض الأسبوع إلى صندوق الطوارئ ([EMERGENCY_ID]) أو إلى هدف ادخار. */
    fun saveWeekSurplus(weekIndex: Int, amount: Double, goalId: Long) = viewModelScope.launch {
        val today = LocalDate.now().toEpochDay()
        val entryId = if (goalId == EMERGENCY_ID) {
            dao.insertEmergencyEntry(
                EmergencyEntry(epochDay = today, amount = amount, note = "فائض الأسبوع ${weekIndex + 1}", source = WeekAdjustment.SAVE)
            )
        } else {
            dao.insertSavingsEntry(
                SavingsEntry(goalId = goalId, epochDay = today, amount = amount, source = WeekAdjustment.SAVE)
            )
        }
        dao.insertAdjustment(
            WeekAdjustment(
                monthKey = month.value.toString(), weekIndex = weekIndex, kind = WeekAdjustment.SAVE,
                amount = amount, epochDay = today, goalId = goalId, entryId = entryId
            )
        )
    }

    /** يغطي [amount] من عجز الأسبوع بسحبها من صندوق الطوارئ. */
    fun coverWeekDeficit(weekIndex: Int, amount: Double) = viewModelScope.launch {
        val today = LocalDate.now().toEpochDay()
        val entryId = dao.insertEmergencyEntry(
            EmergencyEntry(epochDay = today, amount = -amount, note = "تغطية عجز الأسبوع ${weekIndex + 1}", source = WeekAdjustment.COVER)
        )
        dao.insertAdjustment(
            WeekAdjustment(
                monthKey = month.value.toString(), weekIndex = weekIndex, kind = WeekAdjustment.COVER,
                amount = amount, epochDay = today, goalId = EMERGENCY_ID, entryId = entryId
            )
        )
    }

    /** يتراجع عن ادخار أو تغطية ويحذف قيدهما المرتبط. */
    fun undoAdjustment(a: WeekAdjustment) = viewModelScope.launch {
        if (a.kind == WeekAdjustment.SAVE && a.goalId != EMERGENCY_ID) {
            dao.deleteSavingsEntryById(a.entryId)
        } else {
            dao.deleteEmergencyEntry(a.entryId)
        }
        dao.deleteAdjustment(a.id)
    }

    // ---- صندوق الطوارئ ----
    /** [amount] موجب للإيداع وسالب للسحب. */
    fun addEmergencyEntry(amount: Double, note: String) = viewModelScope.launch {
        dao.insertEmergencyEntry(EmergencyEntry(epochDay = LocalDate.now().toEpochDay(), amount = amount, note = note))
    }

    fun deleteEmergencyEntry(e: EmergencyEntry) = viewModelScope.launch { dao.deleteEmergencyEntry(e.id) }

    fun saveEmergencySettings(target: Double, monthly: Double) = viewModelScope.launch {
        dao.upsertEmergencySettings(EmergencySettings(0, target, monthly))
    }

    // ---- الملف الشخصي ----
    /** الملف الشخصي اختياري، فيُنشأ عند أول حفظ إن لم يوجد. */
    private suspend fun currentOrNewProfile(): Profile =
        dao.profile() ?: Profile(name = "", phone = "", createdEpochDay = LocalDate.now().toEpochDay())

    fun saveProfile(name: String, email: String, city: String, currency: String) = viewModelScope.launch {
        dao.upsertProfile(currentOrNewProfile().copy(name = name, email = email, city = city, currency = currency))
    }

    /** يحفظ مسار الصورة الجديدة ويحذف القديمة. مرّر نصًا فارغًا لإزالة الصورة. */
    fun setProfilePhoto(path: String) = viewModelScope.launch {
        val current = currentOrNewProfile()
        if (current.photoPath.isNotBlank()) withContext(Dispatchers.IO) { File(current.photoPath).delete() }
        dao.upsertProfile(current.copy(photoPath = path))
    }

    /** يحذف الملف الشخصي وصورته، وتبقى البيانات المالية. */
    fun deleteProfile() = viewModelScope.launch {
        val current = dao.profile()
        if (current != null && current.photoPath.isNotBlank()) {
            withContext(Dispatchers.IO) { File(current.photoPath).delete() }
        }
        dao.clearProfile()
    }

    // ---- النسخ الاحتياطي ----
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        BackupJson.encode(
            Snapshot(
                dao.allBudgets(), dao.allCategories(), dao.allExpenses(), dao.allBills(),
                dao.allDebts(), dao.allDebtPayments(), dao.allGoals(), dao.allSavingsEntries(),
                dao.allAdjustments(), dao.allEmergencyEntries(), dao.emergencySettings(), dao.profile()
            )
        )
    }

    /** يستبدل كل البيانات الحالية بمحتوى النسخة. يعيد false إن كان الملف غير صالح (ولا يتغير شيء). */
    suspend fun importJson(text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val s = BackupJson.decode(text)
            db.withTransaction {
                dao.clearAdjustments(); dao.clearEmergencyEntries(); dao.clearEmergencySettings()
                dao.clearSavingsEntries(); dao.clearGoals(); dao.clearDebtPayments(); dao.clearDebts()
                dao.clearBills(); dao.clearExpenses(); dao.clearCategories(); dao.clearBudgets()

                dao.putBudgets(s.budgets); dao.putCategories(s.categories); dao.putExpenses(s.expenses)
                dao.putBills(s.bills); dao.putDebts(s.debts); dao.putDebtPayments(s.debtPayments)
                dao.putGoals(s.goals); dao.putSavingsEntries(s.entries)
                dao.putAdjustments(s.adjustments); dao.putEmergencyEntries(s.emergencyEntries)
                s.emergencySettings?.let { dao.upsertEmergencySettings(it) }
                // لا يُستبدل الملف الشخصي الحالي إلا إن حوت النسخة ملفًا شخصيًا، وتُحفظ صورته الحالية.
                s.profile?.let { restored ->
                    val photo = dao.profile()?.photoPath ?: ""
                    dao.upsertProfile(restored.copy(photoPath = photo))
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
