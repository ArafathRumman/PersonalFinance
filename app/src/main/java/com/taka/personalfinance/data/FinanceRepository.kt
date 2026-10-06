package com.taka.personalfinance.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val dao: FinanceDao) {
    val transactions: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val loans: Flow<List<LoanEntity>> = dao.observeLoans()
    val repayments: Flow<List<RepaymentEntity>> = dao.observeRepayments()

    suspend fun saveTransaction(t: TransactionEntity): Long = dao.upsertTransaction(t)
    suspend fun deleteTransaction(id: Long) = dao.deleteTransactionById(id)

    suspend fun saveCategory(c: CategoryEntity): Long = dao.upsertCategory(c)
    suspend fun deleteCategory(id: Long) = dao.deleteCategoryAndDetach(id)

    suspend fun saveLoan(l: LoanEntity): Long = dao.upsertLoan(l)
    suspend fun deleteLoan(id: Long) = dao.deleteLoanById(id)

    suspend fun saveRepayment(r: RepaymentEntity): Boolean = dao.saveRepaymentChecked(r)
    suspend fun deleteRepayment(id: Long) = dao.deleteRepaymentById(id)

    /** Deletes every transaction, loan and repayment and restores the default categories. */
    suspend fun wipeAll() {
        dao.replaceAll(DefaultCategories.all(), emptyList(), emptyList(), emptyList())
    }

    suspend fun seedIfNeeded() {
        if (dao.countCategories() == 0) dao.insertCategories(DefaultCategories.all())
    }
}
