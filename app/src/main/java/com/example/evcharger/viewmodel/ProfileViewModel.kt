package com.example.evcharger.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.evcharger.model.EvOwnerProfile
import com.example.evcharger.repository.ProfileRepository
import com.example.evcharger.repository.OfflineDataRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing EV Owner profile operations.
 * Handles profile retrieval and updates with offline caching support.
 * 
 * Caching Strategy:
 * 1. Check local cache first
 * 2. If cached data exists, display it immediately
 * 3. Fetch fresh data from API in background
 * 4. Update cache and UI with fresh data if API succeeds
 * 5. If API fails but cache exists, keep showing cached data
 */
class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ProfileRepository()
    private val offlineRepository = OfflineDataRepository(app.applicationContext)

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
    
    // Deactivation success state
    private val _deactivationSuccess = MutableLiveData<Boolean>()
    val deactivationSuccess: LiveData<Boolean> get() = _deactivationSuccess
    
    // Indicates if data is from cache (for offline indicator)
    private val _isDataFromCache = MutableLiveData<Boolean>()
    val isDataFromCache: LiveData<Boolean> get() = _isDataFromCache

    /**
     * Load profile by NIC with offline caching support
     * Strategy: Cache-first, then network update
     */
    fun loadProfile(nic: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _isDataFromCache.value = false
            
            // Step 1: Try to load from cache first
            val cachedProfile = offlineRepository.getCachedUserProfile(nic)
            if (cachedProfile != null) {
                // Display cached data immediately
                _profile.value = cachedProfile
                _isDataFromCache.value = true
                _loading.value = false
            }
            
            // Step 2: Fetch fresh data from API (regardless of cache)
            try {
                val response = repository.getProfile(nic)
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true && apiResponse.data != null) {
                        // Update cache with fresh data
                        offlineRepository.cacheUserProfile(apiResponse.data)
                        
                        // Update UI with fresh data
                        _profile.value = apiResponse.data
                        _isDataFromCache.value = false
                    } else {
                        // API returned error - if we have cache, keep it, otherwise show error
                        if (cachedProfile == null) {
                            _error.value = apiResponse?.message ?: "Failed to load profile"
                        }
                    }
                } else {
                    // HTTP error - if we have cache, keep it, otherwise show error
                    if (cachedProfile == null) {
                        when (response.code()) {
                            401 -> _error.value = "Unauthorized. Please login again."
                            404 -> _error.value = "Profile not found"
                            else -> _error.value = "Error: ${response.code()} - ${response.message()}"
                        }
                    }
                }
            } catch (e: Exception) {
                // Network error - if we have cache, keep it, otherwise show error
                if (cachedProfile == null) {
                    _error.value = "Network error: ${e.message}"
                }
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Update profile information with cache update
     */
    fun updateProfile(
        nic: String,
        fullName: String,
        email: String,
        phoneNumber: String,
        address: String? = null,
        vehicleNumber: String? = null,
        vehicleModel: String? = null,
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
                    address = address,
                    vehicleNumber = vehicleNumber,
                    vehicleModel = vehicleModel,
                    password = password
                )
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true && apiResponse.data != null) {
                        // Update cache with new profile data
                        offlineRepository.cacheUserProfile(apiResponse.data)
                        
                        // Update UI
                        _profile.value = apiResponse.data
                        _success.value = true
                        _isDataFromCache.value = false
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

    /**
     * Deactivate account
     * This will permanently deactivate the user's account
     * @param nic The NIC of the account to deactivate
     */
    fun deactivateAccount(nic: String) {
        if (nic.isBlank()) {
            _error.value = "Invalid NIC"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _deactivationSuccess.value = false
            
            try {
                val response = repository.deactivateAccount(nic)
                
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        // Clear cached profile data
                        offlineRepository.clearUserProfileCache(nic)
                        
                        _deactivationSuccess.value = true
                    } else {
                        _error.value = apiResponse?.message ?: "Failed to deactivate account"
                    }
                } else {
                    when (response.code()) {
                        401 -> _error.value = "Unauthorized. Please login again."
                        404 -> _error.value = "Account not found"
                        400 -> _error.value = "Cannot deactivate account at this time"
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
}
