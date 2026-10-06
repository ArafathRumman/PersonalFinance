package com.taka.personalfinance.ui

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taka.personalfinance.PFApp

class MainActivity : FragmentActivity() {
    private val vm: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PFApp

        setContent {
            val lang by app.settings.language.collectAsStateWithLifecycle()
            val theme by app.settings.theme.collectAsStateWithLifecycle()
            val lockEnabled by app.appLock.enabled.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (theme) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(
                        AndroidColor.argb(0xE6, 0xFF, 0xFF, 0xFF),
                        AndroidColor.argb(0x80, 0x1B, 0x1B, 0x1B),
                    ) { dark },
                )
                onDispose {}
            }

            // Hide the app from screenshots and the recent-apps preview while a PIN is set.
            LaunchedEffect(lockEnabled) {
                if (lockEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            PersonalFinanceTheme(dark) {
                CompositionLocalProvider(LocalBn provides (lang == "bn")) {
                    AppRoot(vm, app, this@MainActivity)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            (application as PFApp).appLock.onAppBackgrounded()
        }
    }
}
