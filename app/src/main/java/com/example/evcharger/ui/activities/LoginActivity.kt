package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.example.evcharger.databinding.ActivityLoginBinding
import com.example.evcharger.viewmodel.LoginViewModel
import com.example.evcharger.auth.UserSessionManager
import android.util.Log
import android.widget.Toast

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


        vm.loading.observe(this) { isLoading ->
            binding.progressLogin.visibility = if (isLoading == true) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnLogin.isEnabled = isLoading != true
            binding.btnSignup.isEnabled = isLoading != true
        }

        vm.userLive.observe(this) {
            if (it != null) {
                val role = vm.roleLive.value ?: ""
                val token = vm.tokenLive.value
                val username = vm.usernameLive.value

                // Persist session with NIC
                if (!token.isNullOrBlank()) {
                    val mgr = UserSessionManager(this)
                    mgr.saveSession(token, role, username ?: "", it.nic, null)
                }

                // Route based on role
                Log.d("LoginActivity", "Routing after login, role='$role' username='$username'")
                Toast.makeText(this, "Logged in as: $role", Toast.LENGTH_SHORT).show()
                if (role.equals("StationOperator", ignoreCase = true) || role.equals("Operator", ignoreCase = true)) {
                    val i = Intent(this, OperatorDashboardActivity::class.java)
                    i.putExtra("USERNAME", username)
                    startActivity(i)
                } else {
                    val i = Intent(this, HomeActivity::class.java)
                    i.putExtra("NIC", it.nic)
                    startActivity(i)
                }

                finish()
            }
        }
        vm.errorLive.observe(this) { msg ->
            msg?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }
    }
}