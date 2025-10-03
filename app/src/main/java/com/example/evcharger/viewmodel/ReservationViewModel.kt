package com.example.evcharger.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.evcharger.model.Reservation
import com.example.evcharger.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.LocalDateTime

/**
 * Manages reservation CRUD with 12h rule validations done in repository.
 */
class ReservationViewModel : BaseViewModel() {
    private val repo = ReservationRepository()

    private val _reservation = MutableStateFlow<Reservation?>(null)
    val reservation: StateFlow<Reservation?> = _reservation.asStateFlow()

    fun create(nic: String, stationId: String, start: LocalDateTime) {
        launchWithLoading {
            repo.createReservation(nic, stationId, start)
                .onEach { result ->
                    result.fold(
                        onSuccess = { _reservation.value = it },
                        onFailure = { throw it }
                    )
                }.launchIn(viewModelScope)
        }
    }

    fun modify(resId: String, newStart: LocalDateTime, currentStart: LocalDateTime) {
        launchWithLoading {
            repo.modifyReservation(resId, newStart, currentStart)
                .onEach { result ->
                    result.fold(
                        onSuccess = { _reservation.value = it },
                        onFailure = { throw it }
                    )
                }.launchIn(viewModelScope)
        }
    }

    fun cancel(resId: String, start: LocalDateTime) {
        launchWithLoading {
            repo.cancelReservation(resId, start)
                .onEach { result ->
                    result.fold(
                        onSuccess = { _reservation.value = it },
                        onFailure = { throw it }
                    )
                }.launchIn(viewModelScope)
        }
    }
}