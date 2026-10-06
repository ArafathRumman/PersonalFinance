@file:OptIn(ExperimentalMaterial3Api::class)

package com.taka.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taka.personalfinance.data.LoanDir
import com.taka.personalfinance.data.LoanEntity
import com.taka.personalfinance.util.Fmt
import java.time.LocalDate

/** Add (id <= 0) or edit a loan. [onSaved] gets the loan id; [onDeleted] is called after a delete. */
@Composable
fun LoanEditScreen(vm: FinanceViewModel, id: Long, onClose: () -> Unit, onDeleted: () -> Unit) {
    val bn = LocalBn.current
    val fin = MaterialTheme.finance
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val loans by vm.loans.collectAsStateWithLifecycle()
    val reps by vm.repayments.collectAsStateWithLifecycle()
    val editing = id > 0L
    val existing = remember(loaded, loans, id) { if (editing) loans.firstOrNull { it.id == id } else null }
    val repaid = remember(reps, id) { reps.filter { it.loanId == id }.sumOf { it.amountMinor } }

    var askDelete by remember { mutableStateOf(false) }
    if (askDelete) {
        ConfirmDialog(
            title = t("Delete this loan?", "এই ঋণটি মুছবেন?"),
            message = t("The loan and all its repayments will be deleted. This cannot be undone.", "ঋণ ও এর সব কিস্তি মুছে যাবে। এটি আর ফিরিয়ে আনা যাবে না।"),
            confirmLabel = t("Delete", "মুছুন"),
            onConfirm = { askDelete = false; vm.deleteLoan(id) { onDeleted() } },
            onDismiss = { askDelete = false },
        )
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (editing) t("Edit loan", "ঋণ সম্পাদনা") else t("New loan", "নতুন ঋণ"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back", "পেছনে")) }
                },
                actions = {
                    if (editing && existing != null) {
                        IconButton(onClick = { askDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = t("Delete", "মুছুন"), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { inner ->
        if (!loaded) {
            Column(Modifier.fillMaxSize().padding(inner), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (editing && existing == null) {
            Column(Modifier.fillMaxSize().padding(inner), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                EmptyState("🤷", t("Loan not found", "ঋণ পাওয়া যায়নি"), t("It may have been deleted.", "এটি হয়তো মুছে ফেলা হয়েছে।"), actionLabel = t("Go back", "পেছনে যান"), onAction = onClose)
            }
            return@Scaffold
        }

        val key = existing?.id ?: 0L
        var direction by rememberSaveable(key) { mutableStateOf(existing?.direction ?: LoanDir.LENT) }
        var person by rememberSaveable(key) { mutableStateOf(existing?.personName ?: "") }
        var phone by rememberSaveable(key) { mutableStateOf(existing?.phone ?: "") }
        var amountText by rememberSaveable(key) { mutableStateOf(existing?.let { Fmt.editable(it.principalMinor) } ?: "") }
        var day by rememberSaveable(key) { mutableStateOf(existing?.dateEpochDay ?: LocalDate.now().toEpochDay()) }
        var hasDue by rememberSaveable(key) { mutableStateOf(existing?.dueEpochDay != null) }
        var dueDay by rememberSaveable(key) { mutableStateOf(existing?.dueEpochDay ?: (LocalDate.now().toEpochDay() + 30)) }
        var note by rememberSaveable(key) { mutableStateOf(existing?.note ?: "") }
        var nameError by remember { mutableStateOf(false) }
        var amountError by remember { mutableStateOf<String?>(null) }
        var saving by remember { mutableStateOf(false) }
        val accent = if (direction == LoanDir.LENT) fin.income else fin.expense

        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = direction == LoanDir.LENT, onClick = { direction = LoanDir.LENT }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                    Text(t("I gave money", "আমি টাকা দিয়েছি"))
                }
                SegmentedButton(selected = direction == LoanDir.BORROWED, onClick = { direction = LoanDir.BORROWED }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                    Text(t("I took money", "আমি টাকা নিয়েছি"))
                }
            }
            OutlinedTextField(
                value = person,
                onValueChange = { if (it.length <= 60) { person = it; nameError = false } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (direction == LoanDir.LENT) t("Who borrowed from you?", "কে আপনার কাছ থেকে নিয়েছে?") else t("Who did you borrow from?", "আপনি কার কাছ থেকে নিয়েছেন?")) },
                singleLine = true,
                isError = nameError,
                supportingText = if (nameError) ({ Text(t("Enter a name", "একটি নাম লিখুন")) }) else null,
                shape = MaterialTheme.shapes.medium,
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { v -> val c = v.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' }; if (c.length <= 20) phone = c },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(t("Phone (optional)", "ফোন (ঐচ্ছিক)")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = MaterialTheme.shapes.medium,
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { v ->
                    val cleaned = v.filter { it.isDigit() || it == '.' || it == ',' }
                    if (cleaned.length <= 15) { amountText = cleaned; amountError = null }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(t("Loan amount", "ঋণের পরিমাণ")) },
                prefix = { Text(Fmt.currency.symbol.trim() + " ", fontSize = 20.sp, color = accent, fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = accent),
                singleLine = true,
                isError = amountError != null,
                supportingText = amountError?.let { msg -> ({ Text(msg) }) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.medium,
            )
            DatePickerField(label = t("Date given", "তারিখ"), epochDay = day, onChange = { if (it != null) day = it })
            DatePickerField(
                label = t("Due date (optional)", "ফেরতের তারিখ (ঐচ্ছিক)"),
                epochDay = if (hasDue) dueDay else null,
                onChange = { if (it == null) hasDue = false else { hasDue = true; dueDay = it } },
                clearable = true,
            )
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 200) note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(t("Note (optional)", "নোট (ঐচ্ছিক)")) },
                minLines = 2,
                maxLines = 4,
                shape = MaterialTheme.shapes.medium,
            )
            Button(
                onClick = {
                    val amount = Fmt.parseAmount(amountText)
                    if (person.isBlank()) {
                        nameError = true
                    } else if (amount == null || amount <= 0L) {
                        amountError = if (bn) "০-এর বেশি একটি পরিমাণ লিখুন" else "Enter an amount greater than 0"
                    } else if (editing && amount < repaid) {
                        amountError = (if (bn) "ইতিমধ্যে পরিশোধ হয়েছে " else "Already repaid ") + Fmt.money(repaid, bn) + (if (bn) " — এর চেয়ে কম হতে পারবে না" else " — amount cannot be lower")
                    } else if (!saving) {
                        saving = true
                        vm.saveLoan(
                            LoanEntity(
                                id = existing?.id ?: 0L,
                                personName = person.trim(),
                                phone = phone.trim(),
                                direction = direction,
                                principalMinor = amount,
                                dateEpochDay = day,
                                dueEpochDay = if (hasDue) dueDay else null,
                                note = note.trim(),
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            ),
                        ) { onClose() }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.large,
            ) { Text(if (editing) t("Save changes", "পরিবর্তন সংরক্ষণ") else t("Save loan", "ঋণ সংরক্ষণ করুন"), fontSize = 17.sp) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
