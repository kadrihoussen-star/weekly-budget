package com.example.weeklybudget.data

import org.json.JSONArray
import org.json.JSONObject

/** لقطة كاملة من بيانات التطبيق. */
data class Snapshot(
    val budgets: List<MonthBudget>,
    val categories: List<Category>,
    val expenses: List<Expense>,
    val bills: List<Bill>,
    val debts: List<Debt>,
    val debtPayments: List<DebtPayment>,
    val goals: List<SavingsGoal>,
    val entries: List<SavingsEntry>,
    val adjustments: List<WeekAdjustment>,
    val emergencyEntries: List<EmergencyEntry>,
    val emergencySettings: EmergencySettings?
)

/** تحويل اللقطة إلى JSON وبالعكس. يستعمل org.json المضمّنة في أندرويد، فلا مكتبات إضافية. */
object BackupJson {
    private const val FORMAT = "weekly-budget-backup"
    private const val VERSION = 2

    private fun <T> JSONArray?.toList(f: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else (0 until length()).map { f(getJSONObject(it)) }

    private fun <T> List<T>.toJsonArray(f: (T) -> JSONObject): JSONArray =
        JSONArray().also { array -> forEach { array.put(f(it)) } }

    fun encode(s: Snapshot): String = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put("exportedAt", System.currentTimeMillis())
        put("budgets", s.budgets.toJsonArray {
            JSONObject().put("monthKey", it.monthKey).put("income", it.income)
                .put("startDay", it.startDay).put("rollover", it.rollover)
        })
        put("categories", s.categories.toJsonArray {
            JSONObject().put("id", it.id).put("name", it.name).put("iconKey", it.iconKey)
                .put("weeklyLimit", it.weeklyLimit).put("sortOrder", it.sortOrder)
        })
        put("expenses", s.expenses.toJsonArray {
            JSONObject().put("id", it.id).put("monthKey", it.monthKey).put("epochDay", it.epochDay)
                .put("amount", it.amount).put("categoryId", it.categoryId).put("note", it.note)
        })
        put("bills", s.bills.toJsonArray {
            JSONObject().put("id", it.id).put("monthKey", it.monthKey).put("name", it.name)
                .put("amount", it.amount).put("iconKey", it.iconKey).put("dueDay", it.dueDay)
                .put("paid", it.paid)
        })
        put("debts", s.debts.toJsonArray {
            JSONObject().put("id", it.id).put("name", it.name).put("total", it.total)
                .put("monthlyInstallment", it.monthlyInstallment)
        })
        put("debtPayments", s.debtPayments.toJsonArray {
            JSONObject().put("id", it.id).put("debtId", it.debtId).put("epochDay", it.epochDay)
                .put("amount", it.amount)
        })
        put("goals", s.goals.toJsonArray {
            JSONObject().put("id", it.id).put("name", it.name).put("target", it.target)
                .put("monthlyContribution", it.monthlyContribution)
        })
        put("entries", s.entries.toJsonArray {
            JSONObject().put("id", it.id).put("goalId", it.goalId).put("epochDay", it.epochDay)
                .put("amount", it.amount).put("source", it.source)
        })
        put("adjustments", s.adjustments.toJsonArray {
            JSONObject().put("id", it.id).put("monthKey", it.monthKey).put("weekIndex", it.weekIndex)
                .put("kind", it.kind).put("amount", it.amount).put("epochDay", it.epochDay)
                .put("goalId", it.goalId).put("entryId", it.entryId)
        })
        put("emergencyEntries", s.emergencyEntries.toJsonArray {
            JSONObject().put("id", it.id).put("epochDay", it.epochDay).put("amount", it.amount)
                .put("note", it.note).put("source", it.source)
        })
        s.emergencySettings?.let {
            put("emergencySettings", JSONObject().put("target", it.target).put("monthlyContribution", it.monthlyContribution))
        }
    }.toString(2)

    /** يرمي استثناءً إن لم يكن الملف نسخة صالحة من هذا التطبيق. الحقول الجديدة اختيارية ليُقبل الملف القديم. */
    fun decode(text: String): Snapshot {
        val o = JSONObject(text)
        require(o.optString("format") == FORMAT) { "ليس ملف نسخة احتياطية لهذا التطبيق" }
        require(o.optInt("version", 0) <= VERSION) { "إصدار الملف أحدث من التطبيق" }

        return Snapshot(
            budgets = o.getJSONArray("budgets").toList {
                MonthBudget(it.getString("monthKey"), it.getDouble("income"), it.getInt("startDay"), it.getBoolean("rollover"))
            },
            categories = o.getJSONArray("categories").toList {
                Category(it.getLong("id"), it.getString("name"), it.getString("iconKey"), it.getDouble("weeklyLimit"), it.getInt("sortOrder"))
            },
            expenses = o.getJSONArray("expenses").toList {
                Expense(it.getLong("id"), it.getString("monthKey"), it.getLong("epochDay"), it.getDouble("amount"), it.getLong("categoryId"), it.getString("note"))
            },
            bills = o.getJSONArray("bills").toList {
                Bill(it.getLong("id"), it.getString("monthKey"), it.getString("name"), it.getDouble("amount"), it.getString("iconKey"), it.getInt("dueDay"), it.getBoolean("paid"))
            },
            debts = o.getJSONArray("debts").toList {
                Debt(it.getLong("id"), it.getString("name"), it.getDouble("total"), it.getDouble("monthlyInstallment"))
            },
            debtPayments = o.getJSONArray("debtPayments").toList {
                DebtPayment(it.getLong("id"), it.getLong("debtId"), it.getLong("epochDay"), it.getDouble("amount"))
            },
            goals = o.getJSONArray("goals").toList {
                SavingsGoal(it.getLong("id"), it.getString("name"), it.getDouble("target"), it.getDouble("monthlyContribution"))
            },
            entries = o.getJSONArray("entries").toList {
                SavingsEntry(it.getLong("id"), it.getLong("goalId"), it.getLong("epochDay"), it.getDouble("amount"), it.optString("source", ""))
            },
            adjustments = o.optJSONArray("adjustments").toList {
                WeekAdjustment(
                    it.getLong("id"), it.getString("monthKey"), it.getInt("weekIndex"), it.getString("kind"),
                    it.getDouble("amount"), it.getLong("epochDay"), it.getLong("goalId"), it.getLong("entryId")
                )
            },
            emergencyEntries = o.optJSONArray("emergencyEntries").toList {
                EmergencyEntry(it.getLong("id"), it.getLong("epochDay"), it.getDouble("amount"), it.optString("note", ""), it.optString("source", ""))
            },
            emergencySettings = o.optJSONObject("emergencySettings")?.let {
                EmergencySettings(0, it.getDouble("target"), it.getDouble("monthlyContribution"))
            }
        )
    }
}
