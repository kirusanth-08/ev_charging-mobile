package com.example.evcharger.ui.activities

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.evcharger.databinding.ActivityProfileEditBinding
import com.example.evcharger.viewmodel.ProfileViewModel
import com.example.evcharger.auth.UserSessionManager
import com.google.android.material.snackbar.Snackbar

class ProfileEditActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProfileEditBinding
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var sessionManager: UserSessionManager
    private var userNic: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = UserSessionManager(this)
        val session = sessionManager.loadSession()
        userNic = session.nic ?: session.username ?: ""
        
        setupUI()
        setupObservers()
        loadProfile()
    }
    
    private fun setupUI() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Update button
        binding.btnUpdate.setOnClickListener {
            updateProfile()
        }
    }
    
    private fun setupObservers() {
        // Profile data
        viewModel.profile.observe(this) { profile ->
            profile?.let {
                binding.inputNic.setText(it.nic)
                binding.inputName.setText(it.fullName)
                binding.inputEmail.setText(it.email)
                binding.inputPhone.setText(it.phoneNumber)
            }
        }
        
        // Loading state
        viewModel.loading.observe(this) { isLoading ->
            binding.progressUpdate.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnUpdate.isEnabled = !isLoading
            
            // Disable inputs while loading
            binding.inputName.isEnabled = !isLoading
            binding.inputEmail.isEnabled = !isLoading
            binding.inputPhone.isEnabled = !isLoading
            binding.inputPassword.isEnabled = !isLoading
        }
        
        // Error state
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getColor(android.R.color.holo_red_dark))
                    .show()
                viewModel.clearError()
            }
        }
        
        // Success state
        viewModel.success.observe(this) { isSuccess ->
            if (isSuccess) {
                Snackbar.make(binding.root, "Profile updated successfully", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getColor(android.R.color.holo_green_dark))
                    .show()
                
                // Navigate back after a short delay
                binding.root.postDelayed({
                    finish()
                }, 1500)
            }
        }
    }
    
    private fun loadProfile() {
        if (userNic.isNotEmpty()) {
            viewModel.loadProfile(userNic)
        } else {
            Snackbar.make(binding.root, "User session not found", Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(android.R.color.holo_red_dark))
                .show()
        }
    }
    
    private fun updateProfile() {
        val fullName = binding.inputName.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val phone = binding.inputPhone.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()
        
        // Validate inputs
        if (fullName.isEmpty()) {
            binding.inputName.error = "Full name is required"
            return
        }
        
        if (email.isEmpty()) {
            binding.inputEmail.error = "Email is required"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmail.error = "Invalid email address"
            return
        }
        
        if (phone.isEmpty()) {
            binding.inputPhone.error = "Phone number is required"
            return
        }
        
        // Password is optional
        val passwordToSend = if (password.isEmpty()) null else password
        
        // Call update method
        viewModel.updateProfile(
            nic = userNic,
            fullName = fullName,
            email = email,
            phoneNumber = phone,
            password = passwordToSend
        )
    }
}
