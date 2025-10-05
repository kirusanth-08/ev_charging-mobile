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

// Unified login request/response for both StationOperator and evOwner
// Backend accepts the same route and returns role-specific payload.
data class LoginRequest(
    // For operator this is username, for owner it can be NIC as username; keep one field
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val role: String,
    val username: String,
    val expiresAt: String
)

data class ConfirmBookingRequest(
    val reservationId: String,
    val operatorId: String
)