package com.example.evcharger.repository

import android.content.ContentValues
import android.content.Context
import com.example.evcharger.db.AppDatabaseHelper
import com.example.evcharger.model.User

/**
 * Repository for local user persistence using SQLiteOpenHelper.
 * Provides CRUD with NIC as primary key.
 */
class UserRepository(context: Context) {

    private val dbHelper = AppDatabaseHelper(context.applicationContext)

    fun register(user: User): Boolean {
        dbHelper.writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("nic", user.nic)
                put("full_name", user.fullName)
                put("email", user.email)
                put("phone", user.phone)
                put("is_active", if (user.isActive) 1 else 0)
            }
            return try {
                db.insertOrThrow("users", null, values) > 0
            } catch (_: Exception) {
                false
            }
        }
    }

    fun getByNic(nic: String): User? {
        dbHelper.readableDatabase.use { db ->
            db.query(
                "users",
                arrayOf("nic", "full_name", "email", "phone", "is_active"),
                "nic=?",
                arrayOf(nic),
                null, null, null
            ).use { c ->
                if (c.moveToFirst()) {
                    return User(
                        nic = c.getString(0),
                        fullName = c.getString(1),
                        email = c.getString(2),
                        phone = c.getString(3),
                        isActive = c.getInt(4) == 1
                    )
                }
            }
        }
        return null
    }

    fun update(user: User): Boolean {
        dbHelper.writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("full_name", user.fullName)
                put("email", user.email)
                put("phone", user.phone)
                put("is_active", if (user.isActive) 1 else 0)
            }
            val rows = db.update("users", values, "nic=?", arrayOf(user.nic))
            return rows > 0
        }
    }

    fun deactivate(nic: String): Boolean {
        dbHelper.writableDatabase.use { db ->
            val values = ContentValues().apply { put("is_active", 0) }
            return db.update("users", values, "nic=?", arrayOf(nic)) > 0
        }
    }
}