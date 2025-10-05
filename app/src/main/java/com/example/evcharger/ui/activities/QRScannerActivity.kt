package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.ScanOptions
import com.journeyapps.barcodescanner.ScanContract
import com.example.evcharger.databinding.ActivityQrscannerBinding
import com.example.evcharger.viewmodel.OperatorViewModel
import com.example.evcharger.auth.UserSessionManager

/**
 * Operator login and QR scanning screen.
 * - Allows operator to login to obtain token
 * - Scans QR to retrieve booking
 * - Confirms booking
 */
class QRScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrscannerBinding
    private val vm: OperatorViewModel by viewModels()

    private val launcher = registerForActivityResult(ScanContract()) { result ->
        if (result != null && result.contents != null) {
            vm.lookupByQr(result.contents)
        } else {
            Snackbar.make(binding.root, "Scan cancelled", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrscannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Disable actions until logged in as StationOperator
        binding.btnScan.isEnabled = false
        binding.btnConfirm.isEnabled = false

        binding.btnOperatorLogin.setOnClickListener {
            val user = binding.inputOperatorUser.text.toString().trim()
            val pass = binding.inputOperatorPass.text.toString().trim()
            vm.login(user, pass)
        }

        binding.btnScan.setOnClickListener {
            val opts = ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            opts.setPrompt("Scan reservation QR")
            launcher.launch(opts)
        }

        binding.btnConfirm.setOnClickListener {
            val res = vm.scannedReservation.value ?: return@setOnClickListener
            val opUser = vm.operatorUsername.value
            if (opUser.isNullOrBlank()) {
                Snackbar.make(binding.root, "Missing operator identity", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            vm.confirm(res.id ?: return@setOnClickListener, opUser)
        }

        vm.operatorToken.observe(this) {
            // token set, wait for role check to enable actions
            // Persist session when token appears
            val token = vm.operatorToken.value
            val role = vm.role.value
            val username = vm.operatorUsername.value
            if (!token.isNullOrBlank()) {
                val mgr = UserSessionManager(this)
                mgr.saveSession(token, role ?: "", username ?: "", null)
            }
        }
        vm.loading.observe(this) { isLoading ->
            binding.progressOperator.visibility = if (isLoading == true) android.view.View.VISIBLE else android.view.View.GONE
            // Disable inputs during operations to prevent duplicate calls
            binding.btnOperatorLogin.isEnabled = isLoading != true
            binding.btnScan.isEnabled = (isLoading != true) && (vm.role.value.equals("StationOperator", true))
            binding.btnConfirm.isEnabled = (isLoading != true) && (vm.role.value.equals("StationOperator", true))
        }
        vm.role.observe(this) { r ->
            if (r.equals("StationOperator", ignoreCase = true)) {
                Snackbar.make(binding.root, "Operator logged in", Snackbar.LENGTH_SHORT).show()
                binding.btnScan.isEnabled = true
                binding.btnConfirm.isEnabled = true
            } else if (!r.isNullOrBlank()) {
                Snackbar.make(binding.root, "Access denied: requires StationOperator", Snackbar.LENGTH_LONG).show()
            }
        }
        vm.scannedReservation.observe(this) {
            if (it != null) {
                binding.txtReservationInfo.text = "Reservation: ${it.id}\nStatus: ${it.status}\nStart: ${it.startTime}"
            }
        }
        vm.error.observe(this) { msg ->
            msg?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }
    }
}