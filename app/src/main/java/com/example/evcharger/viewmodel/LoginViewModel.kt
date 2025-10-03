package com.example.evcharger.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.evcharger.model.User
import com.example.evcharger.repository.UserRepository

/**
 * Handles EV Owner local login (lookup by NIC) and operator login is handled in OperatorViewModel.
 */
class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UserRepository(app)

    val userLive = MutableLiveData<User?>()
    val errorLive = MutableLiveData<String?>()

    fun loginByNic(nic: String) {
        val user = repo.getByNic(nic)
        if (user == null) {
            errorLive.postValue("User not found")
        } else if (!user.isActive) {
            errorLive.postValue("Account is deactivated")
        } else {
            userLive.postValue(user)
        }
    }
}