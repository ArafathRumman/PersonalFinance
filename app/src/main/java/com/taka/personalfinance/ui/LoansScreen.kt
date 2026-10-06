@file:OptIn(ExperimentalMaterial3Api::class)

package com.taka.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taka.personalfinance.data.LoanDir
import com.taka.personalfinance.util.Fmt
import java.time.LocalDate

@Composable
fun LoansScreen(vm: FinanceViewModel, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val bn = LocalBn.current
    val fin = MaterialTheme.finance
    val loans by vm.loans.collectAsStateWithLifecycle()
    val reps by vm.repayments.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val stats = remember(loans, reps) { loanStats(loans, reps) }
    val toReceive = remember(stats) { totalOutstanding(stats, LoanDir.LENT) }
    val toPay = remember(stats) { totalOutstanding(stats, LoanDir.BORROWED) }

    var direction by rememberSaveable { mutableStateOf(LoanDir.LENT) }
    var showSettled by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now().toEpochDay()

    val shown = remember(stats, direction, showSettled) {
        stats.filter { it.loan.direction == direction && it.settled == showSettled }
    }

    TabScaffold(
        title = t("Loans", "ঋণ"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(t("Add loan", "ঋণ যোগ করুন")) },
            )
        },
    ) { inner ->
        CenteredContent {
            LazyColumn(
                contentPadding = listPadding(inner),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppCard(Modifier.weight(1f)) {
                            Text(t("You will receive", "আপনি পাবেন"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(Fmt.money(toReceive, bn), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = fin.income)
                        }
                        AppCard(Modifier.weight(1f)) {
                            Text(t("You will pay", "আপনি দেবেন"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(Fmt.money(toPay, bn), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = fin.expense)
                        }
                    }
                }
                item {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = direction == LoanDir.LENT, onClick = { direction = LoanDir.LENT }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                            Text(t("They owe me", "আমার পাওনা"))
                        }
                        SegmentedButton(selected = direction == LoanDir.BORROWED, onClick = { direction = LoanDir.BORROWED }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                            Text(t("I owe them", "আমার দেনা"))
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !showSettled, onClick = { showSettled = false }, label = { Text(t("Active", "চলমান")) })
                        FilterChip(selected = showSettled, onClick = { showSettled = true }, label = { Text(t("Settled", "পরিশোধিত")) })
                    }
                }
                if (!loaded) {
                    item { Text(t("Loading…", "লোড হচ্ছে…"), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else if (shown.isEmpty()) {
                    item {
                        EmptyState(
                            emoji = if (showSettled) "✅" else "🤝",
                            title = if (showSettled) t("No settled loans", "কোনো পরিশোধিত ঋণ নেই") else t("No active loans", "কোনো চলমান ঋণ নেই"),
                            message = t(
                                "Track who borrowed from you, or who you borrowed from, with repayments and dues.",
                                "কে আপনার কাছ থেকে নিয়েছে বা আপনি কার কাছ থেকে নিয়েছেন, কিস্তি ও বাকিসহ হিসাব রাখুন।",
                            ),
                            actionLabel = if (!showSettled) t("Add loan", "ঋণ যোগ করুন") else null,
                            onAction = if (!showSettled) onAdd else null,
                        )
                    }
                } else {
                    items(shown, key = { it.loan.id }) { s ->
                        val l = s.loan
                        val overdue = !s.settled && l.dueEpochDay != null && l.dueEpochDay < today
                        AppCard(onClick = { onOpen(l.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                EmojiAvatar(l.personName.trim().take(1).uppercase().ifEmpty { "?" }, if (l.direction == LoanDir.LENT) fin.income else fin.expense)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(l.personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(
                                        t("Given ", "তারিখ ") + Fmt.date(l.dateEpochDay, bn),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (l.dueEpochDay != null && !s.settled) {
                                        Text(
                                            (if (overdue) t("Overdue • due ", "মেয়াদ উত্তীর্ণ • ") else t("Due ", "মেয়াদ ")) + Fmt.date(l.dueEpochDay, bn),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (overdue) fin.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        Fmt.money(s.outstanding, bn),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (s.settled) MaterialTheme.colorScheme.onSurfaceVariant else if (l.direction == LoanDir.LENT) fin.income else fin.expense,
                                    )
                                    Text(
                                        t("of ", "মোট ") + Fmt.money(l.principalMinor, bn),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
