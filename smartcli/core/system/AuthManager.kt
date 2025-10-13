package com.smartcli.core.system

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AuthManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "smartcli_auth"
        private const val KEY_ALIAS = "smartcli_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        createEncryptedPreferences()
    }
    
    private fun createEncryptedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    fun saveSessionToken(token: String): Boolean {
        return try {
            sharedPreferences.edit().putString("session_token", token).apply()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getSessionToken(): String? {
        return sharedPreferences.getString("session_token", null)
    }
    
    fun clearSession(): Boolean {
        return try {
            sharedPreferences.edit().clear().apply()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun isAuthenticated(): Boolean {
        return getSessionToken() != null
    }
    
    fun validatePin(pin: String): Boolean {
        val storedPin = sharedPreferences.getString("user_pin", null)
        return storedPin == pin.hashCode().toString()
    }
    
    fun setPin(pin: String): Boolean {
        return try {
            sharedPreferences.edit()
                .putString("user_pin", pin.hashCode().toString())
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }
}
