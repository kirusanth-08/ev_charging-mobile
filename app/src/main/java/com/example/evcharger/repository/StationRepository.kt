package com.example.evcharger.repository

import com.example.evcharger.network.RetrofitClient

class StationRepository {
    suspend fun getOperatorStations() = RetrofitClient.api.getOperatorStations()

    suspend fun updateSlotAvailability(stationId: String, slotNumber: Int, isAvailable: Boolean) =
        RetrofitClient.api.updateSlotAvailability(stationId, slotNumber, mapOf("IsAvailable" to isAvailable))
}
