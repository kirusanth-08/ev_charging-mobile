package com.example.evcharger.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.evcharger.db.AppDatabaseHelper
import com.example.evcharger.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing offline data caching.
 * Handles storage and retrieval of charging stations and user profiles for offline access.
 */
class OfflineDataRepository(context: Context) {

    private val dbHelper = AppDatabaseHelper(context.applicationContext)

    // ==================== STATION OPERATIONS ====================

    /**
     * Cache a list of charging stations for offline access
     * @param stations List of stations to cache
     * @return Number of stations successfully cached
     */
    suspend fun cacheStations(stations: List<BackendStationV2>): Int = withContext(Dispatchers.IO) {
        var cached = 0
        val currentTime = System.currentTimeMillis()
        
        dbHelper.writableDatabase.use { db ->
            db.beginTransaction()
            try {
                stations.forEach { station ->
                    val values = ContentValues().apply {
                        put("station_id", station.stationId)
                        put("name", station.name)
                        put("address", station.location.address)
                        put("city", station.location.city)
                        put("latitude", station.location.latitude)
                        put("longitude", station.location.longitude)
                        put("type", station.type)
                        put("operator_id", station.operatorId)
                        put("is_active", if (station.isActive) 1 else 0)
                        put("total_slots", station.totalSlots)
                        put("available_slots", station.availableSlots)
                        put("created_at", station.createdAt)
                        put("updated_at", station.updatedAt)
                        put("last_synced_at", currentTime)
                    }
                    
                    val rows = db.insertWithOnConflict("stations", null, values, 
                        android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                    
                    if (rows != -1L) {
                        cached++
                        // Cache slots for this station
                        cacheStationSlots(station.stationId, station.slots, currentTime)
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        cached
    }

    /**
     * Cache slots for a specific station
     * @param stationId The station ID
     * @param slots List of slots to cache
     * @param syncTime The sync timestamp
     */
    private fun cacheStationSlots(stationId: String, slots: List<BackendSlot>, syncTime: Long) {
        dbHelper.writableDatabase.use { db ->
            // Delete existing slots for this station
            db.delete("slots", "station_id=?", arrayOf(stationId))
            
            // Insert new slots
            slots.forEach { slot ->
                val values = ContentValues().apply {
                    put("station_id", stationId)
                    put("slot_number", slot.slotNumber)
                    put("connector_type", slot.connectorType)
                    put("power_rating", slot.powerRating)
                    put("is_available", if (slot.isAvailable) 1 else 0)
                    put("is_currently_occupied", 0)
                    put("last_synced_at", syncTime)
                }
                db.insert("slots", null, values)
            }
        }
    }

    /**
     * Cache station availability data including slot details
     * @param availabilityData The availability data from API
     */
    suspend fun cacheStationAvailability(availabilityData: StationAvailabilityData) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        
        dbHelper.writableDatabase.use { db ->
            db.beginTransaction()
            try {
                // Update station availability counts
                val stationValues = ContentValues().apply {
                    put("total_slots", availabilityData.totalSlots)
                    put("available_slots", availabilityData.availableSlots)
                    put("last_synced_at", currentTime)
                }
                db.update("stations", stationValues, "station_id=?", arrayOf(availabilityData.stationId))
                
                // Update slot details
                availabilityData.slotDetails.forEach { slot ->
                    val slotValues = ContentValues().apply {
                        put("is_available", if (slot.isAvailable) 1 else 0)
                        put("is_currently_occupied", if (slot.isCurrentlyOccupied) 1 else 0)
                        put("current_booking_id", slot.currentBookingId)
                        put("next_booking_time", slot.nextBookingTime)
                        put("next_booking_id", slot.nextBookingId)
                        put("last_synced_at", currentTime)
                    }
                    db.update("slots", slotValues, 
                        "station_id=? AND slot_number=?", 
                        arrayOf(availabilityData.stationId, slot.slotNumber.toString()))
                }
                
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    /**
     * Get all cached stations for offline display
     * @return List of cached stations with their slots
     */
    suspend fun getCachedStations(): List<BackendStationV2> = withContext(Dispatchers.IO) {
        val stations = mutableListOf<BackendStationV2>()
        
        dbHelper.readableDatabase.use { db ->
            db.query("stations", null, null, null, null, null, "name ASC").use { cursor ->
                while (cursor.moveToNext()) {
                    val stationId = cursor.getString(cursor.getColumnIndexOrThrow("station_id"))
                    val slots = getStationSlots(stationId)
                    
                    stations.add(BackendStationV2(
                        stationId = stationId,
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        location = BackendLocation(
                            address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                            city = cursor.getString(cursor.getColumnIndexOrThrow("city")),
                            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                        ),
                        type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                        slots = slots,
                        operatorId = cursor.getString(cursor.getColumnIndexOrThrow("operator_id")),
                        isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1,
                        totalSlots = cursor.getIntOrNull(cursor.getColumnIndexOrThrow("total_slots")),
                        availableSlots = cursor.getIntOrNull(cursor.getColumnIndexOrThrow("available_slots")),
                        createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                        updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updated_at"))
                    ))
                }
            }
        }
        
        stations
    }

    /**
     * Get stations near a location for offline display
     * @param latitude User's current latitude
     * @param longitude User's current longitude
     * @param radiusKm Search radius in kilometers
     * @return List of nearby cached stations
     */
    suspend fun getCachedStationsNearLocation(
        latitude: Double, 
        longitude: Double, 
        radiusKm: Double = 50.0
    ): List<BackendStationV2> = withContext(Dispatchers.IO) {
        val stations = mutableListOf<BackendStationV2>()
        
        // Simple bounding box calculation (not precise, but good for offline cache)
        val latDelta = radiusKm / 111.0 // Approximate km per degree latitude
        val lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)))
        
        val minLat = latitude - latDelta
        val maxLat = latitude + latDelta
        val minLon = longitude - lonDelta
        val maxLon = longitude + lonDelta
        
        dbHelper.readableDatabase.use { db ->
            val query = """
                SELECT * FROM stations 
                WHERE latitude BETWEEN ? AND ? 
                AND longitude BETWEEN ? AND ?
                AND is_active = 1
                ORDER BY name ASC
            """.trimIndent()
            
            db.rawQuery(query, arrayOf(
                minLat.toString(), maxLat.toString(),
                minLon.toString(), maxLon.toString()
            )).use { cursor ->
                while (cursor.moveToNext()) {
                    val stationId = cursor.getString(cursor.getColumnIndexOrThrow("station_id"))
                    val slots = getStationSlots(stationId)
                    
                    stations.add(BackendStationV2(
                        stationId = stationId,
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        location = BackendLocation(
                            address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                            city = cursor.getString(cursor.getColumnIndexOrThrow("city")),
                            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                        ),
                        type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                        slots = slots,
                        operatorId = cursor.getString(cursor.getColumnIndexOrThrow("operator_id")),
                        isActive = true,
                        totalSlots = cursor.getIntOrNull(cursor.getColumnIndexOrThrow("total_slots")),
                        availableSlots = cursor.getIntOrNull(cursor.getColumnIndexOrThrow("available_slots")),
                        createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                        updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updated_at"))
                    ))
                }
            }
        }
        
        stations
    }

    /**
     * Get slots for a specific station
     * @param stationId The station ID
     * @return List of cached slots
     */
    private fun getStationSlots(stationId: String): List<BackendSlot> {
        val slots = mutableListOf<BackendSlot>()
        
        dbHelper.readableDatabase.use { db ->
            db.query(
                "slots",
                null,
                "station_id=?",
                arrayOf(stationId),
                null, null,
                "slot_number ASC"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    slots.add(BackendSlot(
                        slotNumber = cursor.getInt(cursor.getColumnIndexOrThrow("slot_number")),
                        connectorType = cursor.getString(cursor.getColumnIndexOrThrow("connector_type")),
                        powerRating = cursor.getInt(cursor.getColumnIndexOrThrow("power_rating")),
                        isAvailable = cursor.getInt(cursor.getColumnIndexOrThrow("is_available")) == 1
                    ))
                }
            }
        }
        
        return slots
    }

    /**
     * Get a single cached station by ID
     * @param stationId The station ID
     * @return The cached station or null if not found
     */
    suspend fun getCachedStation(stationId: String): BackendStationV2? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.use { db ->
            db.query(
                "stations",
                null,
                "station_id=?",
                arrayOf(stationId),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val slots = getStationSlots(stationId)
                    BackendStationV2(
                        stationId = stationId,
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        location = BackendLocation(
                            address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                            city = cursor.getString(cursor.getColumnIndexOrThrow("city")),
                            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                        ),
                        type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                        slots = slots,
                        operatorId = cursor.getString(cursor.getColumnIndexOrThrow("operator_id")),
                        isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1,
                        totalSlots = cursor.getIntOrNull(cursor.getColumnIndexOrThrow("total_slots")),
                        availableSlots = cursor.getIntOrNull(cursor.getColumnIndexOrThrow("available_slots")),
                        createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                        updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updated_at"))
                    )
                } else null
            }
        }
    }

    /**
     * Clear all cached station data
     */
    suspend fun clearStationCache() = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.use { db ->
            db.delete("slots", null, null)
            db.delete("stations", null, null)
        }
    }

