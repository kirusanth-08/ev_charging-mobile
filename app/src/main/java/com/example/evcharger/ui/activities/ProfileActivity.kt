package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.evcharger.databinding.ActivityProfileBinding
import com.example.evcharger.viewmodel.ProfileViewModel
import com.example.evcharger.auth.UserSessionManager
import com.google.android.material.snackbar.Snackbar

/**
 * Profile screen for EV Owner
 * Displays profile information retrieved from the API
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var sessionManager: UserSessionManager
    private var currentNic: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = UserSessionManager(this)
        
        // Get NIC and token from session
        val session = sessionManager.loadSession()
        currentNic = session.nic ?: intent.getStringExtra("NIC")
        
        // ✅ IMPORTANT: Set auth token in RetrofitClient before making API calls
        // This ensures the Authorization header is included in the profile API request
        session.token?.let { token ->
            com.example.evcharger.network.RetrofitClient.setAuthToken(token)
        } ?: run {
            // No token found - redirect to login
            Snackbar.make(binding.root, "Session expired. Please login again.", Snackbar.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        // Set up UI
        setupUI()
        
        // Set up observers
        setupObservers()

        // Load profile data
        loadProfile()
    }
    
    override fun onResume() {
        super.onResume()
        // Reload profile when returning from edit screen
        loadProfile()
    }
    
    private fun setupUI() {
        // Edit profile button
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(this, ProfileEditActivity::class.java)
            startActivity(intent)
        }
        
        // Logout button
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }
    
    private fun loadProfile() {
        currentNic?.let { nic ->
            viewModel.loadProfile(nic)
        } ?: run {
            binding.txtProfileInfo.text = "Error: No user session found. Please login again."
            binding.txtProfileInfo.visibility = View.VISIBLE
        }
    }

    private fun setupObservers() {
        // Observe profile data
        viewModel.profile.observe(this) { profile ->
            profile?.let {
                val profileText = buildString {
                    append("📋 Personal Information\n")
                    append("━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
                    
                    append("NIC: ${it.nic}\n\n")
                    append("Full Name: ${it.fullName}\n\n")
                    append("Email: ${it.email}\n\n")
                    append("Phone: ${it.phoneNumber}\n\n")
                    
                    // Address (if available)
                    if (!it.address.isNullOrBlank()) {
                        append("Address: ${it.address}\n\n")
                    }
                    
                    append("\n🚗 Vehicle Information\n")
                    append("━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
                    
                    if (!it.vehicleNumber.isNullOrBlank()) {
                        append("Vehicle Number: ${it.vehicleNumber}\n\n")
                    } else {
                        append("Vehicle Number: Not set\n\n")
                    }
                    
                    if (!it.vehicleModel.isNullOrBlank()) {
                        append("Vehicle Model: ${it.vehicleModel}\n\n")
                    } else {
                        append("Vehicle Model: Not set\n\n")
                    }
                    
                    append("\n📊 Account Status\n")
                    append("━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
                    
                    append("Status: ${if (it.isActive) "✅ Active" else "❌ Inactive"}\n\n")
                    
                    it.createdAt?.let { created ->
                        append("Member Since: ${formatDate(created)}\n\n")
                    }
                    it.updatedAt?.let { updated ->
                        append("Last Updated: ${formatDate(updated)}\n\n")
                    }
                    it.deactivatedAt?.let { deactivated ->
                        append("Deactivated On: ${formatDate(deactivated)}")
                    }
                }
                binding.txtProfileInfo.text = profileText
                binding.txtProfileInfo.visibility = View.VISIBLE
            }
        }

        // Observe loading state
        viewModel.loading.observe(this) { isLoading ->
            binding.progressProfile.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnEditProfile.isEnabled = !isLoading
            
            if (isLoading) {
                binding.txtProfileInfo.visibility = View.GONE
            }
        }

        // Observe errors
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                binding.txtProfileInfo.visibility = View.VISIBLE
                binding.txtProfileInfo.text = "Failed to load profile: $it"
                
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getColor(android.R.color.holo_red_dark))
                    .show()
                viewModel.clearError()
            }
        }
    }

    private fun formatDate(isoDate: String): String {
        return try {
            // Simple formatting - you can enhance this with proper date formatting
            isoDate.split("T").firstOrNull()?.replace("-", "/") ?: isoDate
        } catch (e: Exception) {
            isoDate
        }
    }
    
    private fun logout() {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performLogout() {
        // Clear session
        sessionManager.clearSession()
        
        // Clear token from RetrofitClient
        com.example.evcharger.network.RetrofitClient.setAuthToken(null)
        
        // Show success message
        Snackbar.make(binding.root, "Logged out successfully", Snackbar.LENGTH_SHORT).show()
        
        // Navigate to login screen
        navigateToLogin()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
