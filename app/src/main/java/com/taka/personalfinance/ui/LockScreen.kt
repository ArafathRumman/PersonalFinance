package com.taka.personalfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taka.personalfinance.util.AppLock
import com.taka.personalfinance.util.BiometricHelper
import com.taka.personalfinance.util.Fmt
import com.taka.personalfinance.util.PinResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LockScreen(appLock: AppLock, activity: FragmentActivity) {
    val bn = LocalBn.current
    val scope = rememberCoroutineScope()
    val bioEnabled by appLock.biometricEnabled.collectAsStateWithLifecycle()
    val pinLength = remember { appLock.pinLength }
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var lockoutLeft by remember { mutableStateOf(appLock.remainingLockoutMs()) }
    var prompted by remember { mutableStateOf(false) }

    val bioTitle = t("Unlock Personal Finance", "পার্সোনাল ফাইন্যান্স আনলক করুন")
    val bioSubtitle = t("Confirm it's you", "নিশ্চিত করুন এটি আপনি")
    val bioCancel = t("Use PIN", "পিন ব্যবহার করুন")

    LaunchedEffect(lockoutLeft > 0L) {
        while (lockoutLeft > 0L) {
            delay(500)
            lockoutLeft = appLock.remainingLockoutMs()
        }
    }

    fun promptBiometric() {
        BiometricHelper.prompt(
            activity = activity,
            title = bioTitle,
            subtitle = bioSubtitle,
            negative = bioCancel,
            onSuccess = { appLock.unlock() },
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (bioEnabled && !prompted && lockoutLeft <= 0L) {
            prompted = true
            promptBiometric()
        }
    }

    fun submit(p: String) {
        scope.launch {
            busy = true
            when (appLock.verify(p)) {
                PinResult.OK -> appLock.unlock()
                PinResult.WRONG -> {
                    wrong = true
                    pin = ""
                }
                PinResult.LOCKED_OUT -> pin = ""
            }
            lockoutLeft = appLock.remainingLockoutMs()
            busy = false
        }
    }

    fun onDigit(d: String) {
        if (busy || lockoutLeft > 0L) return
        if (pin.length < pinLength) {
            pin += d
            wrong = false
            if (pin.length == pinLength) submit(pin)
        }
    }

    fun onBackspace() {
        if (pin.isNotEmpty()) pin = pin.dropLast(1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {}
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🔒", fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text(t("Personal Finance", "পার্সোনাল ফাইন্যান্স"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                lockoutLeft > 0L -> t("Too many wrong attempts. Try again in ", "অনেকবার ভুল হয়েছে। আবার চেষ্টা করুন ") +
                    Fmt.num(((lockoutLeft + 999L) / 1000L).toInt(), bn) + t(" s", " সেকেন্ড পর")
                wrong -> t("Wrong PIN. Try again.", "ভুল পিন। আবার চেষ্টা করুন।")
                else -> t("Enter your PIN to unlock", "আনলক করতে আপনার পিন দিন")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (wrong || lockoutLeft > 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            for (i in 0 until pinLength) {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < pin.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            rows.forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    r.forEach { d -> KeyButton(Fmt.num(d, bn), onClick = { onDigit(d) }) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (bioEnabled) {
                    KeyButton("👆", description = t("Use fingerprint or face", "ফিঙ্গারপ্রিন্ট বা ফেস ব্যবহার করুন"), onClick = { promptBiometric() })
                } else {
                    Spacer(Modifier.size(68.dp))
                }
                KeyButton(Fmt.num("0", bn), onClick = { onDigit("0") })
                KeyButton("⌫", description = t("Delete", "মুছুন"), onClick = { onBackspace() })
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            t(
                "Forgot your PIN? You can only reset it by clearing the app's data in Android settings, which erases your records. Restore a backup afterwards.",
                "পিন ভুলে গেছেন? অ্যান্ড্রয়েড সেটিংসে অ্যাপের ডেটা মুছে রিসেট করতে হবে, এতে আপনার হিসাব মুছে যাবে। পরে ব্যাকআপ থেকে ফিরিয়ে আনুন।",
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun KeyButton(label: String, onClick: () -> Unit, description: String? = null) {
    Box(
        Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 26.sp, fontWeight = FontWeight.Medium)
    }
}
