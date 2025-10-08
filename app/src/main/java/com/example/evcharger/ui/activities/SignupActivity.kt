package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.google.android.material.snackbar.Snackbar
import com.example.evcharger.databinding.ActivitySignupBinding
import com.example.evcharger.model.User
import com.example.evcharger.viewmodel.SignupViewModel

/**
 * Signup local EV Owner account (SQLite).
 */
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val vm: SignupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button handler
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Real-time password matching validation
        setupPasswordValidation()

        binding.btnRegister.setOnClickListener {
            if (validateForm()) {
                val user = User(
                    nic = binding.inputNic.text.toString().trim(),
                    fullName = binding.inputName.text.toString().trim(),
                    email = binding.inputEmail.text.toString().trim(),
                    phone = binding.inputPhone.text.toString().trim(),
                    isActive = true
                )
                val pwd = binding.inputPassword.text.toString().trim()
                vm.register(user.copy(), pwd)
            }
        }

        vm.successLive.observe(this) {
            if (it) {
                Snackbar.make(binding.root, "Account created successfully!", Snackbar.LENGTH_SHORT).show()
                finish() // Return to login screen
            }
        }
        vm.errorLive.observe(this) { msg ->
            msg?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }
        vm.loading.observe(this) { isLoading ->
            binding.progressSignup.visibility = if (isLoading == true) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnRegister.isEnabled = isLoading != true
        }
    }
    
    private fun setupPasswordValidation() {
        // Clear error when user starts typing
        binding.inputConfirmPassword.doOnTextChanged { text, _, _, _ ->
            binding.layoutConfirmPassword.error = null
        }
        
        binding.inputPassword.doOnTextChanged { text, _, _, _ ->
            binding.layoutConfirmPassword.error = null
        }
    }
    
    private fun validateForm(): Boolean {
        var isValid = true
        
        // Validate NIC
        val nic = binding.inputNic.text.toString().trim()
        if (nic.isEmpty()) {
            binding.inputNic.error = "NIC is required"
            isValid = false
        } else if (nic.length < 10) {
            binding.inputNic.error = "Invalid NIC format"
            isValid = false
        }
        
        // Validate Full Name
        val fullName = binding.inputName.text.toString().trim()
        if (fullName.isEmpty()) {
            binding.inputName.error = "Full name is required"
            isValid = false
        }
        
        // Validate Email
        val email = binding.inputEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.inputEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmail.error = "Invalid email format"
            isValid = false
        }
        
        // Validate Phone
        val phone = binding.inputPhone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.inputPhone.error = "Phone is required"
            isValid = false
        }
        
        // Validate Password
        val password = binding.inputPassword.text.toString().trim()
        if (password.isEmpty()) {
            binding.inputPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.inputPassword.error = "Password must be at least 6 characters"
            isValid = false
        }
        
        // Validate Confirm Password
        val confirmPassword = binding.inputConfirmPassword.text.toString().trim()
        if (confirmPassword.isEmpty()) {
            binding.layoutConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.layoutConfirmPassword.error = "Passwords do not match"
            isValid = false
        }
        
        return isValid
    }
}