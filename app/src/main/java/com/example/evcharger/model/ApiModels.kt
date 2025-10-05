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

// EV Owner registration request matching provided JSON body
data class EvOwnerRegisterRequest(
    val NIC: String,
    val FullName: String,
    val Email: String,
    val PhoneNumber: String,
    val Password: String
)

data class EvOwnerRegisterResponse(
    val nic: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val isActive: Boolean,
    val createdAt: String
)

// Backend DTOs for nearby station endpoint
// Previous BackendStation used PascalCase names. New API returns a wrapper with different shape.
data class BackendLocation(
    val address: String?,
    val city: String?,
    val latitude: Double,
    val longitude: Double
)

data class BackendSlot(
    val slotNumber: Int,
    val isAvailable: Boolean = true,
    val powerRating: Int,
    val connectorType: String
)

data class BackendStationV2(
    val stationId: String,
    val name: String,
    val location: BackendLocation,
    val type: String?,
    val slots: List<BackendSlot> = emptyList(),
    val operatorId: String? = null,
    val isActive: Boolean = true,
    val totalSlots: Int? = null,
    val availableSlots: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class BackendNearbyItem(
    val station: BackendStationV2,
    val distanceKm: Double?
)