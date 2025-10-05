package com.example.evcharger.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Station model matching server response for nearby endpoint.
 */
@Parcelize
data class Station(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val connectorTypes: List<String> = emptyList(),
    val chargingPowerKw: Int? = null,
    val status: String? = null,
    val lastUpdated: String? = null,
    val distanceMeters: Int? = null
) : Parcelable