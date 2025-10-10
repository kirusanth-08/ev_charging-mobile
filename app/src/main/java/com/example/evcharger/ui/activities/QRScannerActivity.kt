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

        // Set transparent status bar with dark icons for modern look
        StatusBarUtil.makeTransparent(this, lightIcons = false)

        // If there's a persisted operator session, apply it and hide login inputs
        val mgr = UserSessionManager(this)
        val sess = mgr.loadSession()
        if (!sess.token.isNullOrBlank()) {
            com.example.evcharger.network.RetrofitClient.setAuthToken(sess.token)
            vm.operatorToken.postValue(sess.token)
            vm.role.postValue(sess.role)
            vm.operatorUsername.postValue(sess.username)

            // Hide login section when a session is available
            binding.loginSection.visibility = android.view.View.GONE

            Snackbar.make(binding.root, "Welcome back, ${sess.username}! ⚡", Snackbar.LENGTH_SHORT).show()
        }

        // Login button click handler
        binding.btnOperatorLogin.setOnClickListener {
            val user = binding.inputOperatorUser.text.toString().trim()
            val pass = binding.inputOperatorPass.text.toString().trim()
            
            if (user.isEmpty() || pass.isEmpty()) {
                Snackbar.make(binding.root, "Please enter username and password", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            vm.login(user, pass)
        }

        // Scan QR button click handler
        binding.btnScanQR.setOnClickListener {
            startQRScanner()
        }

        // Scan another button (appears after successful scan)
        binding.btnScanAnother.setOnClickListener {
            binding.reservationDetailsCard.visibility = android.view.View.GONE
            startQRScanner()
        }

        // Support autoScan flow: if launched with autoScan=true, immediately start scanner
        val autoScan = intent?.getBooleanExtra("autoScan", false) ?: false
        if (autoScan && !sess.token.isNullOrBlank()) {
            // Only auto-scan if user is already logged in
            startQRScanner()
        }

        // Observe ViewModel state changes
        setupObservers()
        
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
    
    /**
     * Start the QR code scanner
     */
    private fun startQRScanner() {
        val opts = ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        opts.setPrompt("📱 Scan reservation QR code")
        opts.setBeepEnabled(true)
        try {
            opts.setOrientationLocked(false) // Allow rotation
        } catch (ignored: Throwable) {
            // If the runtime library version doesn't expose this, ignore and continue
        }
        launcher.launch(opts)
    }
    
    /**
     * Setup all ViewModel observers
     */
    private fun setupObservers() {
        vm.operatorToken.observe(this) {
            // token set, wait for role check to enable actions
            // Persist session when token appears
            val token = vm.operatorToken.value
            val role = vm.role.value
            val username = vm.operatorUsername.value
            if (!token.isNullOrBlank()) {
                val mgr = UserSessionManager(this)
                // Operators don't have NIC, so pass null for nic parameter
                mgr.saveSession(token, role ?: "", username ?: "", null, null)
            }
        }
        
        vm.loading.observe(this) { isLoading ->
            binding.progressOperator.visibility = if (isLoading == true) android.view.View.VISIBLE else android.view.View.GONE
            binding.txtLoadingMessage.visibility = if (isLoading == true) android.view.View.VISIBLE else android.view.View.GONE
            // Disable inputs during operations to prevent duplicate calls
            binding.btnOperatorLogin.isEnabled = isLoading != true
            binding.btnScanQR.isEnabled = isLoading != true
        }
        
        vm.role.observe(this) { r ->
            if (r.equals("StationOperator", ignoreCase = true)) {
                Snackbar.make(binding.root, "✅ Operator logged in successfully!", Snackbar.LENGTH_SHORT).show()
                // Hide login section after successful login
                binding.loginSection.visibility = android.view.View.GONE
            } else if (!r.isNullOrBlank()) {
                Snackbar.make(binding.root, "❌ Access denied: requires StationOperator role", Snackbar.LENGTH_LONG).show()
            }
        }
        
        // After calling confirmArrival the ViewModel will update scannedReservation if the API returns data
        vm.scannedReservation.observe(this) {
            if (it != null) {
                // Show reservation details card with beautiful formatting
                binding.reservationDetailsCard.visibility = android.view.View.VISIBLE
                binding.txtReservationStatus.text = it.status ?: "CONFIRMED"
                
                // Format reservation info with emojis
                val info = buildString {
                    append("📋 Reservation ID: ${it.id}\n")
                    append("👤 Customer: ${it.userId ?: "N/A"}\n")
                    append("🔋 Slot: ${it.slotId ?: "N/A"}\n")
                    append("⏰ Start: ${it.startTime ?: "N/A"}\n")
                    append("⏱️ End: ${it.endTime ?: "N/A"}\n")
                    append("📊 Status: ${it.status ?: "N/A"}\n")
                    append("🔐 QR Code: ${lastScannedPayload ?: "N/A"}")
                }
                binding.txtReservationInfo.text = info
                
                Snackbar.make(binding.root, "✅ Arrival confirmed successfully!", Snackbar.LENGTH_LONG).show()
            }
        }
        
        vm.error.observe(this) { msg ->
            msg?.let { 
                Snackbar.make(binding.root, "❌ $it", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}