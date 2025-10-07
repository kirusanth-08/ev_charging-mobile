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

    // New booking endpoint matching `/api/booking` that accepts PascalCase property names
//    @POST("booking")
//    suspend fun postBooking(@Body body: com.example.evcharger.model.BookingRequest): Response<com.example.evcharger.model.ApiResponse<com.example.evcharger.model.BookingResponseData>>

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

    @GET("booking/history")
    suspend fun getHistory(@Query("nic") nic: String): Response<ApiResponse<List<Reservation>>>

    @GET("booking/upcoming")
    suspend fun getUpcoming(@Query("nic") nic: String): Response<ApiResponse<List<Reservation>>>

    @GET("booking/pending")
    suspend fun getPending(): Response<ApiResponse<List<Reservation>>>

    @GET("station/nearby")
    suspend fun getNearbyStations(
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double,
        @Query("radius") radius: Int = 10
    ): Response<ApiResponse<List<BackendNearbyItem>>>

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

    @PATCH("booking/{id}/approve")
    suspend fun confirmBooking(@Body body: ConfirmBookingRequest): Response<ApiResponse<Reservation>>

    @POST("evowner/register")
    suspend fun registerEvOwner(@Body body: EvOwnerRegisterRequest): Response<ApiResponse<EvOwnerRegisterResponse>>
}