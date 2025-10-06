package com.example.evcharger.repository

import com.example.evcharger.network.RetrofitClient

class StationRepository {
    suspend fun getOperatorStations() = RetrofitClient.api.getOperatorStations()
}
