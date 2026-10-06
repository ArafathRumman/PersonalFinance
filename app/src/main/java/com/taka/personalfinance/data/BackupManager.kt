package com.taka.personalfinance.data

import android.content.Context
import android.net.Uri
import com.taka.personalfinance.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Counts of what was restored, for the confirmation message. */
data class RestoreSummary(val transactions: Int, val categories: Int, val loans: Int, val repayments: Int)

object BackupManager {
    private const val APP_ID = "PersonalFinance"
    private const val VERSION = 1
    private const val MAX_BYTES = 100 * 1024 * 1024

    suspend fun export(context: Context, uri: Uri, dao: FinanceDao) = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", APP_ID)
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val cats = JSONArray()
        for (c in dao.getAllCategories()) {
            cats.put(
                JSONObject().put("id", c.id).put("name", c.name).put("nameBn", c.nameBn)
                    .put("type", c.type).put("icon", c.icon).put("isDefault", c.isDefault),
            )
        }
        val txs = JSONArray()
        for (t in dao.getAllTransactions()) {
            txs.put(
                JSONObject().put("id", t.id).put("type", t.type).put("amountMinor", t.amountMinor)
                    .put("categoryId", t.categoryId ?: JSONObject.NULL)
                    .put("dateEpochDay", t.dateEpochDay).put("note", t.note).put("paymentMethod", t.paymentMethod)
                    .put("createdAt", t.createdAt),
            )
        }
        val loans = JSONArray()
        for (l in dao.getAllLoans()) {
            loans.put(
                JSONObject().put("id", l.id).put("personName", l.personName).put("phone", l.phone)
                    .put("direction", l.direction).put("principalMinor", l.principalMinor)
                    .put("dateEpochDay", l.dateEpochDay).put("dueEpochDay", l.dueEpochDay ?: JSONObject.NULL)
                    .put("note", l.note).put("createdAt", l.createdAt),
            )
        }
        val reps = JSONArray()
        for (r in dao.getAllRepayments()) {
            reps.put(
                JSONObject().put("id", r.id).put("loanId", r.loanId).put("amountMinor", r.amountMinor)
                    .put("dateEpochDay", r.dateEpochDay).put("note", r.note),
            )
        }
        root.put("categories", cats)
        root.put("transactions", txs)
        root.put("loans", loans)
        root.put("repayments", reps)

