package com.example.evcharger.network

import com.example.evcharger.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for C# Web API endpoints.
 * Replace paths to match your backend.
 */
interface ApiService {

    // Unified login for both StationOperator and evOwner
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("booking")
    suspend fun createReservation(@Body body: CreateReservationRequest): Response<ApiResponse<Reservation>>

    // New booking endpoint matching `/api/booking`
    // Body: {"StationId":"ST20251005780","SlotNumber":1,"ReservationDateTime":"2025-10-11T10:00:00Z","Duration":4}
    @POST("booking")
    suspend fun postBooking(@Body body: BookingRequest): Response<ApiResponse<BookingResponseData>>

    @PUT("booking/{id}")
    suspend fun modifyReservation(
        @Path("id") reservationId: String,
        @Body body: ModifyReservationRequest
    ): Response<ApiResponse<Reservation>>

    @DELETE("booking/{id}")
    suspend fun cancelReservation(
        @Path("id") reservationId: String,
        @Body body: CancelReservationRequest
    ): Response<ApiResponse<Unit>>

    // Get booking history for evOwner
    @GET("booking/history")
    suspend fun getHistory(): Response<ApiResponse<List<BookingResponseData>>>

    // Get all pending booking for evOwner
    @GET("booking/upcoming")
    suspend fun getUpcoming(@Query("nic") nic: String): Response<ApiResponse<List<Reservation>>>

    // Get all pending bookings for the authenticated user (requires auth token)
    @GET("booking/pending")
    suspend fun getPending(): Response<ApiResponse<List<BookingResponseData>>>

    // Get pending bookings for a specific station (operator)
    @GET("booking/operator/bookings")
    suspend fun getOperatorBookings(@Query("stationId") stationId: String): Response<ApiResponse<List<BookingResponseData>>>

    // Get pending bookings for operator (optionally filtered by stationId)
    @GET("booking/operator/pending")
    suspend fun getOperatorPending(@Query("stationId") stationId: String? = null): Response<OperatorPendingResponse>

    // Update pending booking (only ReservationDateTime and Duration can be updated)
    @PUT("booking/{bookingId}")
    suspend fun updateBooking(
        @Path("bookingId") bookingId: String,
        @Body body: UpdateBookingRequest
    ): Response<ApiResponse<BookingResponseData>>

    // Cancel/delete a booking
    @DELETE("booking/{bookingId}")
    suspend fun cancelBooking(@Path("bookingId") bookingId: String): Response<ApiResponse<Unit>>

    @GET("station/nearby")
    suspend fun getNearbyStations(
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double,
        @Query("radius") radius: Int = 10
    ): Response<ApiResponse<List<BackendNearbyItem>>>

    // Get all stations
    @GET("station")
    suspend fun getAllStations(): Response<ApiResponse<List<BackendStationV2>>>

    // Get station details by ID
    @GET("station/{stationId}")
    suspend fun getStationDetails(
        @Path("stationId") stationId: String
    ): Response<ApiResponse<BackendStationV2>>

    // Get station availability with slot details
    @GET("station/{stationId}/availability")
    suspend fun getStationAvailability(
        @Path("stationId") stationId: String
    ): Response<ApiResponse<StationAvailabilityData>>

    // Get stations assigned to the currently-authenticated operator (JWT required)
    @GET("station/operator/stations")
    suspend fun getOperatorStations(): Response<ApiResponse<List<BackendStationV2>>>

    // Update a slot's availability for a station (operator only)
    @PATCH("station/{stationId}/slots/{slotNumber}/availability")
    suspend fun updateSlotAvailability(
        @Path("stationId") stationId: String,
        @Path("slotNumber") slotNumber: Int,
        @Body body: Map<String, Boolean>
    ): Response<ApiResponse<Unit>>

    // Fetch reservation by QR payload (used by scanner/lookup). Backend expects query param 'payload'
    // @GET("booking/confirm-arrival")
    // suspend fun getReservationByQr(@Query("payload") payload: String): Response<ApiResponse<Reservation>>

    // Station operator scans QR and confirms arrival: POST /api/booking/confirm-arrival
    @POST("booking/confirm-arrival")
    suspend fun confirmArrival(@Body body: com.example.evcharger.model.ConfirmArrivalRequest): Response<ApiResponse<Reservation>>

    // Complete booking after charging: PATCH /api/booking/{bookingId}/complete
    @PATCH("booking/{bookingId}/complete")
    suspend fun completeBooking(
        @Path("bookingId") bookingId: String,
        @Body body: com.example.evcharger.model.CompleteBookingRequest
    ): Response<ApiResponse<BookingResponseData>>

    // Confirm booking with reservation ID and operator ID
    @PATCH("booking/{id}/approve")
    suspend fun confirmBooking(@Body body: ConfirmBookingRequest): Response<ApiResponse<Reservation>>

    // Approve booking (for station operators)
    @PATCH("booking/{bookingId}/approve")
    suspend fun approveBooking(@Path("bookingId") bookingId: String): Response<ApiResponse<BookingResponseData>>

    @POST("evowner/register")
    suspend fun registerEvOwner(@Body body: EvOwnerRegisterRequest): Response<ApiResponse<EvOwnerRegisterResponse>>

    // Get EV Owner profile by NIC (authenticated with bearer token)
    @GET("evowner/{nic}")
    suspend fun getEvOwnerProfile(@Path("nic") nic: String): Response<ApiResponse<EvOwnerProfile>>

    // Update EV Owner profile (authenticated with bearer token)
    @PUT("evowner/{nic}")
    suspend fun updateEvOwnerProfile(
        @Path("nic") nic: String,
        @Body body: EvOwnerUpdateRequest
    ): Response<ApiResponse<EvOwnerProfile>>

    // Deactivate EV Owner account (authenticated with bearer token)
    @PATCH("evowner/{nic}/deactivate")
    suspend fun deactivateEvOwnerAccount(@Path("nic") nic: String): Response<ApiResponse<Unit>>
}
