package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.ScanOptions
import com.journeyapps.barcodescanner.ScanContract
import com.example.evcharger.databinding.ActivityQrscannerBinding
import com.example.evcharger.viewmodel.OperatorViewModel

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
            val opId = "operator-1" // retrieve from login response if needed
            vm.confirm(res.id ?: return@setOnClickListener, opId)
        }

        vm.operatorToken.observe(this) {
            Snackbar.make(binding.root, "Operator logged in", Snackbar.LENGTH_SHORT).show()
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