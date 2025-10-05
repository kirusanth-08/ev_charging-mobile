package com.example.evcharger.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.evcharger.model.User
import com.example.evcharger.model.LoginRequest
import com.example.evcharger.network.RetrofitClient
import com.example.evcharger.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles EV Owner local login (lookup by NIC) and operator login is handled in OperatorViewModel.
 */
class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UserRepository(app)

    val userLive = MutableLiveData<User?>()
    val errorLive = MutableLiveData<String?>()
    val loading = MutableLiveData(false)

    fun loginOwner(nic: String, password: String) {
        if (nic.isBlank() || password.isBlank()) {
            errorLive.postValue("NIC and password are required")
            return
        }
        loading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = RetrofitClient.api.login(LoginRequest(username = nic, password = password))
                if (res.isSuccessful && res.body()?.success == true) {
                    val local = repo.getByNic(nic) ?: User(nic, fullName = nic, email = "", phone = "")
                    userLive.postValue(local)
                } else {
                    errorLive.postValue(res.body()?.message ?: "Login failed")
                }
            } catch (e: Exception) {
                errorLive.postValue(e.localizedMessage ?: "Network error")
            } finally {
                loading.postValue(false)
            }
        }
    }
}