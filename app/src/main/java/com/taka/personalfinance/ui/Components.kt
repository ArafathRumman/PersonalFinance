@file:OptIn(ExperimentalMaterial3Api::class)

package com.taka.personalfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taka.personalfinance.data.CategoryEntity
import com.taka.personalfinance.data.PayMethod
import com.taka.personalfinance.data.TransactionEntity
import com.taka.personalfinance.data.TxType
import com.taka.personalfinance.util.Fmt
import com.taka.personalfinance.util.categoryLabel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colors) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colors) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (trailing != null) trailing()
    }
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun EmojiAvatar(emoji: String, tint: Color, size: Dp = 44.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size.value * 0.45f).sp)
    }
}

@Composable
fun TransactionRow(
    tx: TransactionEntity,
    category: CategoryEntity?,
    showDate: Boolean,
    onClick: () -> Unit,
) {
    val bn = LocalBn.current
    val fin = MaterialTheme.finance
    val isIncome = tx.type == TxType.INCOME
    val color = if (isIncome) fin.income else fin.expense
    Row(
        Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiAvatar(category?.icon ?: "❔", color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                categoryLabel(category, bn),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = buildString {
                if (showDate) append(Fmt.date(tx.dateEpochDay, bn))
                if (isNotEmpty()) append(" • ")
                append(PayMethod.label(tx.paymentMethod, bn))
                if (tx.note.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(tx.note.replace('\n', ' '))
                }
            }
            if (sub.isNotEmpty()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            (if (isIncome) "+" else "−") + Fmt.money(tx.amountMinor, bn),
            color = color,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel", "বাতিল")) } },
    )
}

/** A read-only field that opens a calendar dialog when tapped. */
@Composable
fun DatePickerField(
    label: String,
    epochDay: Long?,
    onChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    clearable: Boolean = false,
) {
    val bn = LocalBn.current
    var show by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            OutlinedTextField(
                value = if (epochDay != null) Fmt.date(epochDay, bn) else "",
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                placeholder = { Text(t("Not set", "দেওয়া হয়নি")) },
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.matchParentSize().clickable { show = true })
        }
        if (clearable && epochDay != null) {
            TextButton(onClick = { onChange(null) }) { Text(t("Clear", "মুছুন")) }
        }
    }
    if (show) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = Fmt.epochDayToUtcMillis(epochDay ?: LocalDate.now().toEpochDay()),
        )
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    val ms = state.selectedDateMillis
                    if (ms != null) onChange(Fmt.utcMillisToEpochDay(ms))
                    show = false
                }) { Text(t("OK", "ঠিক আছে")) }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text(t("Cancel", "বাতিল")) } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun MonthSwitcher(
    ym: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val bn = LocalBn.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = t("Previous month", "আগের মাস"))
        }
        Text(
            Fmt.monthYear(ym, bn),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.then(if (onToday != null) Modifier.clip(MaterialTheme.shapes.small).clickable { onToday() }.padding(horizontal = 12.dp, vertical = 6.dp) else Modifier),
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = t("Next month", "পরের মাস"))
        }
    }
}

/** Standard tab page: collapsing large title (One UI style) + content. */
@Composable
fun TabScaffold(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                actions = actions,
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = floatingActionButton,
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

/** Keeps content readable on tablets / unfolded foldables. */
@Composable
fun CenteredContent(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 840.dp).fillMaxWidth().fillMaxSize(), content = content)
    }
}

@Composable
fun listPadding(inner: PaddingValues, bottomExtra: Dp = 96.dp): PaddingValues {
    val dir = LocalLayoutDirection.current
    return PaddingValues(
        start = inner.calculateStartPadding(dir) + 16.dp,
        end = inner.calculateEndPadding(dir) + 16.dp,
        top = inner.calculateTopPadding() + 8.dp,
        bottom = inner.calculateBottomPadding() + bottomExtra,
    )
}
