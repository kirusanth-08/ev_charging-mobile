package com.example.evcharger.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.evcharger.model.BookingResponseData
import com.example.evcharger.model.Reservation
import com.example.evcharger.repository.ReservationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Manages reservation CRUD with 12h rule validations done in repository.
 */
class ReservationViewModel : ViewModel() {
    private val repo = ReservationRepository()

    val result = MutableLiveData<Reservation?>()
    val bookingResult = MutableLiveData<BookingResponseData?>()
    val operatorBookings = MutableLiveData<List<BookingResponseData>?>()
    val error = MutableLiveData<String?>()

    fun create(nic: String, stationId: String, start: LocalDateTime) {
        CoroutineScope(Dispatchers.IO).launch {
            repo.createReservation(nic, stationId, start)
                .onSuccess { result.postValue(it) }
                .onFailure { error.postValue(it.message) }
        }
    }

    /**
     * Create booking using new API format
     * @param stationId The station ID (e.g., "ST20251005780")
     * @param slotNumber The slot number (1-based)
     * @param reservationDateTime ISO 8601 formatted datetime
     * @param duration Duration in hours
     */
    fun createBooking(stationId: String, slotNumber: Int, reservationDateTime: String, duration: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            repo.createBooking(stationId, slotNumber, reservationDateTime, duration)
                .onSuccess { bookingResult.postValue(it) }
                .onFailure { error.postValue(it.message) }
        }
    }

    fun modify(resId: String, newStart: LocalDateTime, currentStart: LocalDateTime) {
        CoroutineScope(Dispatchers.IO).launch {
            repo.modifyReservation(resId, newStart, currentStart)
                .onSuccess { result.postValue(it) }
                .onFailure { error.postValue(it.message) }
        }
    }

    fun cancel(resId: String, start: LocalDateTime) {
        CoroutineScope(Dispatchers.IO).launch {
            repo.cancelReservation(resId, start)
                .onSuccess { result.postValue(null) }
                .onFailure { error.postValue(it.message) }
        }
    }

    /**
     * Get pending bookings for a specific station (operator)
     * @param stationId The station ID to filter bookings (e.g., "ST20251001123")
     */
    fun getOperatorBookings(stationId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = repo.getOperatorBookings(stationId)
                if (response.isSuccessful && response.body()?.success == true) {
                    operatorBookings.postValue(response.body()?.data)
                } else {
                    error.postValue(response.body()?.message ?: "Failed to fetch bookings")
                }
            } catch (e: Exception) {
                error.postValue(e.message ?: "Network error")
            }
        }
    }
}