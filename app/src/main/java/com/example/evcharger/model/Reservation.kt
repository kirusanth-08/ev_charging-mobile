package com.example.evcharger.model

import java.time.LocalDateTime

/**
 * Reservation data class.
 * Most fields are synchronized with server; id provided by backend.
 */
data class Reservation(
    val id: String? = null,
    val ownerNic: String,
    val stationId: String,
    val stationName: String? = null,
    val startTime: LocalDateTime,
    val status: ReservationStatus = ReservationStatus.PENDING,
    val qrCodePayload: String? = null
)

enum class ReservationStatus { PENDING, APPROVED, CANCELLED, COMPLETED }