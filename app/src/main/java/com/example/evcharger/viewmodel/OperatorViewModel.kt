package com.example.evcharger.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.example.evcharger.model.LoginRequest
import com.example.evcharger.model.Reservation
import com.example.evcharger.network.RetrofitClient
import com.example.evcharger.repository.ReservationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles operator login, scan-lookup, and confirm flows.
 */
class OperatorViewModel : ViewModel() {
    private val repo = ReservationRepository()

    val operatorToken = MutableLiveData<String?>()
    val scannedReservation = MutableLiveData<Reservation?>()
    val role = MutableLiveData<String?>()
    val error = MutableLiveData<String?>()
    val operatorUsername = MutableLiveData<String?>()
    val loading = MutableLiveData(false)

    fun login(username: String, password: String) {
        loading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = RetrofitClient.api.login(LoginRequest(username, password))
                if (res.isSuccessful && res.body()?.success == true) {
                    val data = res.body()!!.data!!
                    val token = data.token
                    RetrofitClient.setAuthToken(token)
                    operatorToken.postValue(token)
                    role.postValue(data.role)
                    operatorUsername.postValue(data.username)
                    // Session persistence should be handled by the Activity (has Context)
                } else error.postValue(res.body()?.message ?: "Login failed")
            } catch (e: Exception) {
                error.postValue(e.localizedMessage ?: "Network error")
            } finally {
                loading.postValue(false)
            }
        }
    }

    fun lookupByQr(payload: String) {
        loading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = repo.byQr(payload)
                if (res.isSuccessful && res.body()?.data != null) {
                    scannedReservation.postValue(res.body()!!.data!!)
                } else error.postValue(res.body()?.message ?: "Not found")
            } catch (e: Exception) {
                error.postValue(e.localizedMessage ?: "Network error")
            } finally {
                loading.postValue(false)
            }
        }
    }

    fun confirm(reservationId: String, operatorId: String) {
        loading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = repo.confirm(reservationId, operatorId)
                if (res.isSuccessful && res.body()?.data != null) {
                    scannedReservation.postValue(res.body()!!.data!!)
                } else error.postValue(res.body()?.message ?: "Confirm failed")
            } catch (e: Exception) {
                error.postValue(e.localizedMessage ?: "Network error")
            } finally {
                loading.postValue(false)
            }
        }
    }

    /**
     * Confirm arrival by QR payload. Station operator scans a QR and posts the QR string
     * to the backend which marks the reservation as arrived.
     */
    fun confirmArrivalByQr(qrCode: String) {
        loading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = repo.confirmArrival(qrCode)
                if (res.isSuccessful && res.body()?.data != null) {
                    scannedReservation.postValue(res.body()!!.data!!)
                } else error.postValue(res.body()?.message ?: "Confirm arrival failed")
            } catch (e: Exception) {
                error.postValue(e.localizedMessage ?: "Network error")
            } finally {
                loading.postValue(false)
            }
        }
    }
}