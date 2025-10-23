package com.example.evcharger.repository

import android.content.Context
import com.example.evcharger.model.*
import com.example.evcharger.network.RetrofitClient
import com.example.evcharger.util.TimeUtils
import retrofit2.Response
import java.time.LocalDateTime

/**
 * Repository for interacting with server-side reservation endpoints.
 * Enforces local 12h modification/cancellation rule before API call.
 * Supports offline caching for station data.
 */
class ReservationRepository(private val context: Context? = null) {

    private val stationCache: StationCacheRepository? by lazy {
        context?.let { StationCacheRepository(it) }
    }

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
    suspend fun getHistory() = RetrofitClient.api.getHistory()
    
    /**
     * Get nearby stations with offline support
     * Tries API first, caches result, falls back to cache if offline
     */
    suspend fun getNearby(lat: Double, lng: Double): Response<ApiResponse<List<BackendNearbyItem>>> {
        return try {
            // Try to fetch from API
            val response = RetrofitClient.api.getNearbyStations(lat, lng)
            
            // If successful, cache the stations
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let { nearbyItems ->
                    stationCache?.cacheStations(nearbyItems.map { it.station })
                }
            }
            
            response
        } catch (e: Exception) {
            // If offline or error, try to get from cache
            stationCache?.let { cache ->
                val cachedStations = cache.getNearbyStations(lat, lng)
                if (cachedStations.isNotEmpty()) {
                    // Convert cached stations to NearbyItems with null distance (offline mode)
                    val nearbyItems = cachedStations.map { station ->
                        BackendNearbyItem(
                            station = station,
                            distanceKm = null // Distance calculation requires location, not available offline
                        )
                    }
                    // Return cached data as successful response
                    return Response.success(
                        ApiResponse(
                            success = true,
                            message = "Loaded from offline cache",
                            data = nearbyItems
                        )
                    )
                }
            }
            // Re-throw if no cache available
            throw e
        }
    }
    
    /**
     * Get station details with offline support
     */
    suspend fun getStationDetails(stationId: String): Response<ApiResponse<BackendStationV2>> {
        return try {
            // Try to fetch from API
            val response = RetrofitClient.api.getStationDetails(stationId)
            
            // If successful, cache the station
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let { station ->
                    stationCache?.cacheStation(station)
                }
            }
            
            response
        } catch (e: Exception) {
            // If offline or error, try to get from cache
            stationCache?.let { cache ->
                val cachedStation = cache.getStation(stationId)
                if (cachedStation != null) {
                    // Return cached data as successful response
                    return Response.success(
                        ApiResponse(
                            success = true,
                            message = "Loaded from offline cache",
                            data = cachedStation
                        )
                    )
                }
            }
            // Re-throw if no cache available
            throw e
        }
    }
    
    // Note: backend does not provide a GET reservation-by-QR endpoint.
    // Confirmation of arrival is done via POST /booking/confirm-arrival with { "QrCode": "..." }.
    suspend fun getPending() = RetrofitClient.api.getPending()

    /**
     * Get pending bookings for a specific station (operator)
     * @param stationId The station ID to filter bookings (e.g., "ST20251001123")
     * @return Response containing list of BookingResponseData
     */
    suspend fun getOperatorBookings(stationId: String) = RetrofitClient.api.getOperatorBookings(stationId)

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

    /**
     * Update a pending booking (only ReservationDateTime and Duration can be updated)
     * @param bookingId The booking ID (e.g., "BK202510221834388225")
     * @param reservationDateTime New ISO 8601 datetime string (e.g., "2025-10-26T10:00:00Z")
     * @param duration New duration in hours
     * @return Result containing updated BookingResponseData
     */
    suspend fun updateBooking(
        bookingId: String,
        reservationDateTime: String,
        duration: Int
    ): Result<BookingResponseData> {
        val request = UpdateBookingRequest(reservationDateTime, duration)
        val res = RetrofitClient.api.updateBooking(bookingId, request)
        return if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
            Result.success(res.body()!!.data!!)
        } else {
            Result.failure(Exception(res.body()?.message ?: "Failed to update booking"))
        }
    }

    /**
     * Cancel/delete a booking
     * @param bookingId The booking ID (e.g., "BK202510021000001234")
     * @return Result indicating success or failure
     */
    suspend fun cancelBooking(bookingId: String): Result<Unit> {
        val res = RetrofitClient.api.cancelBooking(bookingId)
        return if (res.isSuccessful && res.body()?.success == true) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(res.body()?.message ?: "Failed to cancel booking"))
        }
    }
}
