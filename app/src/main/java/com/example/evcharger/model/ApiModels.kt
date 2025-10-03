package com.example.evcharger.model

/**
 * DTOs for interacting with the C# Web API via Retrofit.
 * Adjust fields to match your backend contract.
 */

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

data class CreateReservationRequest(
    val nic: String,
    val stationId: String,
    val startTimeIso: String
)

data class ModifyReservationRequest(
    val reservationId: String,
    val newStartTimeIso: String
)

data class CancelReservationRequest(
    val reservationId: String
)

data class OperatorLoginRequest(
    val username: String,
    val password: String
)

data class OperatorLoginResponse(
    val token: String,
    val operatorId: String
)

data class ConfirmBookingRequest(
    val reservationId: String,
    val operatorId: String
)