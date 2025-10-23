package com.example.evcharger.auth

import android.content.ContentValues
import android.content.Context
import com.example.evcharger.db.AppDatabaseHelper

data class UserSession(
    val token: String?,
    val role: String?,
    val username: String?,
    val nic: String?,
    val expiresAt: String?
)

/**
 * Manages user session data using SQLite database.
 * 
 * This implementation stores session data in a single-row table (user_session)
 * for secure, offline-capable session management. All local data is stored
 * in SQLite for consistency.
 */
class UserSessionManager(private val context: Context) {
    
    private val dbHelper = AppDatabaseHelper(context.applicationContext)

    /**
     * Save user session data
     */
    fun saveSession(token: String, role: String?, username: String?, nic: String?, expiresAt: String?) {
        dbHelper.writableDatabase.use { db ->
            // Clear existing session first
            db.delete("user_session", null, null)
            
            // Insert new session (id=1 ensures single row)
            val values = ContentValues().apply {
                put("id", 1)
                put("token", token)
                put("role", role)
                put("username", username)
                put("nic", nic)
                put("expires_at", expiresAt)
                put("created_at", System.currentTimeMillis())
            }
            db.insert("user_session", null, values)
        }
    }

    /**
     * Load user session data
     */
    fun loadSession(): UserSession {
        dbHelper.readableDatabase.use { db ->
            val cursor = db.query(
                "user_session",
                arrayOf("token", "role", "username", "nic", "expires_at"),
                "id = ?",
                arrayOf("1"),
                null,
                null,
                null
            )

            cursor.use {
                if (it.moveToFirst()) {
                    return UserSession(
                        token = it.getString(it.getColumnIndexOrThrow("token")),
                        role = it.getString(it.getColumnIndexOrThrow("role")),
                        username = it.getString(it.getColumnIndexOrThrow("username")),
                        nic = it.getString(it.getColumnIndexOrThrow("nic")),
                        expiresAt = it.getString(it.getColumnIndexOrThrow("expires_at"))
                    )
                }
            }
        }
        return UserSession(null, null, null, null, null)
    }

    /**
     * Clear all session data
     */
    fun clearSession() {
        dbHelper.writableDatabase.use { db ->
            db.delete("user_session", null, null)
        }
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        dbHelper.readableDatabase.use { db ->
            val cursor = db.query(
                "user_session",
                arrayOf("token"),
                "id = ?",
                arrayOf("1"),
                null,
                null,
                null
            )

            cursor.use {
                if (it.moveToFirst()) {
                    val token = it.getString(it.getColumnIndexOrThrow("token"))
                    return !token.isNullOrBlank()
                }
            }
        }
        return false
    }
}
