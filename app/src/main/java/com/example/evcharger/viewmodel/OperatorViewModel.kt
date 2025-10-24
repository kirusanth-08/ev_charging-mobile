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
    val inProgressBookings = MutableLiveData<List<com.example.evcharger.model.BookingResponseData>>()
    val role = MutableLiveData<String?>()
    val error = MutableLiveData<String?>()
    val operatorUsername = MutableLiveData<String?>()
    val loading = MutableLiveData(false)
    val bookingCompleted = MutableLiveData<Boolean>()

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

    // Removed lookupByQr: backend does not expose a GET reservation-by-QR endpoint.
    // Operators should POST the scanned QR to confirm arrival (confirmArrivalByQr / confirmArrival).

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
                } else {
                    // Try to get message from response body first
                    val errorMessage = res.body()?.message 
                    
                    // If body is null (e.g., 4xx/5xx errors), try parsing error body
                    if (errorMessage == null && res.errorBody() != null) {
                        try {
                            val errorJson = res.errorBody()?.string()
                            val gson = com.google.gson.Gson()
                            
                            // Parse as generic ApiResponse with Any type
                            val errorResponse = gson.fromJson(errorJson, 
                                object : com.google.gson.reflect.TypeToken<com.example.evcharger.model.ApiResponse<Any>>() {}.type
                            ) as? com.example.evcharger.model.ApiResponse<*>
                            
                            error.postValue(errorResponse?.message ?: "Confirm arrival failed")
                        } catch (e: Exception) {
                            error.postValue("Confirm arrival failed")
                        }
                    } else {
                        error.postValue(errorMessage ?: "Confirm arrival failed")
                    }
                }
            } catch (e: Exception) {
                error.postValue(e.localizedMessage ?: "Network error")
            } finally {
                loading.postValue(false)
            }
        }
    }

    // Alias to match naming in other parts of the app: calls the same repository method
    fun confirmArrival(qrCode: String) {
        confirmArrivalByQr(qrCode)
    }

    /**
     * Fetch in-progress bookings (status = "InProgress" or "Arrived")
     * These are bookings that have been confirmed and are currently charging
     */
    fun fetchInProgressBookings(stationId: String? = null) {
        loading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Use the operator pending endpoint to get all bookings, then filter
                val res = repo.getOperatorPendingBookings(stationId)
                if (res.isSuccessful && res.body()?.bookings != null) {
                    // Filter for in-progress bookings (status = "InProgress" or "Arrived" or "Confirmed")
                    val inProgress = res.body()!!.bookings!!.filter { booking ->
                        booking.status?.equals("InProgress", ignoreCase = true) == true ||
                        booking.status?.equals("Arrived", ignoreCase = true) == true ||
                        booking.status?.equals("Confirmed", ignoreCase = true) == true
                    }
                    inProgressBookings.postValue(inProgress)
                } else {
                    error.postValue(res.body()?.message ?: "Failed to fetch bookings")
                }
            } catch (e: Exception) {
                error.postValue(e.localizedMessage ?: "Network error")
            } finally {
                loading.postValue(false)
            }
        }
    }

    /**
     * Complete a booking after charging is done
     * @param bookingId The booking ID to complete
     * @param energyConsumed Energy consumed in kWh
     * @param cost Total cost in currency units
     */
    fun completeBooking(bookingId: String, energyConsumed: Double, cost: Double) {
        loading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = repo.completeBooking(bookingId, energyConsumed, cost)
                if (res.isSuccessful && res.body()?.success == true) {
                    bookingCompleted.postValue(true)
                    // Refresh the in-progress list
                    fetchInProgressBookings()
                } else {
                    // Try to get message from response body first
                    val errorMessage = res.body()?.message
                    
                    // If body is null (e.g., 4xx/5xx errors), try parsing error body
                    if (errorMessage == null && res.errorBody() != null) {
                        try {
                            val errorJson = res.errorBody()?.string()
                            val gson = com.google.gson.Gson()
                            
                            val errorResponse = gson.fromJson(errorJson,
                                object : com.google.gson.reflect.TypeToken<com.example.evcharger.model.ApiResponse<Any>>() {}.type
                            ) as? com.example.evcharger.model.ApiResponse<*>
                            
                            error.postValue(errorResponse?.message ?: "Failed to complete booking")
                        } catch (e: Exception) {
                            error.postValue("Failed to complete booking")
                        }
                    } else {
                        error.postValue(errorMessage ?: "Failed to complete booking")
                    }
                }
            } catch (e: Exception) {
                error.postValue(e.localizedMessage ?: "Network error")
            } finally {
                loading.postValue(false)
            }
        }
    }
}