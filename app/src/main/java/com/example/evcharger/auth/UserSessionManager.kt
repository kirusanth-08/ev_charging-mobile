package com.example.evcharger.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class UserSession(
    val token: String?,
    val role: String?,
    val username: String?,
    val expiresAt: String?
)

class UserSessionManager(context: Context) {
    private val prefsName = "ev_charger_secure_prefs"

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        prefsName,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_TOKEN = "key_token"
        private const val KEY_ROLE = "key_role"
        private const val KEY_USERNAME = "key_username"
        private const val KEY_EXPIRES_AT = "key_expires_at"
    }

    fun saveSession(token: String, role: String?, username: String?, expiresAt: String?) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_ROLE, role)
            .putString(KEY_USERNAME, username)
            .putString(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun loadSession(): UserSession {
        return UserSession(
            token = prefs.getString(KEY_TOKEN, null),
            role = prefs.getString(KEY_ROLE, null),
            username = prefs.getString(KEY_USERNAME, null),
            expiresAt = prefs.getString(KEY_EXPIRES_AT, null)
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
