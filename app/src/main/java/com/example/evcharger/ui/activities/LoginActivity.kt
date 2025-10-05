package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.example.evcharger.databinding.ActivityLoginBinding
import com.example.evcharger.viewmodel.LoginViewModel

/**
 * EV Owner login by NIC (local SQLite lookup).
 * Operator login is offered via a button to go to QRScannerActivity (or separate screen).
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val vm: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val nic = binding.inputNic.text?.toString()?.trim().orEmpty()
            val pass = binding.inputPassword.text?.toString()?.trim().orEmpty()
            vm.loginOwner(nic, pass)
        }

        binding.btnSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.btnOperator.setOnClickListener {
            // Operator can head to scanner screen (with an operator login prompt there)
            startActivity(Intent(this, QRScannerActivity::class.java))
        }

        vm.userLive.observe(this) {
            if (it != null) {
                val i = Intent(this, DashboardActivity::class.java)
                i.putExtra("NIC", it.nic)
                startActivity(i)
                finish()
            }
        }
        vm.errorLive.observe(this) { msg ->
            msg?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }
    }
}