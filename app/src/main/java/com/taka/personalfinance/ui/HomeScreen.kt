@file:OptIn(ExperimentalMaterial3Api::class)

package com.taka.personalfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taka.personalfinance.data.LoanDir
import com.taka.personalfinance.data.TxType
import com.taka.personalfinance.util.Fmt
import java.time.YearMonth

@Composable
fun HomeScreen(
    vm: FinanceViewModel,
    onAdd: (String) -> Unit,
    onEdit: (Long) -> Unit,
    onSeeAll: () -> Unit,
    onOpenLoans: () -> Unit,
) {
    val bn = LocalBn.current
    val fin = MaterialTheme.finance
    val tx by vm.transactions.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val loans by vm.loans.collectAsStateWithLifecycle()
    val reps by vm.repayments.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()

    val ym = remember { YearMonth.now() }
    val summary = remember(tx) { summarize(tx, ym) }
    val allIncome = remember(tx) { totalIncome(tx) }
    val allExpense = remember(tx) { totalExpense(tx) }
    val balance = allIncome - allExpense
    val catMap = remember(cats) { cats.associateBy { it.id } }
    val stats = remember(loans, reps) { loanStats(loans, reps) }
    val toReceive = remember(stats) { totalOutstanding(stats, LoanDir.LENT) }
    val toPay = remember(stats) { totalOutstanding(stats, LoanDir.BORROWED) }
    val recent = remember(tx) { tx.take(6) }
    val white = Color.White

    TabScaffold(title = t("Overview", "সারসংক্ষেপ")) { inner ->
        CenteredContent {
            LazyColumn(
                contentPadding = listPadding(inner, bottomExtra = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(
                        Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(Brush.linearGradient(listOf(fin.heroStart, fin.heroEnd)))
                            .padding(20.dp),
                    ) {
                        Text(t("Total balance", "মোট ব্যালেন্স"), color = white.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(Fmt.money(balance, bn), color = white, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(t("Total income", "মোট আয়"), color = white.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                                Text(Fmt.money(allIncome, bn), color = white, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(t("Total expense", "মোট ব্যয়"), color = white.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                                Text(Fmt.money(allExpense, bn), color = white, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onAdd(TxType.INCOME) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = fin.income, contentColor = Color.White),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.padding(horizontal = 3.dp))
                            Text(t("Income", "আয়"), fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { onAdd(TxType.EXPENSE) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = fin.expense, contentColor = Color.White),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.padding(horizontal = 3.dp))
                            Text(t("Expense", "ব্যয়"), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item {
                    AppCard {
                        Text(
                            t("This month • ", "এই মাস • ") + Fmt.monthYear(ym, bn),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(t("Income", "আয়"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Fmt.money(summary.income, bn), color = fin.income, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(t("Expense", "ব্যয়"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Fmt.money(summary.expense, bn), color = fin.expense, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(t("Savings", "সঞ্চয়"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    Fmt.money(summary.savings, bn),
                                    color = if (summary.savings >= 0) fin.income else fin.expense,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                        }
                        if (summary.income > 0) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                t("You saved ", "আপনি সঞ্চয় করেছেন ") + Fmt.percent(summary.savings.toDouble() / summary.income.toDouble(), bn) + t(" of your income", " আয়ের"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    AppCard(onClick = onOpenLoans) {
                        Text(t("Loans & debts", "ঋণ ও দেনা-পাওনা"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(t("You will receive", "আপনি পাবেন"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Fmt.money(toReceive, bn), color = fin.income, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(t("You will pay", "আপনি দেবেন"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Fmt.money(toPay, bn), color = fin.expense, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
                item {
                    SectionTitle(
                        t("Recent transactions", "সাম্প্রতিক লেনদেন"),
                        trailing = {
                            if (tx.isNotEmpty()) TextButton(onClick = onSeeAll) { Text(t("See all", "সব দেখুন")) }
                        },
                    )
                }
                if (!loaded) {
                    item { Text(t("Loading…", "লোড হচ্ছে…"), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else if (recent.isEmpty()) {
                    item {
                        AppCard {
                            EmptyState(
                                emoji = "🪙",
                                title = t("No transactions yet", "এখনো কোনো লেনদেন নেই"),
                                message = t(
                                    "Use the Income and Expense buttons above to record your first transaction.",
                                    "প্রথম লেনদেন লিখতে উপরের আয় ও ব্যয় বোতাম ব্যবহার করুন।",
                                ),
                            )
                        }
                    }
                } else {
                    item {
                        AppCard {
                            recent.forEach { tr ->
                                TransactionRow(tr, catMap[tr.categoryId ?: -1L], showDate = true, onClick = { onEdit(tr.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}
