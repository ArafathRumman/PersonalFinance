package com.taka.personalfinance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FinanceDao {

    // ---------- transactions ----------
    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, id DESC")
    abstract fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, id DESC")
    abstract suspend fun getAllTransactions(): List<TransactionEntity>

    @Upsert
    abstract suspend fun upsertTransaction(t: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    abstract suspend fun deleteTransactionById(id: Long)

    // ---------- categories ----------
    @Query("SELECT * FROM categories ORDER BY type, isDefault DESC, id")
    abstract fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id")
    abstract suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    abstract suspend fun countCategories(): Int

    @Upsert
    abstract suspend fun upsertCategory(c: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCategories(list: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    abstract suspend fun deleteCategoryRow(id: Long)

    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :id")
    abstract suspend fun detachCategory(id: Long)

    @Transaction
    open suspend fun deleteCategoryAndDetach(id: Long) {
        detachCategory(id)
        deleteCategoryRow(id)
    }

    // ---------- loans ----------
    @Query("SELECT * FROM loans ORDER BY id DESC")
    abstract fun observeLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans ORDER BY id")
    abstract suspend fun getAllLoans(): List<LoanEntity>

    @Upsert
    abstract suspend fun upsertLoan(l: LoanEntity): Long

    @Query("DELETE FROM loans WHERE id = :id")
    abstract suspend fun deleteLoanById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertLoans(list: List<LoanEntity>)

    // ---------- repayments ----------
    @Query("SELECT * FROM repayments ORDER BY dateEpochDay DESC, id DESC")
    abstract fun observeRepayments(): Flow<List<RepaymentEntity>>

    @Query("SELECT * FROM repayments ORDER BY id")
    abstract suspend fun getAllRepayments(): List<RepaymentEntity>

    @Upsert
    abstract suspend fun upsertRepayment(r: RepaymentEntity): Long

    @Query("SELECT principalMinor FROM loans WHERE id = :loanId")
    abstract suspend fun principalOf(loanId: Long): Long?

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM repayments WHERE loanId = :loanId AND id != :excludeId")
    abstract suspend fun repaidExcluding(loanId: Long, excludeId: Long): Long

    /**
     * Saves a repayment only if the total repaid would not exceed the loan amount.
     * This also stops an accidental double tap from recording the same payment twice.
     * Returns false when rejected.
     */
    @Transaction
    open suspend fun saveRepaymentChecked(r: RepaymentEntity): Boolean {
        val principal = principalOf(r.loanId) ?: return false
        val others = repaidExcluding(r.loanId, r.id)
        if (r.amountMinor <= 0L || others + r.amountMinor > principal) return false
        upsertRepayment(r)
        return true
    }

    @Query("DELETE FROM repayments WHERE id = :id")
    abstract suspend fun deleteRepaymentById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRepayments(list: List<RepaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTransactions(list: List<TransactionEntity>)

    // ---------- restore ----------
    @Query("DELETE FROM repayments")
    abstract suspend fun clearRepayments()

    @Query("DELETE FROM loans")
    abstract suspend fun clearLoans()

    @Query("DELETE FROM transactions")
    abstract suspend fun clearTransactions()

    @Query("DELETE FROM categories")
    abstract suspend fun clearCategories()

    /** Replaces everything in one database transaction: either all of it is restored or nothing changes. */
    @Transaction
    open suspend fun replaceAll(
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        loans: List<LoanEntity>,
        repayments: List<RepaymentEntity>,
    ) {
        clearRepayments()
        clearLoans()
        clearTransactions()
        clearCategories()
        insertCategories(categories)
        insertTransactions(transactions)
        insertLoans(loans)
        insertRepayments(repayments)
    }
}
