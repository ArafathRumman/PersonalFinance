package com.taka.personalfinance.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {
    private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun isAvailable(context: Context): Boolean =
        try {
            BiometricManager.from(context).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }

    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negative: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {},
    ) {
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure()
                }
            }
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negative)
                .setAllowedAuthenticators(AUTH)
                .build()
            BiometricPrompt(activity, executor, callback).authenticate(info)
        } catch (e: Exception) {
            onFailure()
        }
    }
}
