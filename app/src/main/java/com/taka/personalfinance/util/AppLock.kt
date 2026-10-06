package com.taka.personalfinance.util

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class PinResult { OK, WRONG, LOCKED_OUT }

/**
 * PIN lock. The PIN itself is never stored: only a salted PBKDF2 hash.
 * 5 wrong tries in a row lock the keypad for a while (30 s, then longer).
 */
class AppLock(context: Context) {
    private val prefs = context.getSharedPreferences("lock", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getString(K_HASH, null) != null)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _biometric = MutableStateFlow(prefs.getBoolean(K_BIO, false) && _enabled.value)
    val biometricEnabled: StateFlow<Boolean> = _biometric.asStateFlow()

    private val _locked = MutableStateFlow(_enabled.value)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    @Volatile
    private var suppressUntil = 0L

    val pinLength: Int get() = prefs.getInt(K_LEN, 4)

    /** Call before opening a system file picker so the app does not lock itself when you return. */
    fun suppressLockFor(ms: Long) {
        suppressUntil = System.currentTimeMillis() + ms
    }

    fun onAppBackgrounded() {
        if (!_enabled.value) return
        if (System.currentTimeMillis() < suppressUntil) {
            suppressUntil = 0L
            return
        }
        _locked.value = true
    }

    fun unlock() {
        _locked.value = false
    }

    suspend fun setPin(pin: String) = withContext(Dispatchers.Default) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = derive(pin, salt)
        prefs.edit()
            .putString(K_SALT, enc(salt))
            .putString(K_HASH, enc(hash))
            .putInt(K_LEN, pin.length)
            .putInt(K_FAILS, 0)
            .putLong(K_UNTIL, 0L)
            .commit()
        _enabled.value = true
    }

    suspend fun verify(pin: String): PinResult = withContext(Dispatchers.Default) {
        if (remainingLockoutMs() > 0L) return@withContext PinResult.LOCKED_OUT
        val salt = prefs.getString(K_SALT, null)?.let { dec(it) }
        val stored = prefs.getString(K_HASH, null)?.let { dec(it) }
        if (salt == null || stored == null) return@withContext PinResult.WRONG
        val ok = MessageDigest.isEqual(derive(pin, salt), stored)
        if (ok) {
            prefs.edit().putInt(K_FAILS, 0).putLong(K_UNTIL, 0L).apply()
            PinResult.OK
        } else {
            val fails = prefs.getInt(K_FAILS, 0) + 1
            val editor = prefs.edit().putInt(K_FAILS, fails)
            if (fails % 5 == 0) {
                val level = (fails / 5 - 1).coerceAtMost(5)
                val ms = minOf(30_000L * (1L shl level), 600_000L)
                editor.putLong(K_UNTIL, System.currentTimeMillis() + ms)
            }
            editor.apply()
            PinResult.WRONG
        }
    }

    fun remainingLockoutMs(): Long {
        val left = prefs.getLong(K_UNTIL, 0L) - System.currentTimeMillis()
        return left.coerceIn(0L, 600_000L)
    }

    fun setBiometric(on: Boolean) {
        prefs.edit().putBoolean(K_BIO, on).apply()
        _biometric.value = on && _enabled.value
    }

    fun disable() {
        prefs.edit().clear().commit()
        _enabled.value = false
        _biometric.value = false
        _locked.value = false
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun enc(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun dec(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)

    private companion object {
        const val K_HASH = "pin_hash"
        const val K_SALT = "pin_salt"
        const val K_LEN = "pin_len"
        const val K_BIO = "bio"
        const val K_FAILS = "fails"
        const val K_UNTIL = "locked_until"
    }
}
