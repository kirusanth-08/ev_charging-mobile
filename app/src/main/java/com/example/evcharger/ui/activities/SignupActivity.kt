package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.example.evcharger.databinding.ActivitySignupBinding
import com.example.evcharger.model.User
import com.example.evcharger.viewmodel.SignupViewModel

/**
 * Signup/Update/Deactivate local EV Owner account (SQLite).
 */
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val vm: SignupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val user = User(
                nic = binding.inputNic.text.toString().trim(),
                fullName = binding.inputName.text.toString().trim(),
                email = binding.inputEmail.text.toString().trim(),
                phone = binding.inputPhone.text.toString().trim(),
                isActive = true
            )
            vm.register(user)
        }

        binding.btnUpdate.setOnClickListener {
            val user = User(
                nic = binding.inputNic.text.toString().trim(),
                fullName = binding.inputName.text.toString().trim(),
                email = binding.inputEmail.text.toString().trim(),
                phone = binding.inputPhone.text.toString().trim(),
                isActive = true
            )
            vm.update(user)
        }

        binding.btnDeactivate.setOnClickListener {
            val nic = binding.inputNic.text.toString().trim()
            vm.deactivate(nic)
        }

        vm.successLive.observe(this) {
            if (it) Snackbar.make(binding.root, "Success", Snackbar.LENGTH_SHORT).show()
        }
        vm.errorLive.observe(this) { msg ->
            msg?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }
    }
}