@file:OptIn(ExperimentalMaterial3Api::class)

package com.taka.personalfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taka.personalfinance.data.TxType
import com.taka.personalfinance.util.Fmt
import com.taka.personalfinance.util.categoryLabel
import java.time.LocalDate
import java.time.YearMonth

@Composable
private fun StatTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ReportsScreen(vm: FinanceViewModel) {
    val bn = LocalBn.current
    val fin = MaterialTheme.finance
    val tx by vm.transactions.collectAsStateWithLifecycle()
    val cats by vm.categories.collectAsStateWithLifecycle()
    val catMap = remember(cats) { cats.associateBy { it.id } }

    val nowYm = remember { YearMonth.now() }
    var mode by rememberSaveable { mutableStateOf(0) } // 0 = month, 1 = year
    var monthIndex by rememberSaveable { mutableStateOf(nowYm.year * 12 + nowYm.monthValue - 1) }
    var year by rememberSaveable { mutableStateOf(nowYm.year) }
    var catType by rememberSaveable { mutableStateOf(TxType.EXPENSE) }

    val ym = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)
    val startDay = if (mode == 0) ym.atDay(1).toEpochDay() else LocalDate.of(year, 1, 1).toEpochDay()
    val endDay = if (mode == 0) ym.atEndOfMonth().toEpochDay() else LocalDate.of(year, 12, 31).toEpochDay()

    var selBar by remember(mode, monthIndex, year) { mutableStateOf(-1) }
    var selSlice by remember(mode, monthIndex, year, catType) { mutableStateOf(0) }
    var selDay by remember(mode, monthIndex, year, catType) { mutableStateOf(-1) }

    val summary = remember(tx, mode, ym, year) { if (mode == 0) summarize(tx, ym) else summarizeYear(tx, year) }
    val bars = remember(tx, mode, ym, year) { if (mode == 0) monthlyHistory(tx, ym, 6) else yearHistory(tx, year) }
    val slices = remember(tx, mode, ym, year, catType) { categoryTotalsRange(tx, startDay, endDay, catType) }
    val daily = remember(tx, mode, ym, catType) { if (mode == 0) dailyTotals(tx, ym, catType) else LongArray(0) }
    val inRange = remember(tx, startDay, endDay) { tx.filter { it.dateEpochDay in startDay..endDay } }
    val biggestExpense = remember(inRange) { inRange.filter { it.type == TxType.EXPENSE }.maxByOrNull { it.amountMinor } }
    val biggestIncome = remember(inRange) { inRange.filter { it.type == TxType.INCOME }.maxByOrNull { it.amountMinor } }

    val barIdx = when {
        selBar in bars.indices -> selBar
        mode == 0 -> bars.lastIndex
        year == nowYm.year -> nowYm.monthValue - 1
        else -> 11
    }
    val sliceIdx = if (slices.isEmpty()) 0 else selSlice.coerceIn(0, slices.size - 1)
    val sliceTotal = slices.sumOf { it.amount }
    val today = LocalDate.now()
    val daysElapsed = when {
        mode == 0 && ym == nowYm -> today.dayOfMonth
        mode == 0 -> ym.lengthOfMonth()
        year == today.year -> today.dayOfYear
        else -> LocalDate.of(year, 12, 31).dayOfYear
    }
    val accent = if (catType == TxType.INCOME) fin.income else fin.expense

    TabScaffold(title = t("Reports", "রিপোর্ট")) { inner ->
        CenteredContent {
            LazyColumn(
                contentPadding = listPadding(inner, bottomExtra = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = mode == 0, onClick = { mode = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(t("Monthly", "মাসিক")) }
                        SegmentedButton(selected = mode == 1, onClick = { mode = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(t("Yearly", "বার্ষিক")) }
                    }
                }
                item {
                    if (mode == 0) {
                        MonthSwitcher(
                            ym,
                            onPrev = { monthIndex -= 1 },
                            onNext = { monthIndex += 1 },
                            onToday = { monthIndex = nowYm.year * 12 + nowYm.monthValue - 1 },
                        )
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            IconButton(onClick = { year -= 1 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = t("Previous year", "আগের বছর")) }
                            Text(
                                Fmt.num(year, bn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { year = nowYm.year }.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                            IconButton(onClick = { year += 1 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = t("Next year", "পরের বছর")) }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile(t("Income", "আয়"), Fmt.money(summary.income, bn), fin.income, Modifier.weight(1f))
                            StatTile(t("Expense", "ব্যয়"), Fmt.money(summary.expense, bn), fin.expense, Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile(
                                t("Savings", "সঞ্চয়"),
                                Fmt.money(summary.savings, bn),
                                if (summary.savings >= 0) fin.income else fin.expense,
                                Modifier.weight(1f),
                            )
                            StatTile(
                                if (mode == 0) t("Balance at month end", "মাস শেষে ব্যালেন্স") else t("Balance at year end", "বছর শেষে ব্যালেন্স"),
                                Fmt.money(summary.balance, bn),
                                MaterialTheme.colorScheme.primary,
                                Modifier.weight(1f),
                            )
                        }
                        if (summary.income > 0) {
                            Text(
                                t("Savings rate: ", "সঞ্চয়ের হার: ") + Fmt.percent(summary.savings.toDouble() / summary.income.toDouble(), bn),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
                item {
                    AppCard {
                        Text(
                            if (mode == 0) t("Income vs expense • last 6 months", "আয় বনাম ব্যয় • গত ৬ মাস") else t("Income vs expense by month", "মাস অনুযায়ী আয় বনাম ব্যয়"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        val p = bars[barIdx]
                        Text(Fmt.monthYear(p.ym, bn), style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(Fmt.money(p.income, bn), color = fin.income, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(Fmt.money(p.expense, bn), color = fin.expense, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        GroupedBarChart(
                            groups = bars.map { listOf(it.income, it.expense) },
                            colors = listOf(fin.income, fin.expense),
                            selected = barIdx,
                            onSelect = { selBar = it },
                        )
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            bars.forEach { b ->
                                Text(
                                    Fmt.monthShort(b.ym, bn),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                item {
                    AppCard {
                        Text(t("By category", "ক্যাটাগরি অনুযায়ী"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            SegmentedButton(selected = catType == TxType.EXPENSE, onClick = { catType = TxType.EXPENSE }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(t("Expense", "ব্যয়")) }
                            SegmentedButton(selected = catType == TxType.INCOME, onClick = { catType = TxType.INCOME }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(t("Income", "আয়")) }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (slices.isEmpty()) {
                            EmptyState(
                                emoji = "📊",
                                title = t("No data for this period", "এই সময়ে কোনো তথ্য নেই"),
                                message = t("Add transactions to see your category breakdown.", "ক্যাটাগরি অনুযায়ী হিসাব দেখতে লেনদেন যোগ করুন।"),
                            )
                        } else {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                DonutChart(
                                    values = slices.map { it.amount },
                                    colors = ChartPalette,
                                    selected = sliceIdx,
                                    onSelect = { selSlice = it },
                                ) {
                                    val s = slices[sliceIdx]
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 36.dp)) {
                                        Text(
                                            Fmt.percent(s.amount.toDouble() / sliceTotal.toDouble(), bn),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            categoryLabel(catMap[s.categoryId ?: -1L], bn),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            slices.forEachIndexed { i, s ->
                                val c = catMap[s.categoryId ?: -1L]
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .background(if (i == sliceIdx) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                        .clickable { selSlice = i }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.size(12.dp).clip(CircleShape).background(ChartPalette[i % ChartPalette.size]))
                                    Spacer(Modifier.width(10.dp))
                                    Text((c?.icon ?: "❔") + " " + categoryLabel(c, bn), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(Fmt.money(s.amount, bn), fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        Fmt.percent(s.amount.toDouble() / sliceTotal.toDouble(), bn),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(44.dp),
                                        textAlign = TextAlign.End,
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(t("Total", "মোট"), fontWeight = FontWeight.SemiBold)
                                Text(Fmt.money(sliceTotal, bn), fontWeight = FontWeight.Bold, color = accent)
                            }
                        }
                    }
                }
                if (mode == 0) {
                    item {
                        AppCard {
                            Text(
                                if (catType == TxType.INCOME) t("Daily income", "দৈনিক আয়") else t("Daily spending", "দৈনিক ব্যয়"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(6.dp))
                            val maxDay = daily.indices.maxByOrNull { daily[it] } ?: 0
                            val dayIdx = if (selDay in daily.indices) selDay else maxDay
                            if (daily.isNotEmpty() && daily.any { it > 0L }) {
                                Text(
                                    Fmt.date(ym.atDay(dayIdx + 1).toEpochDay(), bn) + " • " + Fmt.money(daily[dayIdx], bn),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accent,
                                )
                                Spacer(Modifier.height(6.dp))
                                GroupedBarChart(
                                    groups = daily.map { listOf(it) },
                                    colors = listOf(accent),
                                    selected = dayIdx,
                                    onSelect = { selDay = it },
                                    height = 120.dp,
                                )
                                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(Fmt.num(1, bn), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Fmt.num(daily.size, bn), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Text(t("Nothing recorded this month.", "এই মাসে কিছু লেখা হয়নি।"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    AppCard {
                        Text(t("Highlights", "মূল তথ্য"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        InsightLine(t("Transactions", "লেনদেন"), Fmt.num(summary.count, bn))
                        InsightLine(
                            t("Average daily expense", "গড় দৈনিক ব্যয়"),
                            Fmt.money(if (daysElapsed > 0) summary.expense / daysElapsed else 0L, bn),
                        )
                        if (biggestExpense != null) {
                            InsightLine(
                                t("Biggest expense", "সবচেয়ে বড় ব্যয়"),
                                Fmt.money(biggestExpense.amountMinor, bn) + " • " + categoryLabel(catMap[biggestExpense.categoryId ?: -1L], bn),
                            )
                        }
                        if (biggestIncome != null) {
                            InsightLine(
                                t("Biggest income", "সবচেয়ে বড় আয়"),
                                Fmt.money(biggestIncome.amountMinor, bn) + " • " + categoryLabel(catMap[biggestIncome.categoryId ?: -1L], bn),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
    }
}
