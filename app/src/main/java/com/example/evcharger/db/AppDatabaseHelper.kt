package com.example.evcharger.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLiteOpenHelper for local persistence.
 * Stores:
 * - User session data (user_session table) - single row for active session
 * - EV Owner accounts (users table)
 * - User profile data cache (user_profiles table)
 * - Charging stations cache for offline display (stations table)
 * - Charging slots cache (slots table)
 * 
 * Database version: 3
 * - v1: Initial users table
 * - v2: Added stations, slots, and user_profiles tables for offline support
 * - v3: Added user_session table (replacing DataStore)
 */
class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "evcharging.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        // User session table (for storing active session data)
        db.execSQL(
            """
            CREATE TABLE user_session(
                id INTEGER PRIMARY KEY CHECK (id = 1),
                token TEXT NOT NULL,
                role TEXT,
                username TEXT,
                nic TEXT,
                expires_at TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        
        // Users table (existing - for local user accounts)
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
        
        // User profiles table (cache for profile data from API)
        db.execSQL(
            """
            CREATE TABLE user_profiles(
                nic TEXT PRIMARY KEY,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL,
                phone_number TEXT NOT NULL,
                address TEXT,
                vehicle_number TEXT,
                vehicle_model TEXT,
                is_active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT,
                updated_at TEXT,
                deactivated_at TEXT,
                last_synced_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        
        // Charging stations table (cache for offline display)
        db.execSQL(
            """
            CREATE TABLE stations(
                station_id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                address TEXT,
                city TEXT,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                type TEXT,
                operator_id TEXT,
                is_active INTEGER NOT NULL DEFAULT 1,
                total_slots INTEGER,
                available_slots INTEGER,
                created_at TEXT,
                updated_at TEXT,
                last_synced_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        
        // Charging slots table (cache for station slots)
        db.execSQL(
            """
            CREATE TABLE slots(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                station_id TEXT NOT NULL,
                slot_number INTEGER NOT NULL,
                connector_type TEXT NOT NULL,
                power_rating INTEGER NOT NULL,
                is_available INTEGER NOT NULL DEFAULT 1,
                is_currently_occupied INTEGER NOT NULL DEFAULT 0,
                current_booking_id TEXT,
                next_booking_time TEXT,
                next_booking_id TEXT,
                last_synced_at INTEGER NOT NULL,
                FOREIGN KEY(station_id) REFERENCES stations(station_id) ON DELETE CASCADE,
                UNIQUE(station_id, slot_number)
            )
            """.trimIndent()
        )
        
        // Create indexes for better query performance
        db.execSQL("CREATE INDEX idx_stations_location ON stations(latitude, longitude)")
        db.execSQL("CREATE INDEX idx_slots_station ON slots(station_id)")
        db.execSQL("CREATE INDEX idx_slots_availability ON slots(is_available)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        when (oldVersion) {
            1 -> {
                // Upgrade from v1 to v2: Add new tables
                db.execSQL(
                    """
                    CREATE TABLE user_profiles(
                        nic TEXT PRIMARY KEY,
                        full_name TEXT NOT NULL,
                        email TEXT NOT NULL,
                        phone_number TEXT NOT NULL,
                        address TEXT,
                        vehicle_number TEXT,
                        vehicle_model TEXT,
                        is_active INTEGER NOT NULL DEFAULT 1,
                        created_at TEXT,
                        updated_at TEXT,
                        deactivated_at TEXT,
                        last_synced_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                
                db.execSQL(
                    """
                    CREATE TABLE stations(
                        station_id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        address TEXT,
                        city TEXT,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        type TEXT,
                        operator_id TEXT,
                        is_active INTEGER NOT NULL DEFAULT 1,
                        total_slots INTEGER,
                        available_slots INTEGER,
                        created_at TEXT,
                        updated_at TEXT,
                        last_synced_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                
                db.execSQL(
                    """
                    CREATE TABLE slots(
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        station_id TEXT NOT NULL,
                        slot_number INTEGER NOT NULL,
                        connector_type TEXT NOT NULL,
                        power_rating INTEGER NOT NULL,
                        is_available INTEGER NOT NULL DEFAULT 1,
                        is_currently_occupied INTEGER NOT NULL DEFAULT 0,
                        current_booking_id TEXT,
                        next_booking_time TEXT,
                        next_booking_id TEXT,
                        last_synced_at INTEGER NOT NULL,
                        FOREIGN KEY(station_id) REFERENCES stations(station_id) ON DELETE CASCADE,
                        UNIQUE(station_id, slot_number)
                    )
                    """.trimIndent()
                )
                
                // Create indexes
                db.execSQL("CREATE INDEX idx_stations_location ON stations(latitude, longitude)")
                db.execSQL("CREATE INDEX idx_slots_station ON slots(station_id)")
                db.execSQL("CREATE INDEX idx_slots_availability ON slots(is_available)")
                
                // Fall through to v3 upgrade
            }
            2 -> {
                // Upgrade from v2 to v3: Add user_session table
                db.execSQL(
                    """
                    CREATE TABLE user_session(
                        id INTEGER PRIMARY KEY CHECK (id = 1),
                        token TEXT NOT NULL,
                        role TEXT,
                        username TEXT,
                        nic TEXT,
                        expires_at TEXT,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
        
        // Handle multi-version upgrades (e.g., v1 -> v3)
        if (oldVersion == 1 && newVersion >= 3) {
            db.execSQL(
                """
                CREATE TABLE user_session(
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    token TEXT NOT NULL,
                    role TEXT,
                    username TEXT,
                    nic TEXT,
                    expires_at TEXT,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }
}