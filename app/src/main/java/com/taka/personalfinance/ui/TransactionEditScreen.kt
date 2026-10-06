@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.taka.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.taka.personalfinance.data.CategoryEntity
import com.taka.personalfinance.data.PayMethod
import com.taka.personalfinance.data.TransactionEntity
import com.taka.personalfinance.data.TxType
import com.taka.personalfinance.util.Fmt
import com.taka.personalfinance.util.categoryLabel
import java.time.LocalDate

@Composable
fun TransactionEditScreen(vm: FinanceViewModel, id: Long, presetDay: Long, presetType: String, onClose: () -> Unit) {
    val bn = LocalBn.current
    val fin = MaterialTheme.finance
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val editing = id > 0L
    val existing = remember(loaded, txs, id) { if (editing) txs.firstOrNull { it.id == id } else null }

    var askDelete by remember { mutableStateOf(false) }
    if (askDelete) {
        ConfirmDialog(
            title = t("Delete this transaction?", "এই লেনদেনটি মুছবেন?"),
            message = t("This cannot be undone.", "এটি আর ফিরিয়ে আনা যাবে না।"),
            confirmLabel = t("Delete", "মুছুন"),
            onConfirm = {
                askDelete = false
                vm.deleteTransaction(id)
                onClose()
            },
            onDismiss = { askDelete = false },
        )
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editing) t("Edit transaction", "লেনদেন সম্পাদনা") else t("New transaction", "নতুন লেনদেন"),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back", "পেছনে"))
                    }
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
                EmptyState(
                    emoji = "🤷",
                    title = t("Transaction not found", "লেনদেন পাওয়া যায়নি"),
                    message = t("It may have been deleted.", "এটি হয়তো মুছে ফেলা হয়েছে।"),
                    actionLabel = t("Go back", "পেছনে যান"),
                    onAction = onClose,
                )
            }
            return@Scaffold
        }

        val key = existing?.id ?: 0L
        var type by rememberSaveable(key) {
            mutableStateOf(existing?.type ?: (if (presetType == TxType.INCOME) TxType.INCOME else TxType.EXPENSE))
        }
        var amountText by rememberSaveable(key) { mutableStateOf(existing?.let { Fmt.editable(it.amountMinor) } ?: "") }
        var categoryId by rememberSaveable(key) { mutableStateOf(existing?.categoryId ?: -1L) }
        var day by rememberSaveable(key) {
            mutableStateOf(existing?.dateEpochDay ?: (if (presetDay >= 0L) presetDay else LocalDate.now().toEpochDay()))
        }
        var note by rememberSaveable(key) { mutableStateOf(existing?.note ?: "") }
        var method by rememberSaveable(key) { mutableStateOf(existing?.paymentMethod ?: PayMethod.CASH) }
        var amountError by remember { mutableStateOf(false) }
        var categoryError by remember { mutableStateOf(false) }
        var saving by remember { mutableStateOf(false) }
        var showNewCategory by remember { mutableStateOf(false) }

        val typeCats = remember(cats, type) { cats.filter { it.type == type } }
        val accent = if (type == TxType.INCOME) fin.income else fin.expense

        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == TxType.EXPENSE,
                    onClick = { if (type != TxType.EXPENSE) { type = TxType.EXPENSE; categoryId = -1L } },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text(t("Expense", "ব্যয়")) }
                SegmentedButton(
                    selected = type == TxType.INCOME,
                    onClick = { if (type != TxType.INCOME) { type = TxType.INCOME; categoryId = -1L } },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text(t("Income", "আয়")) }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { v ->
                    val cleaned = v.filter { it.isDigit() || it == '.' || it == ',' }
                    if (cleaned.length <= 15) {
                        amountText = cleaned
                        amountError = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(t("Amount", "পরিমাণ")) },
                prefix = { Text(Fmt.currency.symbol.trim() + " ", fontSize = 22.sp, color = accent, fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = accent),
                singleLine = true,
                isError = amountError,
                supportingText = if (amountError) ({ Text(t("Enter an amount greater than 0 (up to 2 decimals)", "০-এর বেশি একটি পরিমাণ লিখুন (সর্বোচ্চ ২ দশমিক)")) }) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.medium,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("Category", "ক্যাটাগরি"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    typeCats.forEach { c ->
                        FilterChip(
                            selected = categoryId == c.id,
                            onClick = { categoryId = c.id; categoryError = false },
                            label = { Text(c.icon + " " + categoryLabel(c, bn)) },
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = { showNewCategory = true },
                        label = { Text(t("+ New", "+ নতুন")) },
                    )
                }
                if (categoryError) {
                    Text(t("Please choose a category", "একটি ক্যাটাগরি বেছে নিন"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("Payment method", "পেমেন্ট মাধ্যম"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PayMethod.all.forEach { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(PayMethod.label(m, bn)) })
                    }
                }
            }

            DatePickerField(label = t("Date", "তারিখ"), epochDay = day, onChange = { if (it != null) day = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = day == LocalDate.now().toEpochDay(), onClick = { day = LocalDate.now().toEpochDay() }, label = { Text(t("Today", "আজ")) })
                FilterChip(selected = day == LocalDate.now().toEpochDay() - 1, onClick = { day = LocalDate.now().toEpochDay() - 1 }, label = { Text(t("Yesterday", "গতকাল")) })
            }

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 200) note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(t("Note (optional)", "নোট (ঐচ্ছিক)")) },
                minLines = 2,
                maxLines = 4,
                supportingText = { Text(Fmt.num(note.length, bn) + "/" + Fmt.num(200, bn)) },
                shape = MaterialTheme.shapes.medium,
            )

            Button(
                onClick = {
                    val amount = Fmt.parseAmount(amountText)
                    if (amount == null || amount <= 0L) {
                        amountError = true
                    } else if (categoryId < 0L) {
                        categoryError = true
                    } else if (!saving) {
                        saving = true
                        vm.saveTransaction(
                            TransactionEntity(
                                id = existing?.id ?: 0L,
                                type = type,
                                amountMinor = amount,
                                categoryId = categoryId,
                                dateEpochDay = day,
                                note = note.trim(),
                                paymentMethod = method,
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            ),
                        ) { onClose() }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(if (editing) t("Save changes", "পরিবর্তন সংরক্ষণ") else t("Save", "সংরক্ষণ করুন"), fontSize = 17.sp)
            }
            Spacer(Modifier.height(24.dp))
        }

        if (showNewCategory) {
            CategoryDialog(
                initial = null,
                fixedType = type,
                onDismiss = { showNewCategory = false },
                onSave = { c ->
                    showNewCategory = false
                    vm.saveCategory(c) { newId -> categoryId = newId; categoryError = false }
                },
            )
        }
    }
}

