package com.example.evcharger.model

/**
 * Basic Station model for nearby map display and selection.
 */
data class Station(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)