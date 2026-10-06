package com.taka.personalfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object TxType {
    const val INCOME = "INCOME"
    const val EXPENSE = "EXPENSE"
}

object PayMethod {
    const val CASH = "CASH"
    const val BKASH = "BKASH"
    const val NAGAD = "NAGAD"
    const val ROCKET = "ROCKET"
    const val BANK = "BANK"
    const val CARD = "CARD"
    const val OTHER = "OTHER"

    val all = listOf(CASH, BKASH, NAGAD, ROCKET, BANK, CARD, OTHER)

    fun isValid(v: String): Boolean = v in all

    fun label(v: String, bn: Boolean): String = when (v) {
        CASH -> if (bn) "ক্যাশ" else "Cash"
        BKASH -> if (bn) "বিকাশ" else "bKash"
        NAGAD -> if (bn) "নগদ" else "Nagad"
        ROCKET -> if (bn) "রকেট" else "Rocket"
        BANK -> if (bn) "ব্যাংক" else "Bank"
        CARD -> if (bn) "কার্ড" else "Card"
        else -> if (bn) "অন্যান্য" else "Other"
    }
}

object LoanDir {
    /** I gave money to someone: they owe me. */
    const val LENT = "LENT"

    /** I took money from someone: I owe them. */
    const val BORROWED = "BORROWED"
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nameBn: String,
    val type: String,
    val icon: String,
    val isDefault: Boolean = false,
)

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["dateEpochDay"]), Index(value = ["categoryId"])],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    /** Amount in paisa (1 taka = 100 paisa) so that sums are always exact. */
    val amountMinor: Long,
    val categoryId: Long?,
    val dateEpochDay: Long,
    val note: String = "",
    val paymentMethod: String = PayMethod.CASH,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val phone: String = "",
    val direction: String,
    val principalMinor: Long,
    val dateEpochDay: Long,
    val dueEpochDay: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "repayments",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["loanId"])],
)
data class RepaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val amountMinor: Long,
    val dateEpochDay: Long,
    val note: String = "",
)
