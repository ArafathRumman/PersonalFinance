package com.taka.personalfinance.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taka.personalfinance.PFApp
import com.taka.personalfinance.data.BackupManager
import com.taka.personalfinance.data.CategoryEntity
import com.taka.personalfinance.data.LoanEntity
import com.taka.personalfinance.data.RepaymentEntity
import com.taka.personalfinance.data.TransactionEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A short message shown at the bottom of the screen, in both languages. */
data class UiMsg(val en: String, val bn: String)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PFApp
    private val repo = app.repository

    val transactions: StateFlow<List<TransactionEntity>> =
        repo.transactions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories: StateFlow<List<CategoryEntity>> =
        repo.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val loans: StateFlow<List<LoanEntity>> =
        repo.loans.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val repayments: StateFlow<List<RepaymentEntity>> =
        repo.repayments.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** False until the database has delivered its first data. */
    val loaded: StateFlow<Boolean> =
        combine(repo.transactions, repo.categories, repo.loans, repo.repayments) { _, _, _, _ -> true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _events = MutableSharedFlow<UiMsg>(extraBufferCapacity = 16)
    val events: SharedFlow<UiMsg> = _events.asSharedFlow()

    fun toast(en: String, bn: String) {
        _events.tryEmit(UiMsg(en, bn))
    }

    private fun launchSafe(failEn: String = "Something went wrong", failBn: String = "কিছু ভুল হয়েছে", block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast(failEn, failBn)
            }
        }
    }

    // ---------- transactions ----------
    fun saveTransaction(t: TransactionEntity, onDone: () -> Unit = {}) = launchSafe {
        repo.saveTransaction(t)
        onDone()
    }

    fun deleteTransaction(id: Long) = launchSafe {
        repo.deleteTransaction(id)
        toast("Transaction deleted", "লেনদেন মুছে ফেলা হয়েছে")
    }

    // ---------- categories ----------
    fun saveCategory(c: CategoryEntity, onDone: (Long) -> Unit = {}) = launchSafe {
        val id = repo.saveCategory(c)
        onDone(if (id > 0) id else c.id)
    }

    fun deleteCategory(id: Long) = launchSafe {
        repo.deleteCategory(id)
        toast("Category deleted", "ক্যাটাগরি মুছে ফেলা হয়েছে")
    }

    // ---------- loans ----------
    fun saveLoan(l: LoanEntity, onDone: (Long) -> Unit = {}) = launchSafe {
        val id = repo.saveLoan(l)
        onDone(if (id > 0) id else l.id)
    }

    fun deleteLoan(id: Long, onDone: () -> Unit = {}) = launchSafe {
        repo.deleteLoan(id)
        onDone()
        toast("Loan deleted", "ঋণ মুছে ফেলা হয়েছে")
    }

    fun saveRepayment(r: RepaymentEntity, onDone: () -> Unit = {}) = launchSafe {
        if (repo.saveRepayment(r)) {
            onDone()
        } else {
            toast(
                "This payment is more than the amount still due, or was already recorded",
                "এই কিস্তি বাকির চেয়ে বেশি, অথবা আগেই লেখা হয়েছে",
            )
        }
    }

    fun deleteRepayment(id: Long) = launchSafe { repo.deleteRepayment(id) }

    // ---------- data management ----------
    fun wipeAllData() = launchSafe {
        repo.wipeAll()
        toast("All data deleted", "সব ডেটা মুছে ফেলা হয়েছে")
    }

    // ---------- backup / restore ----------
    fun exportBackup(uri: Uri) = launchSafe("Backup failed", "ব্যাকআপ ব্যর্থ হয়েছে") {
        BackupManager.export(app, uri, app.database.dao())
        toast("Backup saved successfully", "ব্যাকআপ সফলভাবে সংরক্ষিত হয়েছে")
    }

    fun exportCsv(uri: Uri) = launchSafe("Export failed", "এক্সপোর্ট ব্যর্থ হয়েছে") {
        BackupManager.exportCsv(app, uri, app.database.dao())
        toast("Spreadsheet file saved", "স্প্রেডশিট ফাইল সংরক্ষিত হয়েছে")
    }

    fun restoreBackup(uri: Uri) = launchSafe(
        "Restore failed: this is not a valid backup file. Your data was not changed.",
        "রিস্টোর ব্যর্থ: এটি সঠিক ব্যাকআপ ফাইল নয়। আপনার ডেটা পরিবর্তন হয়নি।",
    ) {
        val s = BackupManager.restore(app, uri, app.database.dao())
        toast(
            "Restored ${s.transactions} transactions and ${s.loans} loans",
            "${s.transactions}টি লেনদেন ও ${s.loans}টি ঋণ রিস্টোর হয়েছে",
        )
    }
}
