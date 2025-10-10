package com.example.evcharger.repository

import com.example.evcharger.model.ApiResponse
import com.example.evcharger.model.EvOwnerProfile
import com.example.evcharger.model.EvOwnerUpdateRequest
import com.example.evcharger.network.RetrofitClient
import retrofit2.Response

/**
 * Repository for managing EV Owner profile operations via API.
 * Handles profile retrieval and updates with bearer token authentication.
 */
class ProfileRepository {

    private val api = RetrofitClient.api

    /**
     * Fetch EV Owner profile by NIC
     * GET /api/evowner/{nic}
     * Requires bearer token authentication
     */
    suspend fun getProfile(nic: String): Response<ApiResponse<EvOwnerProfile>> {
        return api.getEvOwnerProfile(nic)
    }

    /**
     * Update EV Owner profile
     * PUT /api/evowner/{nic}
     * Requires bearer token authentication
     * 
     * @param nic The NIC of the EV owner
     * @param fullName Updated full name
     * @param email Updated email
     * @param phoneNumber Updated phone number
     * @param password Optional new password (null if not changing)
     */
    suspend fun updateProfile(
        nic: String,
        fullName: String,
        email: String,
        phoneNumber: String,
        password: String? = null
    ): Response<ApiResponse<EvOwnerProfile>> {
        val updateRequest = EvOwnerUpdateRequest(
            nic = nic,
            fullName = fullName,
            email = email,
            phoneNumber = phoneNumber,
            password = password
        )
        return api.updateEvOwnerProfile(nic, updateRequest)
    }
}
