@file:OptIn(ExperimentalMaterial3Api::class)

package com.taka.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taka.personalfinance.data.LoanDir
import com.taka.personalfinance.data.RepaymentEntity
import com.taka.personalfinance.util.Fmt
import java.time.LocalDate

@Composable
fun LoanDetailScreen(vm: FinanceViewModel, id: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val bn = LocalBn.current
    val fin = MaterialTheme.finance
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val loans by vm.loans.collectAsStateWithLifecycle()
    val reps by vm.repayments.collectAsStateWithLifecycle()

    val loan = remember(loans, id) { loans.firstOrNull { it.id == id } }
    val myReps = remember(reps, id) { reps.filter { it.loanId == id } }
    val repaid = remember(myReps) { myReps.sumOf { it.amountMinor } }

    var dialogFor by remember { mutableStateOf<RepaymentEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleteRep by remember { mutableStateOf<RepaymentEntity?>(null) }

    // If the loan disappears (deleted), leave this screen.
    LaunchedEffect(loaded, loan) {
        if (loaded && loan == null) onBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(loan?.personName ?: "", fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back", "পেছনে")) }
                },
                actions = {
                    if (loan != null) {
                        IconButton(onClick = { onEdit(loan.id) }) { Icon(Icons.Filled.Edit, contentDescription = t("Edit", "সম্পাদনা")) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { inner ->
        if (loan == null) return@Scaffold
        val outstanding = (loan.principalMinor - repaid).coerceAtLeast(0L)
        val settled = outstanding <= 0L
        val lent = loan.direction == LoanDir.LENT
        val accent = if (lent) fin.income else fin.expense
        val today = LocalDate.now().toEpochDay()
        val overdue = !settled && loan.dueEpochDay != null && loan.dueEpochDay < today

        CenteredContent {
            LazyColumn(
                contentPadding = listPadding(inner, bottomExtra = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AppCard {
                        Text(
                            if (lent) t("They owe you", "আপনি পাবেন") else t("You owe them", "আপনি দেবেন"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (settled) t("Settled ✓", "পরিশোধিত ✓") else Fmt.money(outstanding, bn),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (settled) fin.income else accent,
                        )
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { if (loan.principalMinor <= 0L) 1f else (repaid.toFloat() / loan.principalMinor.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        DetailLine(t("Loan amount", "ঋণের পরিমাণ"), Fmt.money(loan.principalMinor, bn))
                        DetailLine(t("Repaid so far", "এ পর্যন্ত পরিশোধ"), Fmt.money(repaid, bn))
                        DetailLine(t("Date given", "তারিখ"), Fmt.date(loan.dateEpochDay, bn))
                        if (loan.dueEpochDay != null) {
                            DetailLine(
                                if (overdue) t("Overdue since", "মেয়াদ শেষ") else t("Due date", "ফেরতের তারিখ"),
                                Fmt.date(loan.dueEpochDay, bn),
                                valueColor = if (overdue) fin.expense else null,
                            )
                        }
                        if (loan.phone.isNotBlank()) DetailLine(t("Phone", "ফোন"), Fmt.num(loan.phone, bn))
                        if (loan.note.isNotBlank()) DetailLine(t("Note", "নোট"), loan.note)
                    }
                }
                if (!settled) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { showAdd = true }, modifier = Modifier.weight(1f).height(50.dp)) {
                                Text(if (lent) t("Add repayment", "কিস্তি পেলাম") else t("Add payment", "কিস্তি দিলাম"))
                            }
                            OutlinedButton(
                                onClick = {
                                    vm.saveRepayment(RepaymentEntity(loanId = loan.id, amountMinor = outstanding, dateEpochDay = today, note = ""))
                                    vm.toast("Marked as fully settled", "পুরো ঋণ পরিশোধিত হিসেবে চিহ্নিত")
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                            ) { Text(t("Settle all", "সব মিটিয়ে দিন")) }
                        }
                    }
                }
                item { SectionTitle(t("Repayments", "কিস্তির হিসাব")) }
                if (myReps.isEmpty()) {
                    item {
                        Text(
                            t("No repayments recorded yet.", "এখনো কোনো কিস্তি লেখা হয়নি।"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                } else {
                    items(myReps, key = { it.id }) { r ->
                        AppCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(Fmt.money(r.amountMinor, bn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = accent)
                                    Text(
                                        Fmt.date(r.dateEpochDay, bn) + (if (r.note.isNotBlank()) " • " + r.note else ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { dialogFor = r }) { Icon(Icons.Filled.Edit, contentDescription = t("Edit", "সম্পাদনা")) }
                                IconButton(onClick = { deleteRep = r }) {
                                    Icon(Icons.Filled.Delete, contentDescription = t("Delete", "মুছুন"), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAdd) {
            RepaymentDialog(
                initial = null,
                maxMinor = outstanding,
                onDismiss = { showAdd = false },
                onSave = { amount, day, note ->
                    showAdd = false
                    vm.saveRepayment(RepaymentEntity(loanId = loan.id, amountMinor = amount, dateEpochDay = day, note = note))
                },
            )
        }
        dialogFor?.let { r ->
            RepaymentDialog(
                initial = r,
                maxMinor = outstanding + r.amountMinor,
                onDismiss = { dialogFor = null },
                onSave = { amount, day, note ->
                    dialogFor = null
                    vm.saveRepayment(r.copy(amountMinor = amount, dateEpochDay = day, note = note))
                },
            )
        }
        deleteRep?.let { r ->
            ConfirmDialog(
                title = t("Delete this repayment?", "এই কিস্তিটি মুছবেন?"),
                message = t("The outstanding amount will go up by ", "বাকির পরিমাণ বাড়বে ") + Fmt.money(r.amountMinor, bn) + ".",
                confirmLabel = t("Delete", "মুছুন"),
                onConfirm = { deleteRep = null; vm.deleteRepayment(r.id) },
                onDismiss = { deleteRep = null },
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RepaymentDialog(
    initial: RepaymentEntity?,
    maxMinor: Long,
    onDismiss: () -> Unit,
    onSave: (amount: Long, day: Long, note: String) -> Unit,
) {
    val bn = LocalBn.current
    var amountText by remember { mutableStateOf(initial?.let { Fmt.editable(it.amountMinor) } ?: "") }
    var day by remember { mutableStateOf(initial?.dateEpochDay ?: LocalDate.now().toEpochDay()) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) t("Add repayment", "কিস্তি যোগ করুন") else t("Edit repayment", "কিস্তি সম্পাদনা")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    t("Up to ", "সর্বোচ্চ ") + Fmt.money(maxMinor, bn),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { v ->
                        val c = v.filter { it.isDigit() || it == '.' || it == ',' }
                        if (c.length <= 15) { amountText = c; error = null }
                    },
                    label = { Text(t("Amount", "পরিমাণ")) },
                    prefix = { Text(Fmt.currency.symbol.trim() + " ") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { msg -> ({ Text(msg) }) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                DatePickerField(label = t("Date", "তারিখ"), epochDay = day, onChange = { if (it != null) day = it })
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 100) note = it },
                    label = { Text(t("Note (optional)", "নোট (ঐচ্ছিক)")) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = Fmt.parseAmount(amountText)
                if (a == null || a <= 0L) {
                    error = if (bn) "০-এর বেশি একটি পরিমাণ লিখুন" else "Enter an amount greater than 0"
                } else if (a > maxMinor) {
                    error = if (bn) "বাকির চেয়ে বেশি হতে পারবে না" else "Cannot be more than the amount due"
                } else {
                    onSave(a, day, note.trim())
                }
            }) { Text(t("Save", "সংরক্ষণ")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel", "বাতিল")) } },
    )
}
