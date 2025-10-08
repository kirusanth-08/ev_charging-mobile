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
        
        // Get NIC from session or intent
        val session = sessionManager.loadSession()
        currentNic = intent.getStringExtra("NIC") ?: session.username

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
                    append("NIC: ${it.nic}\n\n")
                    append("Full Name: ${it.fullName}\n\n")
                    append("Email: ${it.email}\n\n")
                    append("Phone: ${it.phoneNumber}\n\n")
                    append("Status: ${if (it.isActive == true) "Active" else "Inactive"}\n\n")
                    it.createdAt?.let { created ->
                        append("Member Since: ${formatDate(created)}\n\n")
                    }
                    it.updatedAt?.let { updated ->
                        append("Last Updated: ${formatDate(updated)}")
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
}
