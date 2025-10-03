package com.example.evcharger.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLiteOpenHelper for local persistence.
 * Stores EV Owner accounts locally (NIC as PK).
 */
class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "evcharging.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users(
                nic TEXT PRIMARY KEY,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL,
                phone TEXT NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        // Optionally cache reservations if needed later
        // CREATE TABLE reservations_cache(...)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle schema migrations when bumping DB version
    }
}