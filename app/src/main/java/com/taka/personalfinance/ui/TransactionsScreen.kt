@file:OptIn(ExperimentalMaterial3Api::class)

package com.taka.personalfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taka.personalfinance.data.CategoryEntity
import com.taka.personalfinance.data.PayMethod
import com.taka.personalfinance.data.TransactionEntity
import com.taka.personalfinance.data.TxType
import com.taka.personalfinance.util.Fmt
import com.taka.personalfinance.util.categoryLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private const val PERIOD_ALL = "all"
private const val PERIOD_THIS_MONTH = "this_month"
private const val PERIOD_LAST_MONTH = "last_month"
private const val PERIOD_THIS_YEAR = "this_year"
private const val PERIOD_CUSTOM = "custom"

private const val SORT_DATE_DESC = "date_desc"
private const val SORT_DATE_ASC = "date_asc"
private const val SORT_AMOUNT_DESC = "amount_desc"
private const val SORT_AMOUNT_ASC = "amount_asc"

@Composable
fun TransactionsScreen(vm: FinanceViewModel, onAdd: (Long) -> Unit, onEdit: (Long) -> Unit) {
    var mode by rememberSaveable { mutableStateOf(0) } // 0 = list, 1 = calendar
    var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }

    TabScaffold(
        title = t("Transactions", "লেনদেন"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAdd(if (mode == 1) selectedDay else -1L) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(t("Add", "যোগ করুন")) },
            )
        },
    ) { inner ->
        CenteredContent {
            Column(Modifier.padding(top = inner.calculateTopPadding())) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    SegmentedButton(
                        selected = mode == 0,
                        onClick = { mode = 0 },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text(t("List", "তালিকা")) }
                    SegmentedButton(
                        selected = mode == 1,
                        onClick = { mode = 1 },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text(t("Calendar", "ক্যালেন্ডার")) }
                }
                if (mode == 0) {
                    TransactionList(vm, inner, onEdit)
                } else {
                    CalendarView(vm, inner, selectedDay, { selectedDay = it }, onAdd, onEdit)
                }
            }
        }
    }
}

