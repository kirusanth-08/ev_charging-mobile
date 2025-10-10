package com.example.evcharger.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

data class UserSession(
    val token: String?,
    val role: String?,
    val username: String?,
    val nic: String?,
    val expiresAt: String?
)

/**
 * Manages user session data using DataStore Preferences.
 * 
 * DataStore is a modern, type-safe data storage solution that uses Kotlin coroutines
 * and Flow for asynchronous operations. It's the recommended replacement for SharedPreferences.
 * 
 * Note: This implementation provides both synchronous (blocking) and asynchronous (Flow) APIs
 * for backward compatibility with existing code.
 */
class UserSessionManager(private val context: Context) {
    
    companion object {
        // DataStore instance - created as a singleton per context
        private val Context.dataStore by preferencesDataStore(name = "ev_charger_secure_prefs")
        
        // Preference keys
        private val KEY_TOKEN = stringPreferencesKey("key_token")
        private val KEY_ROLE = stringPreferencesKey("key_role")
        private val KEY_USERNAME = stringPreferencesKey("key_username")
        private val KEY_NIC = stringPreferencesKey("key_nic")
        private val KEY_EXPIRES_AT = stringPreferencesKey("key_expires_at")
    }

    /**
     * Save user session data (synchronous version for compatibility)
     */
    fun saveSession(token: String, role: String?, username: String?, nic: String?, expiresAt: String?) {
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[KEY_TOKEN] = token
                role?.let { preferences[KEY_ROLE] = it }
                username?.let { preferences[KEY_USERNAME] = it }
                nic?.let { preferences[KEY_NIC] = it }
                expiresAt?.let { preferences[KEY_EXPIRES_AT] = it }
            }
        }
    }

    /**
     * Save user session data (asynchronous version)
     */
    suspend fun saveSessionAsync(token: String, role: String?, username: String?, nic: String?, expiresAt: String?) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
            role?.let { preferences[KEY_ROLE] = it }
            username?.let { preferences[KEY_USERNAME] = it }
            nic?.let { preferences[KEY_NIC] = it }
            expiresAt?.let { preferences[KEY_EXPIRES_AT] = it }
        }
    }

    /**
     * Load user session data (synchronous version for compatibility)
     */
    fun loadSession(): UserSession {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            UserSession(
                token = preferences[KEY_TOKEN],
                role = preferences[KEY_ROLE],
                username = preferences[KEY_USERNAME],
                nic = preferences[KEY_NIC],
                expiresAt = preferences[KEY_EXPIRES_AT]
            )
        }
    }

    /**
     * Load user session data as Flow (asynchronous, reactive)
     */
    fun loadSessionFlow(): Flow<UserSession> {
        return context.dataStore.data.map { preferences ->
            UserSession(
                token = preferences[KEY_TOKEN],
                role = preferences[KEY_ROLE],
                username = preferences[KEY_USERNAME],
                nic = preferences[KEY_NIC],
                expiresAt = preferences[KEY_EXPIRES_AT]
            )
        }
    }

    /**
     * Clear all session data (synchronous version for compatibility)
     */
    fun clearSession() {
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }

    /**
     * Clear all session data (asynchronous version)
     */
    suspend fun clearSessionAsync() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    /**
     * Check if user is logged in (synchronous version for compatibility)
     */
    fun isLoggedIn(): Boolean {
        return runBlocking {
            val preferences = context.dataStore.data.first()
            val token = preferences[KEY_TOKEN]
            !token.isNullOrBlank()
        }
    }

    /**
     * Check if user is logged in as Flow (asynchronous, reactive)
     */
    fun isLoggedInFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            val token = preferences[KEY_TOKEN]
            !token.isNullOrBlank()
        }
    }
}
