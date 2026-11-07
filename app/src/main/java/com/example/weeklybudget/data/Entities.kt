package com.example.weeklybudget.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ميزانية فترة واحدة. المفتاح هو الشهر الذي تبدأ فيه الفترة ("2026-11")،
 * وتبدأ الفترة فعليًا في اليوم [startDay] من ذلك الشهر وتنتهي قبل اليوم نفسه من الشهر التالي.
 */
@Entity(tableName = "month_budget")
data class MonthBudget(
    @PrimaryKey val monthKey: String,
    val income: Double,
    val startDay: Int = 1,
    /** ترحيل المتبقي (أو العجز) من الأسبوع إلى الأسبوع التالي. */
    val rollover: Boolean = true
)

/** فئة مصروف أسبوعية (النقل، الغداء، المصروف الشخصي، أو فئة يضيفها المستخدم). */
@Entity(tableName = "category")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    /** سقف أسبوعي اختياري؛ 0 يعني بلا سقف. */
    val weeklyLimit: Double = 0.0,
    val sortOrder: Int = 0
)

@Entity(tableName = "expense", indices = [Index("monthKey"), Index("categoryId")])
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,
    val epochDay: Long,
    val amount: Double,
    val categoryId: Long,
    val note: String = ""
)

/** فاتورة شهرية تخص فترة بعينها. */
@Entity(tableName = "bill", indices = [Index("monthKey")])
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,
    val name: String,
    val amount: Double,
    val iconKey: String,
    val dueDay: Int,
    val paid: Boolean = false
)

@Entity(tableName = "debt")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val total: Double,
    val monthlyInstallment: Double
)

@Entity(tableName = "debt_payment", indices = [Index("debtId")])
data class DebtPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val epochDay: Long,
    val amount: Double
)

@Entity(tableName = "savings_goal")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Double,
    val monthlyContribution: Double
)

@Entity(tableName = "savings_entry", indices = [Index("goalId")])
data class SavingsEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val epochDay: Long,
    val amount: Double,
    /** "SAVE" إن نشأ من ادخار فائض أسبوع، وإلا فارغ. */
    val source: String = ""
)

/** تعديل على ميزانية أسبوع: ادخار فائضه أو تغطية عجزه من صندوق الطوارئ. */
@Entity(tableName = "week_adjustment", indices = [Index("monthKey")])
data class WeekAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,
    val weekIndex: Int,
    /** [SAVE] أو [COVER]. */
    val kind: String,
    val amount: Double,
    val epochDay: Long,
    /** وجهة الادخار: -1 صندوق الطوارئ، وإلا رقم هدف الادخار. */
    val goalId: Long = -1,
    /** رقم القيد المرتبط في جدول الادخار أو الطوارئ، لحذفه عند التراجع. */
    val entryId: Long = 0
) {
    companion object {
        const val SAVE = "SAVE"
        const val COVER = "COVER"
    }
}

/** إيداع (موجب) أو سحب (سالب) في صندوق الطوارئ. */
@Entity(tableName = "emergency_entry")
data class EmergencyEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val amount: Double,
    val note: String = "",
    /** "SAVE" أو "COVER" إن نشأ من تعديل أسبوع، وإلا فارغ. */
    val source: String = ""
)

/** الملف الشخصي المحلي (سجل واحد، اختياري). */
@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: Int = 0,
    val name: String,
    /** لم يعد يُستعمل بعد إزالة التسجيل برقم الهاتف؛ بقي العمود لتفادي ترحيل جديد لقاعدة البيانات. */
    val phone: String = "",
    val email: String = "",
    val city: String = "",
    val currency: String = "دج",
    /** مسار صورة الملف الشخصي داخل مساحة التطبيق، أو فارغ. */
    val photoPath: String = "",
    val createdEpochDay: Long
)

@Entity(tableName = "emergency_settings")
data class EmergencySettings(
    @PrimaryKey val id: Int = 0,
    /** الحد المستهدف للصندوق؛ 0 يعني بلا هدف. */
    val target: Double = 0.0,
    val monthlyContribution: Double = 0.0
)
