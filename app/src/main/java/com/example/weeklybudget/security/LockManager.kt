package com.example.weeklybudget.security

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * قفل التطبيق برمز سري (4 إلى 6 أرقام). لا يُخزَّن الرمز نفسه، بل ناتج PBKDF2 مع ملح عشوائي.
 */
class LockManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("app_lock", Context.MODE_PRIVATE)

    val isEnabled: Boolean get() = prefs.contains(KEY_HASH)
    val pinLength: Int get() = prefs.getInt(KEY_LENGTH, 4)

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) { prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply() }

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, Base64.getEncoder().encodeToString(salt))
            .putString(KEY_HASH, hash(pin, salt))
            .putInt(KEY_LENGTH, pin.length)
            .apply()
    }

    fun verify(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val expected = prefs.getString(KEY_HASH, null) ?: return false
        val actual = hash(pin, Base64.getDecoder().decode(salt))
        return MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())
    }

    fun disable() {
        prefs.edit().remove(KEY_HASH).remove(KEY_SALT).remove(KEY_LENGTH).remove(KEY_BIOMETRIC).apply()
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 20_000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(bytes)
    }

    private companion object {
        const val KEY_HASH = "hash"
        const val KEY_SALT = "salt"
        const val KEY_LENGTH = "length"
        const val KEY_BIOMETRIC = "biometric"
    }
}
