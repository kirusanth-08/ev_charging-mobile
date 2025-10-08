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
import com.example.evcharger.utils.StatusBarUtil

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
            // store payload and immediately confirm arrival by posting the QR payload
            lastScannedPayload = result.contents
            vm.confirmArrival(result.contents)
        } else {
            Snackbar.make(binding.root, "Scan cancelled", Snackbar.LENGTH_SHORT).show()
        }
    }

    // Keep track of last scanned payload so operator can confirm arrival
    private var lastScannedPayload: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrscannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set black status bar for camera view
        StatusBarUtil.setBlack(this)

        // If there's a persisted operator session, apply it and hide login inputs
        val mgr = UserSessionManager(this)
        val sess = mgr.loadSession()
        if (!sess.token.isNullOrBlank()) {
            com.example.evcharger.network.RetrofitClient.setAuthToken(sess.token)
            vm.operatorToken.postValue(sess.token)
            vm.role.postValue(sess.role)
            vm.operatorUsername.postValue(sess.username)

            // Hide login inputs when a session is available
            binding.inputOperatorUser.visibility = android.view.View.GONE
            binding.inputOperatorPass.visibility = android.view.View.GONE
            binding.btnOperatorLogin.visibility = android.view.View.GONE

            Snackbar.make(binding.root, "Operator session restored: ${sess.username}", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnOperatorLogin.setOnClickListener {
            val user = binding.inputOperatorUser.text.toString().trim()
            val pass = binding.inputOperatorPass.text.toString().trim()
            vm.login(user, pass)
        }

        // Support autoScan flow: if launched with autoScan=true, immediately start scanner
        val autoScan = intent?.getBooleanExtra("autoScan", false) ?: false
        if (autoScan) {
            // Allow the capture activity to rotate with the device (don't force landscape)
            val opts = ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            opts.setPrompt("Scan reservation QR")
            // By default the embedded capture activity may lock orientation; disable that so portrait works
            try {
                opts.setOrientationLocked(false)
            } catch (ignored: Throwable) {
                // If the runtime library version doesn't expose this, ignore and continue
            }
            launcher.launch(opts)
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
        }
        vm.role.observe(this) { r ->
            if (r.equals("StationOperator", ignoreCase = true)) {
                Snackbar.make(binding.root, "Operator logged in", Snackbar.LENGTH_SHORT).show()
            } else if (!r.isNullOrBlank()) {
                Snackbar.make(binding.root, "Access denied: requires StationOperator", Snackbar.LENGTH_LONG).show()
            }
        }
        // After calling confirmArrival the ViewModel will update scannedReservation if the API returns data
        vm.scannedReservation.observe(this) {
            if (it != null) {
                binding.txtReservationInfo.text = "Reservation: ${it.id}\nStatus: ${it.status}\nStart: ${it.startTime}\nScanned QR: ${lastScannedPayload ?: "(unknown)"}"
                // Confirm button still available for manual retry if needed
                binding.btnConfirmArrival.visibility = android.view.View.VISIBLE
            }
        }
        vm.error.observe(this) { msg ->
            msg?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }
        
        // Confirm arrival button: post scanned QR payload to backend (manual retry)
        binding.btnConfirmArrival.setOnClickListener {
            val payload = lastScannedPayload ?: ""
            if (payload.isBlank()) {
                Snackbar.make(binding.root, "No scanned QR available to confirm", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            vm.confirmArrival(payload)
        }
    }
}