private val EmojiChoices = listOf(
    "🍽️", "🛒", "🚌", "🏠", "💡", "📱", "💊", "🎓", "🛍️", "🎬", "👪", "🎁", "✈️", "📦", "💼", "🏪",
    "💻", "📈", "💰", "☕", "⛽", "🐾", "🏋️", "📚", "🔧", "🧾", "🎮", "💈", "🚕", "🍔", "👕", "🕌",
)

/** Create or edit a category. [fixedType] is used when creating from the transaction form. */
@Composable
fun CategoryDialog(
    initial: CategoryEntity?,
    fixedType: String?,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit,
) {
    val bnMode = LocalBn.current
    var name by remember { mutableStateOf(initial?.let { if (bnMode && it.nameBn.isNotBlank()) it.nameBn else it.name } ?: "") }
    var icon by remember { mutableStateOf(initial?.icon ?: "📦") }
    var type by remember { mutableStateOf(initial?.type ?: fixedType ?: TxType.EXPENSE) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) t("New category", "নতুন ক্যাটাগরি") else t("Edit category", "ক্যাটাগরি সম্পাদনা")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 30) { name = it; error = false } },
                    label = { Text(t("Name", "নাম")) },
                    singleLine = true,
                    isError = error,
                    supportingText = if (error) ({ Text(t("Enter a name", "একটি নাম লিখুন")) }) else null,
                )
                if (fixedType == null && initial == null) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = type == TxType.EXPENSE, onClick = { type = TxType.EXPENSE }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(t("Expense", "ব্যয়")) }
                        SegmentedButton(selected = type == TxType.INCOME, onClick = { type = TxType.INCOME }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(t("Income", "আয়")) }
                    }
                }
                Text(t("Icon", "আইকন"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmojiChoices.forEach { e ->
                        FilterChip(selected = icon == e, onClick = { icon = e }, label = { Text(e, fontSize = 18.sp) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val n = name.trim()
                if (n.isEmpty()) {
                    error = true
                } else {
                    val newName: String
                    val newBn: String
                    if (initial == null || initial.name == initial.nameBn) {
                        newName = n
                        newBn = n
                    } else if (bnMode) {
                        newName = initial.name
                        newBn = n
                    } else {
                        newName = n
                        newBn = initial.nameBn
                    }
                    onSave(
                        CategoryEntity(
                            id = initial?.id ?: 0L,
                            name = newName,
                            nameBn = newBn,
                            type = initial?.type ?: type,
                            icon = icon,
                            isDefault = initial?.isDefault ?: false,
                        ),
                    )
                }
            }) { Text(t("Save", "সংরক্ষণ")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel", "বাতিল")) } },
    )
}