@Composable
private fun TransactionList(vm: FinanceViewModel, inner: androidx.compose.foundation.layout.PaddingValues, onEdit: (Long) -> Unit) {
    val bn = LocalBn.current
    val fin = MaterialTheme.finance
    val tx by vm.transactions.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val catMap = remember(cats) { cats.associateBy { it.id } }

    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("") } // "", INCOME, EXPENSE
    var categoryFilter by rememberSaveable { mutableStateOf(-1L) }
    var period by rememberSaveable { mutableStateOf(PERIOD_ALL) }
    var methodFilter by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SORT_DATE_DESC) }
    var methodMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var customStart by rememberSaveable { mutableStateOf(0L) }
    var customEnd by rememberSaveable { mutableStateOf(0L) }
    var showRange by remember { mutableStateOf(false) }
    var periodMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val range: LongRange? = when (period) {
        PERIOD_THIS_MONTH -> YearMonth.from(today).let { it.atDay(1).toEpochDay()..it.atEndOfMonth().toEpochDay() }
        PERIOD_LAST_MONTH -> YearMonth.from(today).minusMonths(1).let { it.atDay(1).toEpochDay()..it.atEndOfMonth().toEpochDay() }
        PERIOD_THIS_YEAR -> LocalDate.of(today.year, 1, 1).toEpochDay()..LocalDate.of(today.year, 12, 31).toEpochDay()
        PERIOD_CUSTOM -> customStart..customEnd
        else -> null
    }

    val filtered = remember(tx, query, typeFilter, categoryFilter, methodFilter, sort, period, customStart, customEnd, catMap, bn) {
        val q = query.trim().lowercase()
        val matches = tx.filter { t ->
            (typeFilter.isEmpty() || t.type == typeFilter) &&
                (categoryFilter < 0L || t.categoryId == categoryFilter) &&
                (methodFilter.isEmpty() || t.paymentMethod == methodFilter) &&
                (range == null || t.dateEpochDay in range) &&
                (
                    q.isEmpty() ||
                        t.note.lowercase().contains(q) ||
                        categoryLabel(catMap[t.categoryId ?: -1L], false).lowercase().contains(q) ||
                        categoryLabel(catMap[t.categoryId ?: -1L], true).lowercase().contains(q) ||
                        Fmt.editable(t.amountMinor).contains(q)
                    )
        }
        when (sort) {
            SORT_DATE_ASC -> matches.sortedWith(compareBy<TransactionEntity> { it.dateEpochDay }.thenBy { it.id })
            SORT_AMOUNT_DESC -> matches.sortedByDescending { it.amountMinor }
            SORT_AMOUNT_ASC -> matches.sortedBy { it.amountMinor }
            else -> matches // already newest first
        }
    }
    val income = remember(filtered) { filtered.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor } }
    val expense = remember(filtered) { filtered.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor } }
    val groupByDay = sort == SORT_DATE_DESC || sort == SORT_DATE_ASC
    val grouped = remember(filtered) { filtered.groupBy { it.dateEpochDay }.toList() }
    val filtersActive = query.isNotBlank() || typeFilter.isNotEmpty() || categoryFilter >= 0L || methodFilter.isNotEmpty() || period != PERIOD_ALL

    fun clearFilters() {
        query = ""; typeFilter = ""; categoryFilter = -1L; methodFilter = ""; period = PERIOD_ALL
    }

    val sortLabel = when (sort) {
        SORT_DATE_ASC -> t("Oldest first", "পুরনো আগে")
        SORT_AMOUNT_DESC -> t("Highest amount", "বেশি টাকা আগে")
        SORT_AMOUNT_ASC -> t("Lowest amount", "কম টাকা আগে")
        else -> t("Newest first", "নতুন আগে")
    }
    val methodChipLabel = if (methodFilter.isNotEmpty()) PayMethod.label(methodFilter, bn) else t("Payment", "পেমেন্ট")

    val periodLabel = when (period) {
        PERIOD_THIS_MONTH -> t("This month", "এই মাস")
        PERIOD_LAST_MONTH -> t("Last month", "গত মাস")
        PERIOD_THIS_YEAR -> t("This year", "এই বছর")
        PERIOD_CUSTOM -> Fmt.date(customStart, bn) + " – " + Fmt.date(customEnd, bn)
        else -> t("All time", "সব সময়")
    }
    val categoryChipLabel = if (categoryFilter >= 0L) categoryLabel(catMap[categoryFilter], bn) else t("Category", "ক্যাটাগরি")

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = inner.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { if (it.length <= 60) query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(t("Search notes, category, amount", "নোট, ক্যাটাগরি, পরিমাণ খুঁজুন")) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = t("Clear", "মুছুন")) }
                    }
                },
                shape = MaterialTheme.shapes.large,
            )
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = typeFilter.isEmpty(), onClick = { typeFilter = "" }, label = { Text(t("All", "সব")) })
                FilterChip(selected = typeFilter == TxType.INCOME, onClick = { typeFilter = TxType.INCOME }, label = { Text(t("Income", "আয়")) })
                FilterChip(selected = typeFilter == TxType.EXPENSE, onClick = { typeFilter = TxType.EXPENSE }, label = { Text(t("Expense", "ব্যয়")) })
                Box {
                    FilterChip(selected = categoryFilter >= 0L, onClick = { categoryMenu = true }, label = { Text(categoryChipLabel) })
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        DropdownMenuItem(text = { Text(t("All categories", "সব ক্যাটাগরি")) }, onClick = { categoryFilter = -1L; categoryMenu = false })
                        cats.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.icon + "  " + categoryLabel(c, bn)) },
                                onClick = { categoryFilter = c.id; categoryMenu = false },
                            )
                        }
                    }
                }
                Box {
                    FilterChip(selected = period != PERIOD_ALL, onClick = { periodMenu = true }, label = { Text(periodLabel) })
                    DropdownMenu(expanded = periodMenu, onDismissRequest = { periodMenu = false }) {
                        DropdownMenuItem(text = { Text(t("All time", "সব সময়")) }, onClick = { period = PERIOD_ALL; periodMenu = false })
                        DropdownMenuItem(text = { Text(t("This month", "এই মাস")) }, onClick = { period = PERIOD_THIS_MONTH; periodMenu = false })
                        DropdownMenuItem(text = { Text(t("Last month", "গত মাস")) }, onClick = { period = PERIOD_LAST_MONTH; periodMenu = false })
                        DropdownMenuItem(text = { Text(t("This year", "এই বছর")) }, onClick = { period = PERIOD_THIS_YEAR; periodMenu = false })
                        DropdownMenuItem(text = { Text(t("Custom range…", "নিজে বেছে নিন…")) }, onClick = { periodMenu = false; showRange = true })
                    }
                }
                Box {
                    FilterChip(selected = methodFilter.isNotEmpty(), onClick = { methodMenu = true }, label = { Text(methodChipLabel) })
                    DropdownMenu(expanded = methodMenu, onDismissRequest = { methodMenu = false }) {
                        DropdownMenuItem(text = { Text(t("All methods", "সব মাধ্যম")) }, onClick = { methodFilter = ""; methodMenu = false })
                        PayMethod.all.forEach { m ->
                            DropdownMenuItem(text = { Text(PayMethod.label(m, bn)) }, onClick = { methodFilter = m; methodMenu = false })
                        }
                    }
                }
                Box {
                    FilterChip(selected = sort != SORT_DATE_DESC, onClick = { sortMenu = true }, label = { Text(t("Sort: ", "সাজান: ") + sortLabel) })
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        DropdownMenuItem(text = { Text(t("Newest first", "নতুন আগে")) }, onClick = { sort = SORT_DATE_DESC; sortMenu = false })
                        DropdownMenuItem(text = { Text(t("Oldest first", "পুরনো আগে")) }, onClick = { sort = SORT_DATE_ASC; sortMenu = false })
                        DropdownMenuItem(text = { Text(t("Highest amount", "বেশি টাকা আগে")) }, onClick = { sort = SORT_AMOUNT_DESC; sortMenu = false })
                        DropdownMenuItem(text = { Text(t("Lowest amount", "কম টাকা আগে")) }, onClick = { sort = SORT_AMOUNT_ASC; sortMenu = false })
                    }
                }
            }
        }
        if (filtered.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(Fmt.num(filtered.size, bn) + t(" items", "টি"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+" + Fmt.money(income, bn), style = MaterialTheme.typography.labelMedium, color = fin.income, fontWeight = FontWeight.SemiBold)
                    Text("−" + Fmt.money(expense, bn), style = MaterialTheme.typography.labelMedium, color = fin.expense, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (!loaded) {
            item { Text(t("Loading…", "লোড হচ্ছে…"), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else if (tx.isEmpty()) {
            item {
                EmptyState(
                    emoji = "🧾",
                    title = t("No transactions yet", "এখনো কোনো লেনদেন নেই"),
                    message = t("Tap “Add” to record your first income or expense.", "প্রথম আয় বা ব্যয় লিখতে “যোগ করুন” চাপুন।"),
                )
            }
        } else if (filtered.isEmpty()) {
            item {
                EmptyState(
                    emoji = "🔎",
                    title = t("Nothing found", "কিছু পাওয়া যায়নি"),
                    message = t("No transactions match your search or filters.", "আপনার খোঁজ বা ফিল্টারের সাথে কোনো লেনদেন মেলেনি।"),
                    actionLabel = if (filtersActive) t("Clear filters", "ফিল্টার মুছুন") else null,
                    onAction = if (filtersActive) ({ clearFilters() }) else null,
                )
            }
        } else if (!groupByDay) {
            item(key = "flat") {
                AppCard {
                    filtered.forEach { tr -> TransactionRow(tr, catMap[tr.categoryId ?: -1L], showDate = true, onClick = { onEdit(tr.id) }) }
                }
            }
        } else {
            val ordered = if (sort == SORT_DATE_ASC) grouped.sortedBy { it.first } else grouped
            ordered.forEach { (day, list) ->
                item(key = "d$day") {
                    val net = list.sumOf { if (it.type == TxType.INCOME) it.amountMinor else -it.amountMinor }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Fmt.dayHeader(day, bn), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Fmt.money(net, bn), style = MaterialTheme.typography.labelLarge, color = if (net >= 0) fin.income else fin.expense)
                    }
                }
                item(key = "g$day") {
                    AppCard {
                        list.forEach { tr -> TransactionRow(tr, catMap[tr.categoryId ?: -1L], showDate = false, onClick = { onEdit(tr.id) }) }
                    }
                }
            }
        }
    }

    if (showRange) {
        val state = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showRange = false },
            confirmButton = {
                TextButton(onClick = {
                    val s = state.selectedStartDateMillis
                    val e = state.selectedEndDateMillis ?: s
                    if (s != null && e != null) {
                        customStart = Fmt.utcMillisToEpochDay(s)
                        customEnd = Fmt.utcMillisToEpochDay(e)
                        period = PERIOD_CUSTOM
                    }
                    showRange = false
                }) { Text(t("Apply", "প্রয়োগ")) }
            },
            dismissButton = { TextButton(onClick = { showRange = false }) { Text(t("Cancel", "বাতিল")) } },
        ) {
            DateRangePicker(state = state, modifier = Modifier.height(480.dp))
        }
    }
}

@Composable
private fun CalendarView(
    vm: FinanceViewModel,
    inner: androidx.compose.foundation.layout.PaddingValues,
    selectedDay: Long,
    onSelectDay: (Long) -> Unit,
    onAdd: (Long) -> Unit,
    onEdit: (Long) -> Unit,
) {
    val bn = LocalBn.current
    val fin = MaterialTheme.finance
    val tx by vm.transactions.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val catMap = remember(cats) { cats.associateBy { it.id } }

    var monthIndex by rememberSaveable { mutableStateOf(YearMonth.from(LocalDate.ofEpochDay(selectedDay)).let { it.year * 12 + (it.monthValue - 1) }) }
    val ym = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)
    val today = LocalDate.now().toEpochDay()

    val byDay = remember(tx) { tx.groupBy { it.dateEpochDay } }
    val dayList = byDay[selectedDay] ?: emptyList()
    val first = ym.atDay(1)
    val leading = first.dayOfWeek.value % 7 // Sunday = 0
    val days = ym.lengthOfMonth()
    val rows = (leading + days + 6) / 7
    val weekdays = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = inner.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AppCard {
                MonthSwitcher(
                    ym,
                    onPrev = { monthIndex -= 1 },
                    onNext = { monthIndex += 1 },
                    onToday = {
                        val n = YearMonth.now()
                        monthIndex = n.year * 12 + (n.monthValue - 1)
                        onSelectDay(today)
                    },
                )
                Row(Modifier.fillMaxWidth()) {
                    weekdays.forEach { d ->
                        Text(
                            Fmt.weekdayShort(d, bn),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val dayNum = r * 7 + c - leading + 1
                            if (dayNum < 1 || dayNum > days) {
                                Box(Modifier.weight(1f).height(54.dp))
                            } else {
                                val epoch = ym.atDay(dayNum).toEpochDay()
                                val list = byDay[epoch]
                                val inc = list?.filter { it.type == TxType.INCOME }?.sumOf { it.amountMinor } ?: 0L
                                val exp = list?.filter { it.type == TxType.EXPENSE }?.sumOf { it.amountMinor } ?: 0L
                                val isSel = epoch == selectedDay
                                val isToday = epoch == today
                                Column(
                                    Modifier.weight(1f).height(54.dp).padding(1.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                                        .clickable { onSelectDay(epoch) }
                                        .padding(vertical = 3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        Fmt.num(dayNum, bn),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (inc > 0L) Text(Fmt.compact(inc, bn), fontSize = 9.sp, color = fin.income, maxLines = 1)
                                    if (exp > 0L) Text(Fmt.compact(exp, bn), fontSize = 9.sp, color = fin.expense, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            val net = dayList.sumOf { if (it.type == TxType.INCOME) it.amountMinor else -it.amountMinor }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(Fmt.dayHeader(selectedDay, bn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (dayList.isNotEmpty()) {
                        Text(
                            t("Net ", "নিট ") + Fmt.money(net, bn),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (net >= 0) fin.income else fin.expense,
                        )
                    }
                }
                TextButton(onClick = { onAdd(selectedDay) }) { Text(t("+ Add here", "+ এখানে যোগ করুন")) }
            }
        }
        if (dayList.isEmpty()) {
            item {
                Text(
                    t("No transactions on this day.", "এই দিনে কোনো লেনদেন নেই।"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp),
                )
            }
        } else {
            item {
                AppCard {
                    dayList.forEach { tr -> TransactionRow(tr, catMap[tr.categoryId ?: -1L], showDate = false, onClick = { onEdit(tr.id) }) }
                }
            }
        }
    }
}
