package com.example.evcharger.repository

import com.example.evcharger.model.*
import com.example.evcharger.network.RetrofitClient
import com.example.evcharger.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

/**
 * Repository for interacting with server-side reservation endpoints.
 * Enforces local 12h modification/cancellation rule before API call.
 */
class ReservationRepository {

    fun getUpcoming(nic: String): Flow<Result<List<Reservation>>> = flow {
        try {
            val res = RetrofitClient.api.getUserReservations(nic)
            if (res.isSuccessful && res.body()?.success == true) {
                emit(Result.success(res.body()!!.data!!))
            } else {
                emit(Result.failure(Exception(res.body()?.message ?: "Failed to load reservations")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun createReservation(nic: String, stationId: String, start: LocalDateTime): Flow<Result<Reservation>> = flow {
        try {
            val res = RetrofitClient.api.createReservation(
                CreateReservationRequest(nic, stationId, TimeUtils.toIso(start))
            )
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                emit(Result.success(res.body()!!.data!!))
            } else {
                emit(Result.failure(Exception(res.body()?.message ?: "Failed to create")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
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

    suspend fun getHistory(nic: String) = RetrofitClient.api.getHistory(nic)
    suspend fun getNearby(lat: Double, lng: Double) = RetrofitClient.api.getNearbyStations(lat, lng)
    suspend fun byQr(payload: String) = RetrofitClient.api.getReservationByQr(payload)
    suspend fun confirm(reservationId: String, operatorId: String) =
        RetrofitClient.api.confirmBooking(ConfirmBookingRequest(reservationId, operatorId))
}