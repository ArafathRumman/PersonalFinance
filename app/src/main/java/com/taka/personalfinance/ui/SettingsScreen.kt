@file:OptIn(ExperimentalMaterial3Api::class)

package com.taka.personalfinance.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taka.personalfinance.PFApp
import com.taka.personalfinance.util.BiometricHelper
import com.taka.personalfinance.util.Currencies
import com.taka.personalfinance.util.Fmt
import com.taka.personalfinance.util.PinResult
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class PinMode { NONE, ENABLE, CHANGE, DISABLE }

@Composable
fun SettingsScreen(vm: FinanceViewModel, app: PFApp, activity: FragmentActivity, onOpenCategories: () -> Unit) {
    val bn = LocalBn.current
    val context = LocalContext.current
    val lang by app.settings.language.collectAsStateWithLifecycle()
    val theme by app.settings.theme.collectAsStateWithLifecycle()
    val currencyCode by app.settings.currency.collectAsStateWithLifecycle()
    val lockEnabled by app.appLock.enabled.collectAsStateWithLifecycle()
    val bioEnabled by app.appLock.biometricEnabled.collectAsStateWithLifecycle()
    val bioAvailable = remember { BiometricHelper.isAvailable(context) }
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    var pinMode by remember { mutableStateOf(PinMode.NONE) }
    var askRestore by remember { mutableStateOf(false) }
    var askWipe by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var currencyMenu by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) vm.exportBackup(uri)
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) vm.exportCsv(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.restoreBackup(uri)
    }

    val bioTitle = t("Turn on biometric unlock", "বায়োমেট্রিক আনলক চালু করুন")
    val bioSubtitle = t("Confirm your fingerprint or face", "আপনার ফিঙ্গারপ্রিন্ট বা ফেস নিশ্চিত করুন")
    val bioCancel = t("Cancel", "বাতিল")
    val currentCurrency = Currencies.byCode(currencyCode)

    TabScaffold(title = t("Settings", "সেটিংস")) { inner ->
        CenteredContent {
            LazyColumn(
                contentPadding = listPadding(inner, bottomExtra = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AppCard {
                        Text(t("Appearance", "চেহারা"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Text(t("Language", "ভাষা"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            SegmentedButton(selected = lang == "en", onClick = { app.settings.setLanguage("en") }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("English") }
                            SegmentedButton(selected = lang == "bn", onClick = { app.settings.setLanguage("bn") }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("বাংলা") }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(t("Theme", "থিম"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            SegmentedButton(selected = theme == "system", onClick = { app.settings.setTheme("system") }, shape = SegmentedButtonDefaults.itemShape(0, 3)) { Text(t("Auto", "অটো"), maxLines = 1) }
                            SegmentedButton(selected = theme == "light", onClick = { app.settings.setTheme("light") }, shape = SegmentedButtonDefaults.itemShape(1, 3)) { Text(t("Light", "লাইট"), maxLines = 1) }
                            SegmentedButton(selected = theme == "dark", onClick = { app.settings.setTheme("dark") }, shape = SegmentedButtonDefaults.itemShape(2, 3)) { Text(t("Dark", "ডার্ক"), maxLines = 1) }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(t("Currency", "মুদ্রা"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Row(
                                Modifier.fillMaxWidth().clickable { currencyMenu = true }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    currentCurrency.symbol.trim() + "  " + (if (bn) currentCurrency.nameBn else currentCurrency.nameEn) + " (" + currentCurrency.code + ")",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("▾")
                            }
                            DropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                                Currencies.all.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.symbol.trim() + "  " + (if (bn) c.nameBn else c.nameEn) + " (" + c.code + ")") },
                                        onClick = { app.settings.setCurrency(c.code); currencyMenu = false },
                                    )
                                }
                            }
                        }
                        Text(
                            t("Only the symbol changes; amounts are not converted.", "শুধু চিহ্ন বদলায়; পরিমাণ রূপান্তর হয় না।"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    AppCard {
                        Text(t("Security", "নিরাপত্তা"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        SwitchRow(
                            title = t("App lock (PIN)", "অ্যাপ লক (পিন)"),
                            subtitle = t("Ask for a PIN every time the app is opened", "অ্যাপ খুললেই পিন চাইবে"),
                            checked = lockEnabled,
                            onChange = { on -> pinMode = if (on) PinMode.ENABLE else PinMode.DISABLE },
                        )
                        if (lockEnabled) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ActionRow(t("Change PIN", "পিন পরিবর্তন"), null) { pinMode = PinMode.CHANGE }
                            if (bioAvailable) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                SwitchRow(
                                    title = t("Fingerprint / face unlock", "ফিঙ্গারপ্রিন্ট / ফেস আনলক"),
                                    subtitle = t("Unlock faster; your PIN always still works", "দ্রুত আনলক; পিন সবসময় কাজ করবে"),
                                    checked = bioEnabled,
                                    onChange = { on ->
                                        if (on) {
                                            BiometricHelper.prompt(activity, bioTitle, bioSubtitle, bioCancel, onSuccess = { app.appLock.setBiometric(true) })
                                        } else {
                                            app.appLock.setBiometric(false)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    AppCard {
                        Text(t("Data & backup", "ডেটা ও ব্যাকআপ"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        ActionRow(
                            t("Create backup", "ব্যাকআপ তৈরি করুন"),
                            t("Save all your data to a file you choose", "আপনার পছন্দের ফাইলে সব ডেটা সংরক্ষণ করুন"),
                        ) {
                            app.appLock.suppressLockFor(120_000)
                            backupLauncher.launch("PersonalFinance-backup-" + LocalDate.now() + ".json")
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ActionRow(
                            t("Restore backup", "ব্যাকআপ রিস্টোর"),
                            t("Replace current data with a backup file", "বর্তমান ডেটার জায়গায় ব্যাকআপ ফাইল বসান"),
                        ) { askRestore = true }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ActionRow(
                            t("Export to spreadsheet (CSV)", "স্প্রেডশিটে এক্সপোর্ট (CSV)"),
                            t("Open in Excel or Google Sheets", "এক্সেল বা গুগল শিটে খুলুন"),
                        ) {
                            app.appLock.suppressLockFor(120_000)
                            csvLauncher.launch("PersonalFinance-transactions-" + LocalDate.now() + ".csv")
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ActionRow(t("Manage categories", "ক্যাটাগরি পরিচালনা"), null, onClick = onOpenCategories)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ActionRow(
                            t("Delete all data", "সব ডেটা মুছুন"),
                            t("Permanently erase transactions and loans", "লেনদেন ও ঋণ চিরতরে মুছে ফেলুন"),
                            destructive = true,
                        ) { askWipe = true }
                    }
                }
                item {
                    AppCard {
                        Text(t("About", "সম্পর্কে"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t("App", "অ্যাপ"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(t("Personal Finance", "পার্সোনাল ফাইন্যান্স"), fontWeight = FontWeight.Medium)
                        }
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t("Version", "সংস্করণ"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Fmt.num(version, bn), fontWeight = FontWeight.Medium)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ActionRow(t("Privacy information", "গোপনীয়তা সংক্রান্ত তথ্য"), t("Your data never leaves this phone", "আপনার ডেটা এই ফোনের বাইরে যায় না")) { showPrivacy = true }
                    }
                }
            }
        }
    }

    // ---------- PIN dialogs ----------
    when (pinMode) {
        PinMode.NONE -> Unit
        PinMode.ENABLE -> PinDialog(
            title = t("Set a PIN", "একটি পিন সেট করুন"),
            needCurrent = false,
            needNew = true,
            onDismiss = { pinMode = PinMode.NONE },
            onConfirm = { _, new ->
                app.appLock.setPin(new)
                null
            },
        )
        PinMode.CHANGE -> PinDialog(
            title = t("Change PIN", "পিন পরিবর্তন"),
            needCurrent = true,
            needNew = true,
            onDismiss = { pinMode = PinMode.NONE },
            onConfirm = { cur, new ->
                when (app.appLock.verify(cur)) {
                    PinResult.OK -> {
                        app.appLock.setPin(new)
                        null
                    }
                    PinResult.WRONG -> UiMsg("Current PIN is incorrect", "বর্তমান পিন ভুল")
                    PinResult.LOCKED_OUT -> UiMsg("Too many wrong attempts. Try again later.", "অনেকবার ভুল হয়েছে। পরে চেষ্টা করুন।")
                }
            },
        )
        PinMode.DISABLE -> PinDialog(
            title = t("Turn off app lock", "অ্যাপ লক বন্ধ করুন"),
            needCurrent = true,
            needNew = false,
            onDismiss = { pinMode = PinMode.NONE },
            onConfirm = { cur, _ ->
                when (app.appLock.verify(cur)) {
                    PinResult.OK -> {
                        app.appLock.disable()
                        null
                    }
                    PinResult.WRONG -> UiMsg("PIN is incorrect", "পিন ভুল")
                    PinResult.LOCKED_OUT -> UiMsg("Too many wrong attempts. Try again later.", "অনেকবার ভুল হয়েছে। পরে চেষ্টা করুন।")
                }
            },
        )
    }

    if (askRestore) {
        ConfirmDialog(
            title = t("Restore from a backup?", "ব্যাকআপ থেকে রিস্টোর করবেন?"),
            message = t(
                "This REPLACES everything currently in the app with the contents of the backup file. Create a backup first if you want to keep your current data.",
                "এটি অ্যাপের বর্তমান সব ডেটা ব্যাকআপ ফাইলের ডেটা দিয়ে বদলে দেবে। বর্তমান ডেটা রাখতে চাইলে আগে একটি ব্যাকআপ নিন।",
            ),
            confirmLabel = t("Choose file", "ফাইল বাছুন"),
            onConfirm = {
                askRestore = false
                app.appLock.suppressLockFor(120_000)
                restoreLauncher.launch(arrayOf("*/*"))
            },
            onDismiss = { askRestore = false },
        )
    }
    if (askWipe) {
        ConfirmDialog(
            title = t("Delete all data?", "সব ডেটা মুছবেন?"),
            message = t(
                "All transactions, loans and repayments on this phone will be permanently deleted. This cannot be undone. Create a backup first if unsure.",
                "এই ফোনের সব লেনদেন, ঋণ ও কিস্তি চিরতরে মুছে যাবে। এটি আর ফিরিয়ে আনা যাবে না। নিশ্চিত না হলে আগে ব্যাকআপ নিন।",
            ),
            confirmLabel = t("Delete everything", "সব মুছুন"),
            onConfirm = {
                askWipe = false
                vm.wipeAllData()
            },
            onDismiss = { askWipe = false },
        )
    }
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text(t("Privacy", "গোপনীয়তা")) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        t(
                            "• All your data stays on this phone. The app has no internet access, no accounts, no ads and no analytics.\n\n" +
                                "• Your PIN is never stored. Only a salted one-way hash of it is kept.\n\n" +
                                "• Backup and CSV files are created only when you ask, in a place you choose. They are NOT encrypted, so keep them safe and share them only with people you trust.\n\n" +
                                "• If you uninstall the app or clear its data, your records are deleted unless you saved a backup.",
                            "• আপনার সব ডেটা শুধু এই ফোনেই থাকে। অ্যাপে ইন্টারনেট অ্যাক্সেস, অ্যাকাউন্ট, বিজ্ঞাপন বা অ্যানালিটিক্স নেই।\n\n" +
                                "• আপনার পিন কোথাও জমা থাকে না। শুধু এর একটি নিরাপদ এক-মুখী হ্যাশ থাকে।\n\n" +
                                "• ব্যাকআপ ও CSV ফাইল শুধু আপনি চাইলেই, আপনার বেছে নেওয়া জায়গায় তৈরি হয়। এগুলো এনক্রিপ্টেড নয়, তাই নিরাপদে রাখুন এবং শুধু বিশ্বস্ত মানুষের সাথে শেয়ার করুন।\n\n" +
                                "• অ্যাপ আনইনস্টল করলে বা ডেটা মুছলে ব্যাকআপ না থাকলে আপনার হিসাব মুছে যাবে।",
                        ),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text(t("Close", "বন্ধ করুন")) } },
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String?, destructive: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * PIN entry dialog. [onConfirm] returns an error message to show, or null when it succeeded
 * (the dialog then closes itself).
 */
@Composable
private fun PinDialog(
    title: String,
    needCurrent: Boolean,
    needNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: suspend (current: String, new: String) -> UiMsg?,
) {
    val bn = LocalBn.current
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<UiMsg?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun digits(s: String): String = Fmt.toAsciiDigits(s).filter { it in '0'..'9' }.take(6)

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (needCurrent) {
                    OutlinedTextField(
                        value = current,
                        onValueChange = { current = digits(it); error = null },
                        label = { Text(t("Current PIN", "বর্তমান পিন")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                }
                if (needNew) {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = digits(it); error = null },
                        label = { Text(t("New PIN (4 to 6 digits)", "নতুন পিন (৪ থেকে ৬ সংখ্যা)")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = digits(it); error = null },
                        label = { Text(t("Confirm new PIN", "নতুন পিন আবার দিন")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                }
                val e = error
                if (e != null) {
                    Text(if (bn) e.bn else e.en, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    if (needCurrent && current.length < 4) {
                        error = UiMsg("Enter your current PIN", "বর্তমান পিন দিন")
                    } else if (needNew && newPin.length !in 4..6) {
                        error = UiMsg("The PIN must be 4 to 6 digits", "পিন ৪ থেকে ৬ সংখ্যার হতে হবে")
                    } else if (needNew && newPin != confirm) {
                        error = UiMsg("The two PINs do not match", "দুটি পিন মেলেনি")
                    } else {
                        scope.launch {
                            busy = true
                            val msg = onConfirm(current, newPin)
                            busy = false
                            if (msg == null) onDismiss() else error = msg
                        }
                    }
                },
            ) { Text(t("OK", "ঠিক আছে")) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(t("Cancel", "বাতিল")) } },
    )
}
