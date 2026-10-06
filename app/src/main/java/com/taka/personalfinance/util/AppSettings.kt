package com.taka.personalfinance.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Language ("en" / "bn") and theme ("system" / "light" / "dark"), remembered between launches. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(
        prefs.getString("lang", null) ?: if (Locale.getDefault().language == "bn") "bn" else "en",
    )
    val language: StateFlow<String> = _language.asStateFlow()

    private val _theme = MutableStateFlow(prefs.getString("theme", "system") ?: "system")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _currency = MutableStateFlow(prefs.getString("currency", "BDT") ?: "BDT")
    val currency: StateFlow<String> = _currency.asStateFlow()

    init {
        Fmt.currency = Currencies.byCode(_currency.value)
    }

    fun setCurrency(code: String) {
        prefs.edit().putString("currency", code).apply()
        _currency.value = code
        Fmt.currency = Currencies.byCode(code)
    }

    fun setLanguage(code: String) {
        prefs.edit().putString("lang", code).apply()
        _language.value = code
    }

    fun setTheme(value: String) {
        prefs.edit().putString("theme", value).apply()
        _theme.value = value
    }
}
