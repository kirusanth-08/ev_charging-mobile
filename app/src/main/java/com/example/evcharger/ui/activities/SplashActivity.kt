package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.evcharger.auth.UserSessionManager
import com.example.evcharger.databinding.ActivitySplashBinding
import com.example.evcharger.network.RetrofitClient

/**
 * Splash screen that checks for stored credentials and auto-logs in
 * If session exists, navigate directly to appropriate dashboard
 * Otherwise, show login screen
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val splashDelay = 1500L // 1.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Delay to show splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            checkSessionAndNavigate()
        }, splashDelay)
    }
    
    private fun checkSessionAndNavigate() {
        val sessionManager = UserSessionManager(this)
        val session = sessionManager.loadSession()
        
        // Check if user is logged in
        if (sessionManager.isLoggedIn() && !session.token.isNullOrBlank()) {
            // Restore auth token to RetrofitClient
            RetrofitClient.setAuthToken(session.token)
            
            // Navigate based on role
            val role = session.role ?: ""
            val intent = when {
                role.equals("StationOperator", ignoreCase = true) || 
                role.equals("Operator", ignoreCase = true) -> {
                    Intent(this, OperatorDashboardActivity::class.java).apply {
                        putExtra("USERNAME", session.username)
                    }
                }
                else -> {
                    // EV Owner
                    Intent(this, HomeActivity::class.java).apply {
                        putExtra("NIC", session.nic ?: session.username)
                    }
                }
            }
            
            startActivity(intent)
            finish()
        } else {
            // No session, go to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
