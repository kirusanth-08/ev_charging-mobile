package com.example.evcharger.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.evcharger.db.AppDatabaseHelper
import com.example.evcharger.model.BackendLocation
import com.example.evcharger.model.BackendSlot
import com.example.evcharger.model.BackendStationV2

/**
 * Repository for caching charging station data locally for offline support.
 * Handles CRUD operations for stations and slots in SQLite database.
 */
class StationCacheRepository(context: Context) {

    private val dbHelper = AppDatabaseHelper(context.applicationContext)

    /**
     * Cache a single station with its slots
     */
    fun cacheStation(station: BackendStationV2) {
        dbHelper.writableDatabase.use { db ->
            db.beginTransaction()
            try {
                // Insert or update station
                val stationValues = ContentValues().apply {
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
                    put("last_synced_at", System.currentTimeMillis())
                }
                
                db.insertWithOnConflict(
                    "stations",
                    null,
                    stationValues,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )

                // Delete old slots for this station
                db.delete("slots", "station_id = ?", arrayOf(station.stationId))

                // Insert new slots
                station.slots.forEach { slot ->
                    val slotValues = ContentValues().apply {
                        put("station_id", station.stationId)
                        put("slot_number", slot.slotNumber)
                        put("connector_type", slot.connectorType)
                        put("power_rating", slot.powerRating)
                        put("is_available", if (slot.isAvailable) 1 else 0)
                        put("last_synced_at", System.currentTimeMillis())
                    }
                    db.insert("slots", null, slotValues)
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    /**
     * Cache multiple stations at once (for bulk operations like nearby stations)
     */
    fun cacheStations(stations: List<BackendStationV2>) {
        dbHelper.writableDatabase.use { db ->
            db.beginTransaction()
            try {
                stations.forEach { station ->
                    // Insert or update station
                    val stationValues = ContentValues().apply {
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
                        put("last_synced_at", System.currentTimeMillis())
                    }
                    
                    db.insertWithOnConflict(
                        "stations",
                        null,
                        stationValues,
                        android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                    )

                    // Delete old slots for this station
                    db.delete("slots", "station_id = ?", arrayOf(station.stationId))

                    // Insert new slots
                    station.slots.forEach { slot ->
                        val slotValues = ContentValues().apply {
                            put("station_id", station.stationId)
                            put("slot_number", slot.slotNumber)
                            put("connector_type", slot.connectorType)
                            put("power_rating", slot.powerRating)
                            put("is_available", if (slot.isAvailable) 1 else 0)
                            put("last_synced_at", System.currentTimeMillis())
                        }
                        db.insert("slots", null, slotValues)
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    /**
     * Get a cached station by ID
     */
    fun getStation(stationId: String): BackendStationV2? {
        dbHelper.readableDatabase.use { db ->
            val cursor = db.query(
                "stations",
                null,
                "station_id = ?",
                arrayOf(stationId),
                null,
                null,
                null
            )

            cursor.use {
                if (it.moveToFirst()) {
                    val station = cursorToStation(it)
                    val slots = getSlots(stationId)
                    return station.copy(slots = slots)
                }
            }
        }
        return null
    }

    /**
     * Get cached stations near a location
     * @param latitude Center latitude
     * @param longitude Center longitude
     * @param radiusKm Radius in kilometers (default 10km)
     */
    fun getNearbyStations(latitude: Double, longitude: Double, radiusKm: Double = 10.0): List<BackendStationV2> {
        val stations = mutableListOf<BackendStationV2>()
        
        // Simple bounding box calculation (rough approximation)
        // 1 degree latitude ≈ 111 km
        // 1 degree longitude ≈ 111 km * cos(latitude)
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)))
        
        val minLat = latitude - latDelta
        val maxLat = latitude + latDelta
        val minLon = longitude - lonDelta
        val maxLon = longitude + lonDelta

        dbHelper.readableDatabase.use { db ->
            val cursor = db.query(
                "stations",
                null,
                "latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ? AND is_active = 1",
                arrayOf(minLat.toString(), maxLat.toString(), minLon.toString(), maxLon.toString()),
                null,
                null,
                null
            )

            cursor.use {
                while (it.moveToNext()) {
                    val station = cursorToStation(it)
                    val slots = getSlots(station.stationId)
                    stations.add(station.copy(slots = slots))
                }
            }
        }

        // Calculate actual distances and filter by radius
        return stations.filter { station ->
            val distance = calculateDistance(
                latitude, longitude,
                station.location.latitude, station.location.longitude
            )
            distance <= radiusKm
        }.sortedBy { station ->
            calculateDistance(
                latitude, longitude,
                station.location.latitude, station.location.longitude
            )
        }
    }

    /**
     * Get all cached stations
     */
    fun getAllStations(): List<BackendStationV2> {
        val stations = mutableListOf<BackendStationV2>()

        dbHelper.readableDatabase.use { db ->
            val cursor = db.query(
                "stations",
                null,
                "is_active = 1",
                null,
                null,
                null,
                "name ASC"
            )

            cursor.use {
                while (it.moveToNext()) {
                    val station = cursorToStation(it)
                    val slots = getSlots(station.stationId)
                    stations.add(station.copy(slots = slots))
                }
            }
        }

        return stations
    }

    /**
     * Get slots for a specific station
     */
    private fun getSlots(stationId: String): List<BackendSlot> {
        val slots = mutableListOf<BackendSlot>()

        dbHelper.readableDatabase.use { db ->
            val cursor = db.query(
                "slots",
                null,
                "station_id = ?",
                arrayOf(stationId),
                null,
                null,
                "slot_number ASC"
            )

            cursor.use {
                while (it.moveToNext()) {
                    slots.add(cursorToSlot(it))
                }
            }
        }

        return slots
    }

    /**
     * Check if cache is stale (older than specified hours)
     */
    fun isCacheStale(maxAgeHours: Int = 24): Boolean {
        dbHelper.readableDatabase.use { db ->
            val cursor = db.rawQuery(
                "SELECT MIN(last_synced_at) as oldest FROM stations",
                null
            )

            cursor.use {
                if (it.moveToFirst()) {
                    val oldestSync = it.getLong(0)
                    if (oldestSync == 0L) return true // No data cached
                    
                    val ageMillis = System.currentTimeMillis() - oldestSync
                    val ageHours = ageMillis / (1000 * 60 * 60)
                    return ageHours >= maxAgeHours
                }
            }
        }
        return true
    }

    /**
     * Get count of cached stations
     */
    fun getCachedStationCount(): Int {
        dbHelper.readableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT COUNT(*) FROM stations", null)
            cursor.use {
                if (it.moveToFirst()) {
                    return it.getInt(0)
                }
            }
        }
        return 0
    }

    /**
     * Clear all cached station data
     */
    fun clearCache() {
        dbHelper.writableDatabase.use { db ->
            db.beginTransaction()
            try {
                db.delete("slots", null, null)
                db.delete("stations", null, null)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    /**
     * Convert database cursor to BackendStationV2
     */
    private fun cursorToStation(cursor: Cursor): BackendStationV2 {
        return BackendStationV2(
            stationId = cursor.getString(cursor.getColumnIndexOrThrow("station_id")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            location = BackendLocation(
                latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                city = cursor.getString(cursor.getColumnIndexOrThrow("city"))
            ),
            type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
            slots = emptyList(), // Will be populated separately
            operatorId = cursor.getString(cursor.getColumnIndexOrThrow("operator_id")),
            isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1,
            totalSlots = cursor.getIntOrNull(cursor.getColumnIndexOrThrow("total_slots")),
            availableSlots = cursor.getIntOrNull(cursor.getColumnIndexOrThrow("available_slots")),
            createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updated_at"))
        )
    }

    /**
     * Convert database cursor to BackendSlot
     */
    private fun cursorToSlot(cursor: Cursor): BackendSlot {
        return BackendSlot(
            slotNumber = cursor.getInt(cursor.getColumnIndexOrThrow("slot_number")),
            connectorType = cursor.getString(cursor.getColumnIndexOrThrow("connector_type")),
            powerRating = cursor.getInt(cursor.getColumnIndexOrThrow("power_rating")),
            isAvailable = cursor.getInt(cursor.getColumnIndexOrThrow("is_available")) == 1
        )
    }

    /**
     * Helper to get nullable int from cursor
     */
    private fun Cursor.getIntOrNull(columnIndex: Int): Int? {
        return if (isNull(columnIndex)) null else getInt(columnIndex)
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     * @return Distance in kilometers
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }
}
