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

    @POST("reservations")
    suspend fun createReservation(@Body body: CreateReservationRequest): Response<ApiResponse<Reservation>>

    @PUT("reservations/{id}")
    suspend fun modifyReservation(
        @Path("id") reservationId: String,
        @Body body: ModifyReservationRequest
    ): Response<ApiResponse<Reservation>>

    @HTTP(method = "DELETE", path = "reservations/{id}", hasBody = true)
    suspend fun cancelReservation(
        @Path("id") reservationId: String,
        @Body body: CancelReservationRequest
    ): Response<ApiResponse<Unit>>

    @GET("reservations/history")
    suspend fun getHistory(@Query("nic") nic: String): Response<ApiResponse<List<Reservation>>>

    @GET("reservations/upcoming")
    suspend fun getUpcoming(@Query("nic") nic: String): Response<ApiResponse<List<Reservation>>>

    // Backend endpoint expects: /api/station/nearby?latitude=...&longitude=...&radius=...
    @GET("station/nearby")
    suspend fun getNearbyStations(
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double,
        // default radius is 10 (units depend on your backend; commonly kilometers)
        @Query("radius") radius: Int = 10
    ): Response<ApiResponse<List<Station>>>

    @GET("reservations/by-qr")
    suspend fun getReservationByQr(@Query("payload") payload: String): Response<ApiResponse<Reservation>>

    @POST("reservations/confirm")
    suspend fun confirmBooking(@Body body: ConfirmBookingRequest): Response<ApiResponse<Reservation>>

    @POST("evowner/register")
    suspend fun registerEvOwner(@Body body: EvOwnerRegisterRequest): Response<ApiResponse<EvOwnerRegisterResponse>>
}