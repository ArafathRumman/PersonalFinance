package com.taka.personalfinance.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** true when the app language is Bengali. */
val LocalBn = staticCompositionLocalOf { false }

/** Picks the English or Bengali text for the current app language. */
@Composable
@ReadOnlyComposable
fun t(en: String, bn: String): String = if (LocalBn.current) bn else en
