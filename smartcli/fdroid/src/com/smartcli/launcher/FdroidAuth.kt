package com.smartcli.launcher

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class FdroidAuth(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "fdroid_smartcli_auth"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun setupInitialPin(pin: String): Boolean {
        return if (pin.length >= 4) {
            sharedPreferences.edit()
                .putString("user_pin_hash", pin.sha256())
                .putBoolean("pin_setup_complete", true)
                .apply()
            true
        } else {
            false
        }
    }

    fun validatePin(pin: String): Boolean {
        val storedHash = sharedPreferences.getString("user_pin_hash", null)
        return storedHash == pin.sha256()
    }

    fun isPinSetupComplete(): Boolean {
        return sharedPreferences.getBoolean("pin_setup_complete", false)
    }

    fun clearAuthData(): Boolean {
        return try {
            sharedPreferences.edit().clear().apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Simple SHA-256 implementation for F-Droid (no Google Play Services)
    private fun String.sha256(): String {
        val bytes = this.toByteArray()
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
