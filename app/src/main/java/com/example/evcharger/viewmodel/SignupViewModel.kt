package com.example.evcharger.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.evcharger.model.User
import com.example.evcharger.repository.UserRepository

/**
 * Registers and updates local EV Owner accounts.
 */
class SignupViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UserRepository(app)

    val successLive = MutableLiveData<Boolean>()
    val errorLive = MutableLiveData<String?>()

    fun register(user: User) {
        val ok = repo.register(user)
        if (ok) successLive.postValue(true) else errorLive.postValue("Registration failed (NIC exists?)")
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