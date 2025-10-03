package com.example.evcharger.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.evcharger.model.OperatorLoginRequest
import com.example.evcharger.model.Reservation
import com.example.evcharger.network.RetrofitClient
import com.example.evcharger.repository.ReservationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles operator login, scan-lookup, and confirm flows.
 */
class OperatorViewModel : ViewModel() {
    private val repo = ReservationRepository()

    val operatorToken = MutableLiveData<String?>()
    val scannedReservation = MutableLiveData<Reservation?>()
    val error = MutableLiveData<String?>()

    fun login(username: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val res = RetrofitClient.api.operatorLogin(OperatorLoginRequest(username, password))
            if (res.isSuccessful && res.body()?.success == true) {
                val token = res.body()!!.data!!.token
                RetrofitClient.setAuthToken(token)
                operatorToken.postValue(token)
            } else error.postValue(res.body()?.message ?: "Login failed")
        }
    }

    fun lookupByQr(payload: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val res = repo.byQr(payload)
            if (res.isSuccessful && res.body()?.data != null) {
                scannedReservation.postValue(res.body()!!.data!!)
            } else error.postValue(res.body()?.message ?: "Not found")
        }
    }

    fun confirm(reservationId: String, operatorId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val res = repo.confirm(reservationId, operatorId)
            if (res.isSuccessful && res.body()?.data != null) {
                scannedReservation.postValue(res.body()!!.data!!)
            } else error.postValue(res.body()?.message ?: "Confirm failed")
        }
    }
}