        val out = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("Cannot open the chosen file")
        out.use { it.write(root.toString().toByteArray(Charsets.UTF_8)) }
    }

    /** Reads and fully validates the file first; the database is only touched if everything is valid. */
    suspend fun restore(context: Context, uri: Uri, dao: FinanceDao): RestoreSummary = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open the chosen file")
        val bytes = input.use { it.readBytes() }
        if (bytes.size > MAX_BYTES) throw IllegalArgumentException("File too large")
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        require(root.optString("app") == APP_ID) { "Not a Personal Finance backup" }
        val version = root.optInt("version", 0)
        require(version in 1..VERSION) { "Unsupported backup version" }

        val categories = ArrayList<CategoryEntity>()
        val catArr = root.getJSONArray("categories")
        for (i in 0 until catArr.length()) {
            val o = catArr.getJSONObject(i)
            val type = o.getString("type")
            require(type == TxType.INCOME || type == TxType.EXPENSE)
            categories.add(
                CategoryEntity(
                    id = o.getLong("id"), name = o.getString("name"), nameBn = o.optString("nameBn", ""),
                    type = type, icon = o.optString("icon", "📦"), isDefault = o.optBoolean("isDefault", false),
                ),
            )
        }
        val finalCategories = if (categories.isEmpty()) DefaultCategories.all() else categories
        val catIds = finalCategories.map { it.id }.toHashSet()

        val transactions = ArrayList<TransactionEntity>()
        val txArr = root.getJSONArray("transactions")
        for (i in 0 until txArr.length()) {
            val o = txArr.getJSONObject(i)
            val type = o.getString("type")
            require(type == TxType.INCOME || type == TxType.EXPENSE)
            val amount = o.getLong("amountMinor")
            require(amount >= 0)
            val cat: Long? = if (o.isNull("categoryId")) null else o.getLong("categoryId")
            transactions.add(
                TransactionEntity(
                    id = o.getLong("id"), type = type, amountMinor = amount,
                    categoryId = if (cat != null && cat in catIds) cat else null,
                    dateEpochDay = o.getLong("dateEpochDay"), note = o.optString("note", ""),
                    paymentMethod = o.optString("paymentMethod", PayMethod.CASH).let { if (PayMethod.isValid(it)) it else PayMethod.CASH },
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                ),
            )
        }

        val loans = ArrayList<LoanEntity>()
        val loanArr = root.getJSONArray("loans")
        for (i in 0 until loanArr.length()) {
            val o = loanArr.getJSONObject(i)
            val dir = o.getString("direction")
            require(dir == LoanDir.LENT || dir == LoanDir.BORROWED)
            val principal = o.getLong("principalMinor")
            require(principal >= 0)
            loans.add(
                LoanEntity(
                    id = o.getLong("id"), personName = o.getString("personName"), phone = o.optString("phone", ""),
                    direction = dir, principalMinor = principal, dateEpochDay = o.getLong("dateEpochDay"),
                    dueEpochDay = if (o.isNull("dueEpochDay")) null else o.getLong("dueEpochDay"),
                    note = o.optString("note", ""), createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                ),
            )
        }
        val loanIds = loans.map { it.id }.toHashSet()

        val repayments = ArrayList<RepaymentEntity>()
        val repArr = root.getJSONArray("repayments")
        for (i in 0 until repArr.length()) {
            val o = repArr.getJSONObject(i)
            val loanId = o.getLong("loanId")
            if (loanId !in loanIds) continue
            val amount = o.getLong("amountMinor")
            require(amount >= 0)
            repayments.add(
                RepaymentEntity(
                    id = o.getLong("id"), loanId = loanId, amountMinor = amount,
                    dateEpochDay = o.getLong("dateEpochDay"), note = o.optString("note", ""),
                ),
            )
        }

        dao.replaceAll(finalCategories, transactions, loans, repayments)
        RestoreSummary(transactions.size, finalCategories.size, loans.size, repayments.size)
    }

    /** Spreadsheet-friendly export of all transactions (opens in Excel / Google Sheets). */
    suspend fun exportCsv(context: Context, uri: Uri, dao: FinanceDao) = withContext(Dispatchers.IO) {
        val cats = dao.getAllCategories().associateBy { it.id }
        val sb = StringBuilder()
        sb.append('﻿')
        sb.append("Date,Type,Category,Payment method,Amount,Note\r\n")
        for (t in dao.getAllTransactions()) {
            val date = LocalDate.ofEpochDay(t.dateEpochDay).toString()
            val type = if (t.type == TxType.INCOME) "Income" else "Expense"
            val cat = t.categoryId?.let { cats[it]?.name } ?: "Uncategorized"
            sb.append(date).append(',').append(type).append(',').append(csv(cat)).append(',')
                .append(PayMethod.label(t.paymentMethod, false)).append(',')
                .append(Fmt.editable(t.amountMinor)).append(',').append(csv(t.note)).append("\r\n")
        }
        val out = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("Cannot open the chosen file")
        out.use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
    }

    private fun csv(raw: String): String {
        var s = raw.replace("\r", " ").replace("\n", " ")
        // Stop spreadsheet programs from treating text as a formula.
        if (s.isNotEmpty() && (s[0] == '=' || s[0] == '+' || s[0] == '-' || s[0] == '@')) s = "'$s"
        return "\"" + s.replace("\"", "\"\"") + "\""
    }
}
