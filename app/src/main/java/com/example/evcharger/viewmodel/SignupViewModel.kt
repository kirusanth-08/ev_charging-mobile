package com.example.evcharger.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.evcharger.model.EvOwnerRegisterRequest
import com.example.evcharger.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.evcharger.model.User
import com.example.evcharger.repository.UserRepository

/**
 * Registers and updates local EV Owner accounts.
 */
class SignupViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UserRepository(app)

    val successLive = MutableLiveData<Boolean>()
    val errorLive = MutableLiveData<String?>()
    val loading = MutableLiveData(false)

    fun register(user: User, password: String) {
        // Attempt server registration first, fallback to local DB
        loading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val body = EvOwnerRegisterRequest(
                    NIC = user.nic,
                    FullName = user.fullName,
                    Email = user.email,
                    PhoneNumber = user.phone,
                    Password = password
                )
                val res = RetrofitClient.api.registerEvOwner(body)
                if (res.isSuccessful && res.body()?.success == true) {
                    successLive.postValue(true)
                } else {
                    // Server responded but registration failed (e.g., nic exists)
                    errorLive.postValue(res.body()?.message ?: "Registration failed")
                }
            } catch (e: Exception) {
                // network error - fallback to local DB registration
                val ok = repo.register(user)
                if (ok) successLive.postValue(true) else errorLive.postValue(e.localizedMessage ?: "Registration failed")
            } finally {
                loading.postValue(false)
            }
        }
    }

    fun update(user: User) {
        val ok = repo.update(user)
        if (ok) successLive.postValue(true) else errorLive.postValue("Update failed")
    }

    fun deactivate(nic: String) {
        val ok = repo.deactivate(nic)
        if (ok) successLive.postValue(true) else errorLive.postValue("Deactivation failed")
    }
}