package com.example.evcharger.model

import com.google.gson.annotations.SerializedName

/**
 * Reservation data class. Fields are nullable to tolerate differences between API versions.
 */
data class Reservation(
    // common identifiers
    val id: String? = null,
    @SerializedName("bookingId") val bookingId: String? = null,

    // owner
    @SerializedName("evOwnerNic") val evOwnerNic: String? = null,
    val ownerNic: String? = null,

    // station
    val stationId: String,
    val stationName: String? = null,
    val stationLocation: String? = null,

    // timing fields (ISO strings)
    @SerializedName("reservationDateTime") val reservationDateTime: String? = null,
    val startTime: String? = null,

    // booking details
    val slotNumber: Int? = null,
    val duration: Int? = null,

    // status and related metadata
    val status: ReservationStatus = ReservationStatus.PENDING,
    val approvedBy: String? = null,
    val approvedAt: String? = null,

    // QR / tokens
    @SerializedName("qrCode") val qrCode: String? = null,
    val qrCodePayload: String? = null,

    // UI helpers
    val timeUntilReservation: String? = null,
    val isExpired: Boolean? = null
)

enum class ReservationStatus { PENDING, APPROVED, CANCELLED, COMPLETED }