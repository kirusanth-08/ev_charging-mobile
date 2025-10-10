package com.example.evcharger.repository

import com.example.evcharger.model.*
import com.example.evcharger.network.RetrofitClient
import com.example.evcharger.util.TimeUtils
import java.time.LocalDateTime

/**
 * Repository for interacting with server-side reservation endpoints.
 * Enforces local 12h modification/cancellation rule before API call.
 */
class ReservationRepository {

    suspend fun createReservation(nic: String, stationId: String, start: LocalDateTime): Result<Reservation> {
        val res = RetrofitClient.api.createReservation(
            CreateReservationRequest(nic, stationId, TimeUtils.toIso(start))
        )
        return if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
            Result.success(res.body()!!.data!!)
        } else Result.failure(Exception(res.body()?.message ?: "Failed to create"))
    }

    suspend fun modifyReservation(reservationId: String, newStart: LocalDateTime, currentStart: LocalDateTime): Result<Reservation> {
        // 12h rule: changes must be done at least 12h before original start
        if (!TimeUtils.canModifyOrCancel(currentStart)) {
            return Result.failure(IllegalStateException("Cannot modify within 12 hours of start time"))
        }
        val res = RetrofitClient.api.modifyReservation(
            reservationId,
            ModifyReservationRequest(reservationId, TimeUtils.toIso(newStart))
        )
        return if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
            Result.success(res.body()!!.data!!)
        } else Result.failure(Exception(res.body()?.message ?: "Failed to modify"))
    }

    suspend fun cancelReservation(reservationId: String, startTime: LocalDateTime): Result<Unit> {
        if (!TimeUtils.canModifyOrCancel(startTime)) {
            return Result.failure(IllegalStateException("Cannot cancel within 12 hours of start time"))
        }
        val res = RetrofitClient.api.cancelReservation(
            reservationId,
            CancelReservationRequest(reservationId)
        )
        return if (res.isSuccessful && res.body()?.success == true) {
            Result.success(Unit)
        } else Result.failure(Exception(res.body()?.message ?: "Failed to cancel"))
    }

    suspend fun getUpcoming(nic: String) = RetrofitClient.api.getUpcoming(nic)
    suspend fun getHistory(nic: String) = RetrofitClient.api.getHistory(nic)
    suspend fun getNearby(lat: Double, lng: Double) = RetrofitClient.api.getNearbyStations(lat, lng)
    // Note: backend does not provide a GET reservation-by-QR endpoint.
    // Confirmation of arrival is done via POST /booking/confirm-arrival with { "QrCode": "..." }.
    suspend fun getPending() = RetrofitClient.api.getPending()

    suspend fun confirm(reservationId: String, operatorId: String) =
        RetrofitClient.api.confirmBooking(ConfirmBookingRequest(reservationId, operatorId))

    /**
     * Create a new booking using the new booking endpoint
     * Body format: {"StationId":"ST20251005780","SlotNumber":1,"ReservationDateTime":"2025-10-11T10:00:00Z","Duration":4}
     * @param stationId The ID of the charging station
     * @param slotNumber The slot number to book (1-based)
     * @param reservationDateTime ISO 8601 datetime string (e.g., "2025-10-11T10:00:00Z")
     * @param duration Duration in hours (e.g., 4)
     * @return ApiResponse containing BookingResponseData with QR code and booking details
     */
    suspend fun createBooking(
        stationId: String,
        slotNumber: Int,
        reservationDateTime: String,
        duration: Int
    ): Result<BookingResponseData> {
        val request = BookingRequest(stationId, slotNumber, reservationDateTime, duration)
        val res = RetrofitClient.api.postBooking(request)
        return if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
            Result.success(res.body()!!.data!!)
        } else {
            Result.failure(Exception(res.body()?.message ?: "Failed to create booking"))
        }
    }

    // Station operator confirms arrival by scanning QR and posting { "QrCode": "..." }
    suspend fun confirmArrival(qrCode: String) =
        RetrofitClient.api.confirmArrival(com.example.evcharger.model.ConfirmArrivalRequest(qrCode))
}