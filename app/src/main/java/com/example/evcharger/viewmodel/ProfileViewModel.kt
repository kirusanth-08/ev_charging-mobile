package com.example.evcharger.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.evcharger.model.EvOwnerProfile
import com.example.evcharger.repository.ProfileRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing EV Owner profile operations.
 * Handles profile retrieval and updates.
 */
class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ProfileRepository()

    // Profile data
    private val _profile = MutableLiveData<EvOwnerProfile?>()
    val profile: LiveData<EvOwnerProfile?> get() = _profile

    // Loading state
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    // Error messages
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    // Success state
    private val _success = MutableLiveData<Boolean>()
    val success: LiveData<Boolean> get() = _success

    /**
     * Load profile by NIC
     */
    fun loadProfile(nic: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            try {
                val response = repository.getProfile(nic)
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true && apiResponse.data != null) {
                        _profile.value = apiResponse.data
                    } else {
                        _error.value = apiResponse?.message ?: "Failed to load profile"
                    }
                } else {
                    when (response.code()) {
                        401 -> _error.value = "Unauthorized. Please login again."
                        404 -> _error.value = "Profile not found"
                        else -> _error.value = "Error: ${response.code()} - ${response.message()}"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Update profile information
     */
    fun updateProfile(
        nic: String,
        fullName: String,
        email: String,
        phoneNumber: String,
        password: String? = null
    ) {
        // Validation
        if (nic.isBlank()) {
            _error.value = "NIC is required"
            return
        }
        if (fullName.isBlank()) {
            _error.value = "Full name is required"
            return
        }
        if (email.isBlank()) {
            _error.value = "Email is required"
            return
        }
        if (phoneNumber.isBlank()) {
            _error.value = "Phone number is required"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _success.value = false
            
            try {
                val response = repository.updateProfile(
                    nic = nic,
                    fullName = fullName,
                    email = email,
                    phoneNumber = phoneNumber,
                    password = password
                )
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true && apiResponse.data != null) {
                        _profile.value = apiResponse.data
                        _success.value = true
                    } else {
                        _error.value = apiResponse?.message ?: "Failed to update profile"
                    }
                } else {
                    when (response.code()) {
                        401 -> _error.value = "Unauthorized. Please login again."
                        404 -> _error.value = "Profile not found"
                        400 -> _error.value = "Invalid data provided"
                        else -> _error.value = "Error: ${response.code()} - ${response.message()}"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clear success state
     */
    fun clearSuccess() {
        _success.value = false
    }
}