    /**
     * Get the last sync time for station data
     * @return Timestamp of last sync or 0 if never synced
     */
    suspend fun getLastStationSyncTime(): Long = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.use { db ->
            db.rawQuery("SELECT MAX(last_synced_at) FROM stations", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            }
        }
    }

    // ==================== USER PROFILE OPERATIONS ====================

    /**
     * Cache user profile data for offline access
     * @param profile The user profile to cache
     */
    suspend fun cacheUserProfile(profile: EvOwnerProfile) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        
        dbHelper.writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("nic", profile.nic)
                put("full_name", profile.fullName)
                put("email", profile.email)
                put("phone_number", profile.phoneNumber)
                put("address", profile.address)
                put("vehicle_number", profile.vehicleNumber)
                put("vehicle_model", profile.vehicleModel)
                put("is_active", if (profile.isActive) 1 else 0)
                put("created_at", profile.createdAt)
                put("updated_at", profile.updatedAt)
                put("deactivated_at", profile.deactivatedAt)
                put("last_synced_at", currentTime)
            }
            
            db.insertWithOnConflict("user_profiles", null, values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    /**
     * Get cached user profile
     * @param nic The user's NIC
     * @return The cached profile or null if not found
     */
    suspend fun getCachedUserProfile(nic: String): EvOwnerProfile? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.use { db ->
            db.query(
                "user_profiles",
                null,
                "nic=?",
                arrayOf(nic),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    EvOwnerProfile(
                        nic = cursor.getString(cursor.getColumnIndexOrThrow("nic")),
                        fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name")),
                        email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                        phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phone_number")),
                        address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                        vehicleNumber = cursor.getString(cursor.getColumnIndexOrThrow("vehicle_number")),
                        vehicleModel = cursor.getString(cursor.getColumnIndexOrThrow("vehicle_model")),
                        isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1,
                        createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                        updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updated_at")),
                        deactivatedAt = cursor.getString(cursor.getColumnIndexOrThrow("deactivated_at"))
                    )
                } else null
            }
        }
    }

    /**
     * Clear cached user profile
     * @param nic The user's NIC
     */
    suspend fun clearUserProfileCache(nic: String) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.use { db ->
            db.delete("user_profiles", "nic=?", arrayOf(nic))
        }
    }

    /**
     * Get the last sync time for user profile
     * @param nic The user's NIC
     * @return Timestamp of last sync or 0 if never synced
     */
    suspend fun getProfileLastSyncTime(nic: String): Long = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.use { db ->
            db.query(
                "user_profiles",
                arrayOf("last_synced_at"),
                "nic=?",
                arrayOf(nic),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Extension function to safely get nullable Int from cursor
     */
    private fun Cursor.getIntOrNull(columnIndex: Int): Int? {
        return if (isNull(columnIndex)) null else getInt(columnIndex)
    }

    /**
     * Check if offline data is stale (older than threshold)
     * @param lastSyncTime The timestamp of last sync
     * @param maxAgeHours Maximum age in hours before data is considered stale
     * @return True if data is stale
     */
    fun isDataStale(lastSyncTime: Long, maxAgeHours: Int = 24): Boolean {
        val maxAgeMillis = maxAgeHours * 60 * 60 * 1000L
        return (System.currentTimeMillis() - lastSyncTime) > maxAgeMillis
    }

    /**
     * Get database statistics
     * @return Map containing count of cached items
     */
    suspend fun getDatabaseStats(): Map<String, Int> = withContext(Dispatchers.IO) {
        val stats = mutableMapOf<String, Int>()
        
        dbHelper.readableDatabase.use { db ->
            db.rawQuery("SELECT COUNT(*) FROM stations", null).use { cursor ->
                if (cursor.moveToFirst()) stats["stations"] = cursor.getInt(0)
            }
            
            db.rawQuery("SELECT COUNT(*) FROM slots", null).use { cursor ->
                if (cursor.moveToFirst()) stats["slots"] = cursor.getInt(0)
            }
            
            db.rawQuery("SELECT COUNT(*) FROM user_profiles", null).use { cursor ->
                if (cursor.moveToFirst()) stats["profiles"] = cursor.getInt(0)
            }
        }
        
        stats
    }
}
