@file:OptIn(ExperimentalMaterial3Api::class)

package com.taka.personalfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taka.personalfinance.data.CategoryEntity
import com.taka.personalfinance.data.TxType
import com.taka.personalfinance.util.Fmt
import com.taka.personalfinance.util.categoryLabel

@Composable
fun CategoriesScreen(vm: FinanceViewModel, onClose: () -> Unit) {
    val bn = LocalBn.current
    val cats by vm.categories.collectAsStateWithLifecycle()
    val txs by vm.transactions.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(TxType.EXPENSE) }
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CategoryEntity?>(null) }

    val usage = remember(txs) { txs.groupingBy { it.categoryId }.eachCount() }
    val list = remember(cats, tab) { cats.filter { it.type == tab } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(t("Categories", "ক্যাটাগরি"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back", "পেছনে")) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, contentDescription = t("Add category", "ক্যাটাগরি যোগ")) }
        },
    ) { inner ->
        CenteredContent {
            Column(Modifier.padding(top = inner.calculateTopPadding())) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    SegmentedButton(selected = tab == TxType.EXPENSE, onClick = { tab = TxType.EXPENSE }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(t("Expense", "ব্যয়")) }
                    SegmentedButton(selected = tab == TxType.INCOME, onClick = { tab = TxType.INCOME }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(t("Income", "আয়")) }
                }
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = inner.calculateBottomPadding() + 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (list.isEmpty()) {
                        item { EmptyState("🗂️", t("No categories", "কোনো ক্যাটাগরি নেই"), t("Tap + to add one.", "যোগ করতে + চাপুন।")) }
                    }
                    items(list, key = { it.id }) { c ->
                        val count = usage[c.id] ?: 0
                        AppCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                EmojiAvatar(c.icon, MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(categoryLabel(c, bn), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Text(
                                        Fmt.num(count, bn) + t(" transactions", "টি লেনদেন"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { editing = c }) { Icon(Icons.Filled.Edit, contentDescription = t("Edit", "সম্পাদনা")) }
                                IconButton(onClick = { deleting = c }) {
                                    Icon(Icons.Filled.Delete, contentDescription = t("Delete", "মুছুন"), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        CategoryDialog(
            initial = null,
            fixedType = tab,
            onDismiss = { creating = false },
            onSave = { c -> creating = false; vm.saveCategory(c) },
        )
    }
    editing?.let { c ->
        CategoryDialog(
            initial = c,
            fixedType = null,
            onDismiss = { editing = null },
            onSave = { updated -> editing = null; vm.saveCategory(updated) },
        )
    }
    deleting?.let { c ->
        ConfirmDialog(
            title = t("Delete category?", "ক্যাটাগরি মুছবেন?"),
            message = t(
                "Transactions in this category will be kept and shown as “Uncategorized”.",
                "এই ক্যাটাগরির লেনদেনগুলো থাকবে, তবে “শ্রেণিহীন” দেখাবে।",
            ),
            confirmLabel = t("Delete", "মুছুন"),
            onConfirm = { deleting = null; vm.deleteCategory(c.id) },
            onDismiss = { deleting = null },
        )
    }
}
