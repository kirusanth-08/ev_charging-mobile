package com.example.evcharger.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.evcharger.model.Reservation
import com.example.evcharger.model.ReservationStatus
import com.example.evcharger.repository.ReservationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Loads dashboard counters and upcoming reservations.
 */
class DashboardViewModel : ViewModel() {
    private val repo = ReservationRepository()

    val pendingCount = MutableLiveData<Int>()
    val approvedFutureCount = MutableLiveData<Int>()
    val upcoming = MutableLiveData<List<Reservation>>()
    val error = MutableLiveData<String?>()

    fun load(nic: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val res = repo.getUpcoming(nic)
            if (res.isSuccessful && res.body()?.data != null) {
                val list = res.body()!!.data!!
                upcoming.postValue(list)
                pendingCount.postValue(list.count { it.status == ReservationStatus.PENDING })
                approvedFutureCount.postValue(list.count { it.status == ReservationStatus.APPROVED })
            } else {
                error.postValue(res.body()?.message ?: "Failed to load upcoming")
            }
        }
    }
}