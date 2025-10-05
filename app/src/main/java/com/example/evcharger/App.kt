package com.example.evcharger

import android.app.Application
import com.example.evcharger.auth.UserSessionManager
import com.example.evcharger.network.RetrofitClient

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Load saved session and set token for Retrofit if present
        val mgr = UserSessionManager(this)
        val sess = mgr.loadSession()
        sess?.token?.let { RetrofitClient.setAuthToken(it) }
    }
}
