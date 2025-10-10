package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.evcharger.R
import com.example.evcharger.databinding.ActivityProfileBinding
import com.example.evcharger.viewmodel.ProfileViewModel
import com.example.evcharger.auth.UserSessionManager
import com.example.evcharger.utils.StatusBarUtil
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

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

        // Set green status bar to match toolbar
        StatusBarUtil.setGreen(this)

        sessionManager = UserSessionManager(this)
        
        // Setup toolbar
        setSupportActionBar(binding.profileTopAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.profileTopAppBar.setNavigationOnClickListener {
            finish()
        }
        
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
            binding.profileContent.visibility = View.GONE
        }
    }

    private fun setupObservers() {
        // Observe profile data
        viewModel.profile.observe(this) { profile ->
            profile?.let {
                // Update hero card
                binding.txtProfileName.text = it.fullName
                binding.txtProfileNic.text = "NIC: ${it.nic}"
                
                // Personal Information
                binding.txtEmail.text = it.email
                binding.txtPhone.text = it.phoneNumber
                
                // Address (show only if available)
                if (!it.address.isNullOrBlank()) {
                    binding.txtAddress.text = it.address
                    binding.layoutAddress.visibility = View.VISIBLE
                    binding.dividerAddress.visibility = View.VISIBLE
                } else {
                    binding.layoutAddress.visibility = View.GONE
                    binding.dividerAddress.visibility = View.GONE
                }
                
                // Vehicle Information
                binding.txtVehicleNumber.text = if (!it.vehicleNumber.isNullOrBlank()) {
                    it.vehicleNumber
                } else {
                    "Not set"
                }
                
                binding.txtVehicleModel.text = if (!it.vehicleModel.isNullOrBlank()) {
                    it.vehicleModel
                } else {
                    "Not set"
                }
                
                // Account Status
                if (it.isActive) {
                    binding.txtStatus.text = "Active"
                    binding.txtStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                    binding.iconStatus.setColorFilter(getColor(android.R.color.holo_green_dark))
                } else {
                    binding.txtStatus.text = "Inactive"
                    binding.txtStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    binding.iconStatus.setColorFilter(getColor(android.R.color.holo_red_dark))
                }
                
                // Member Since
                it.createdAt?.let { created ->
                    binding.txtMemberSince.text = formatDate(created)
                }
                
                // Show content, hide error
                binding.profileContent.visibility = View.VISIBLE
                binding.txtProfileInfo.visibility = View.GONE
            }
        }

        // Observe loading state
        viewModel.loading.observe(this) { isLoading ->
            binding.progressProfile.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnEditProfile.isEnabled = !isLoading
            binding.btnLogout.isEnabled = !isLoading
            
            if (isLoading) {
                binding.profileContent.visibility = View.GONE
                binding.txtProfileInfo.visibility = View.GONE
            }
        }

        // Observe errors
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                binding.profileContent.visibility = View.GONE
                binding.txtProfileInfo.visibility = View.VISIBLE
                binding.txtProfileInfo.text = "Failed to load profile\n\n$it"
                
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
            val date = isoDate.split("T").firstOrNull()?.split("-")
            if (date != null && date.size == 3) {
                "${date[2]}/${date[1]}/${date[0]}"  // DD/MM/YYYY
            } else {
                isoDate.split("T").firstOrNull() ?: isoDate
            }
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
        lifecycleScope.launch {
            // Clear session
            sessionManager.clearSessionAsync()
            
            // Clear token from RetrofitClient
            com.example.evcharger.network.RetrofitClient.setAuthToken(null)
            
            // Show success message
            Snackbar.make(binding.root, "Logged out successfully", Snackbar.LENGTH_SHORT).show()
            
            // Navigate to login screen
            navigateToLogin()
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
