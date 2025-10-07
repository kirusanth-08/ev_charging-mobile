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
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Handles EV Owner local login (lookup by NIC) and operator login is handled in OperatorViewModel.
 */
class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UserRepository(app)

    val userLive = MutableLiveData<User?>()
    val errorLive = MutableLiveData<String?>()
    val loading = MutableLiveData(false)
    val tokenLive = MutableLiveData<String?>()
    val roleLive = MutableLiveData<String?>()
    val usernameLive = MutableLiveData<String?>()

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
                    val data = res.body()!!.data!!
                    val local = repo.getByNic(nic) ?: User(nic, fullName = nic, email = "", phone = "")
                    // Post token and role first so observers that react to userLive can read role immediately
                    tokenLive.postValue(data.token)
                    // Normalize and canonicalize role strings (robust against casing/spaces/variants)
                    val canonicalRole = normalizeRole(data.role)
                    roleLive.postValue(canonicalRole)
                    usernameLive.postValue(data.username)
                    // also set token for Retrofit immediately
                    RetrofitClient.setAuthToken(data.token)
                    // finally publish the local user object
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

    private fun normalizeRole(role: String?): String {
        if (role.isNullOrBlank()) return ""
        val r = role.trim().lowercase(Locale.ROOT)
        return when {
            r.contains("station") && r.contains("operator") -> "StationOperator"
            r.contains("operator") -> "StationOperator"
            r.contains("ev") && r.contains("owner") -> "EVOwner"
            r.contains("owner") && r.contains("ev").not() -> "EVOwner"
            r.contains("evowner") -> "EVOwner"
            r.contains("ev_owner") -> "EVOwner"
            r.contains("owner") -> "EVOwner"
            else -> role.trim()
        }
    }
}