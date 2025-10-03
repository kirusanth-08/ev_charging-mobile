package com.example.evcharger.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.evcharger.model.Reservation
import com.example.evcharger.repository.ReservationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Manages reservation CRUD with 12h rule validations done in repository.
 */
class ReservationViewModel : ViewModel() {
    private val repo = ReservationRepository()

    val result = MutableLiveData<Reservation?>()
    val error = MutableLiveData<String?>()

    fun create(nic: String, stationId: String, start: LocalDateTime) {
        CoroutineScope(Dispatchers.IO).launch {
            repo.createReservation(nic, stationId, start)
                .onSuccess { result.postValue(it) }
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
}