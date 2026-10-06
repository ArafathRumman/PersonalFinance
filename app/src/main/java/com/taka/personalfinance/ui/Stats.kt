package com.taka.personalfinance.ui

import com.taka.personalfinance.data.LoanDir
import com.taka.personalfinance.data.LoanEntity
import com.taka.personalfinance.data.RepaymentEntity
import com.taka.personalfinance.data.TransactionEntity
import com.taka.personalfinance.data.TxType
import java.time.LocalDate
import java.time.YearMonth

data class MonthSummary(
    val income: Long,
    val expense: Long,
    val savings: Long,
    /** Running balance (all income minus all expense) up to the end of the period. */
    val balance: Long,
    val count: Int,
)

/** Totals for the days startDay..endDay (inclusive, as epoch days). */
fun summarizeRange(tx: List<TransactionEntity>, startDay: Long, endDay: Long): MonthSummary {
    var income = 0L
    var expense = 0L
    var count = 0
    var balance = 0L
    for (t in tx) {
        val isIncome = t.type == TxType.INCOME
        if (t.dateEpochDay <= endDay) balance += if (isIncome) t.amountMinor else -t.amountMinor
        if (t.dateEpochDay in startDay..endDay) {
            count++
            if (isIncome) income += t.amountMinor else expense += t.amountMinor
        }
    }
    return MonthSummary(income, expense, income - expense, balance, count)
}

fun summarize(tx: List<TransactionEntity>, ym: YearMonth): MonthSummary =
    summarizeRange(tx, ym.atDay(1).toEpochDay(), ym.atEndOfMonth().toEpochDay())

fun summarizeYear(tx: List<TransactionEntity>, year: Int): MonthSummary =
    summarizeRange(tx, LocalDate.of(year, 1, 1).toEpochDay(), LocalDate.of(year, 12, 31).toEpochDay())

fun totalIncome(tx: List<TransactionEntity>): Long = tx.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor }

fun totalExpense(tx: List<TransactionEntity>): Long = tx.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor }

fun totalBalance(tx: List<TransactionEntity>): Long = totalIncome(tx) - totalExpense(tx)

data class CategorySlice(val categoryId: Long?, val amount: Long)

fun categoryTotalsRange(tx: List<TransactionEntity>, startDay: Long, endDay: Long, type: String): List<CategorySlice> =
    tx.filter { it.type == type && it.dateEpochDay in startDay..endDay }
        .groupBy { it.categoryId }
        .map { (k, v) -> CategorySlice(k, v.sumOf { it.amountMinor }) }
        .filter { it.amount > 0 }
        .sortedByDescending { it.amount }

fun categoryTotals(tx: List<TransactionEntity>, ym: YearMonth, type: String): List<CategorySlice> =
    categoryTotalsRange(tx, ym.atDay(1).toEpochDay(), ym.atEndOfMonth().toEpochDay(), type)

data class MonthPoint(val ym: YearMonth, val income: Long, val expense: Long)

/** [months] months ending with [endYm] (oldest first). */
fun monthlyHistory(tx: List<TransactionEntity>, endYm: YearMonth, months: Int = 6): List<MonthPoint> {
    val list = ArrayList<MonthPoint>()
    for (i in months - 1 downTo 0) {
        val ym = endYm.minusMonths(i.toLong())
        val s = summarize(tx, ym)
        list.add(MonthPoint(ym, s.income, s.expense))
    }
    return list
}

/** The 12 months of [year], January first. */
fun yearHistory(tx: List<TransactionEntity>, year: Int): List<MonthPoint> =
    (1..12).map { m ->
        val ym = YearMonth.of(year, m)
        val s = summarize(tx, ym)
        MonthPoint(ym, s.income, s.expense)
    }

/** Total per day of the month (index 0 = day 1). */
fun dailyTotals(tx: List<TransactionEntity>, ym: YearMonth, type: String): LongArray {
    val out = LongArray(ym.lengthOfMonth())
    val start = ym.atDay(1).toEpochDay()
    for (t in tx) {
        if (t.type != type) continue
        val idx = (t.dateEpochDay - start).toInt()
        if (idx in out.indices) out[idx] += t.amountMinor
    }
    return out
}

data class LoanStat(val loan: LoanEntity, val repaid: Long, val outstanding: Long) {
    val settled: Boolean get() = outstanding <= 0L
    val progress: Float
        get() = if (loan.principalMinor <= 0L) 1f else (repaid.toFloat() / loan.principalMinor.toFloat()).coerceIn(0f, 1f)
}

fun loanStats(loans: List<LoanEntity>, repayments: List<RepaymentEntity>): List<LoanStat> {
    val sums = repayments.groupBy { it.loanId }.mapValues { e -> e.value.sumOf { it.amountMinor } }
    return loans.map { l ->
        val repaid = sums[l.id] ?: 0L
        LoanStat(l, repaid, (l.principalMinor - repaid).coerceAtLeast(0L))
    }
}

fun totalOutstanding(stats: List<LoanStat>, direction: String): Long =
    stats.filter { it.loan.direction == direction }.sumOf { it.outstanding }

val LoanStat.owedToMe: Boolean get() = loan.direction == LoanDir.LENT
