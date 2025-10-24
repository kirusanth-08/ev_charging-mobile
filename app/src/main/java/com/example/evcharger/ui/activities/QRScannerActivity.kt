package com.example.evcharger.ui.activities

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
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
 * - Shows confirmation dialog before confirming arrival
 * - Confirms booking after operator verification
 */
class QRScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrscannerBinding
    private val vm: OperatorViewModel by viewModels()
    private lateinit var inProgressAdapter: InProgressBookingAdapter

    private val launcher = registerForActivityResult(ScanContract()) { result ->
        if (result != null && result.contents != null) {
            // Store payload and directly confirm arrival
            lastScannedPayload = result.contents
            // Directly call confirmArrival without showing QR code details
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

        // Set green status bar to match operator dashboard theme
        StatusBarUtil.setGreen(this)

        // Initialize RecyclerView adapter
        inProgressAdapter = InProgressBookingAdapter { booking ->
            showCompleteBookingDialog(booking)
        }
        binding.rvInProgressBookings.apply {
            adapter = inProgressAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@QRScannerActivity)
        }

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

            Snackbar.make(binding.root, "Welcome back, ${sess.username}!", Snackbar.LENGTH_SHORT).show()
            
            // Load in-progress bookings for logged-in operator
            vm.fetchInProgressBookings()
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
    }
    
    /**
     * Start the QR code scanner with portrait orientation locked
     */
    private fun startQRScanner() {
        val opts = ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        opts.setPrompt("Scan reservation QR code to confirm arrival")
        opts.setBeepEnabled(true)
        opts.setOrientationLocked(true) // Lock to portrait orientation
        launcher.launch(opts)
    }
    
    /**
     * Show dialog to complete a booking with energy and cost input
     */
    private fun showCompleteBookingDialog(booking: com.example.evcharger.model.BookingResponseData) {
        val dialogView = LayoutInflater.from(this).inflate(
            android.R.layout.select_dialog_item, // Temporary, we'll create a custom layout
            null
        )
        
        // Create a simple input dialog
        val energyInput = android.widget.EditText(this).apply {
            hint = "Energy Consumed (kWh)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        
        val costInput = android.widget.EditText(this).apply {
            hint = "Total Cost (LKR)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(energyInput)
            addView(costInput)
        }
        
        AlertDialog.Builder(this)
            .setTitle("⚡ Complete Charging")
            .setMessage("Booking ID: ${booking.bookingId}\nCustomer: ${booking.evOwnerNic}\n\nEnter charging details:")
            .setView(container)
            .setPositiveButton("Complete") { dialog, _ ->
                val energyStr = energyInput.text.toString()
                val costStr = costInput.text.toString()
                
                if (energyStr.isBlank() || costStr.isBlank()) {
                    Snackbar.make(binding.root, "Please enter both energy and cost", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                try {
                    val energy = energyStr.toDouble()
                    val cost = costStr.toDouble()
                    
                    if (energy <= 0 || cost <= 0) {
                        Snackbar.make(binding.root, "Values must be greater than 0", Snackbar.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    
                    vm.completeBooking(booking.bookingId ?: "", energy, cost)
                    dialog.dismiss()
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Invalid number format", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
            
            // Update loading message text
            if (isLoading == true) {
                binding.txtLoadingMessage.text = "Processing QR code and confirming arrival..."
            }
            
            // Disable inputs during operations to prevent duplicate calls
            binding.btnOperatorLogin.isEnabled = isLoading != true
            binding.btnScanQR.isEnabled = isLoading != true
            binding.btnConfirmArrival.isEnabled = isLoading != true
        }
        
        vm.role.observe(this) { r ->
            if (r.equals("StationOperator", ignoreCase = true)) {
                Snackbar.make(binding.root, "Operator logged in successfully!", Snackbar.LENGTH_SHORT).show()
                binding.loginSection.visibility = android.view.View.GONE
                // Load in-progress bookings after successful login
                vm.fetchInProgressBookings()
            } else if (!r.isNullOrBlank()) {
                Snackbar.make(binding.root, "Access denied: requires StationOperator role", Snackbar.LENGTH_LONG).show()
            }
        }
        
        vm.scannedReservation.observe(this) {
            if (it != null) {
                // Show success dialog
                val bookingId = it.id ?: it.bookingId ?: "N/A"
                val customerNic = it.evOwnerNic ?: it.ownerNic ?: "N/A"
                val slotNumber = it.slotNumber ?: "N/A"
                
                AlertDialog.Builder(this)
                    .setTitle("✅ Arrival Confirmed Successfully!")
                    .setMessage(
                        "Booking ID: $bookingId\n" +
                        "Customer NIC: $customerNic\n" +
                        "Slot: $slotNumber\n\n" +
                        "The customer's arrival has been confirmed."
                    )
                    .setPositiveButton("OK") { dialog, _ -> 
                        dialog.dismiss()
                        // Optionally clear the reservation details
                        binding.reservationDetailsCard.visibility = android.view.View.GONE
                    }
                    .setNeutralButton("Scan Another") { dialog, _ ->
                        dialog.dismiss()
                        binding.reservationDetailsCard.visibility = android.view.View.GONE
                        startQRScanner()
                    }
                    .setCancelable(false)
                    .show()
                    
                // Refresh in-progress bookings after successful confirmation
                vm.fetchInProgressBookings()
            }
        }
        
        vm.inProgressBookings.observe(this) { bookings ->
            if (bookings != null) {
                inProgressAdapter.submitList(bookings)
                binding.txtInProgressCount.text = "${bookings.size} booking(s) in progress"
                binding.inProgressSection.visibility = 
                    if (bookings.isEmpty()) android.view.View.GONE 
                    else android.view.View.VISIBLE
            }
        }
        
        vm.bookingCompleted.observe(this) { completed ->
            if (completed == true) {
                AlertDialog.Builder(this)
                    .setTitle("✅ Charging Completed")
                    .setMessage("The booking has been successfully completed and marked as finished.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        vm.bookingCompleted.postValue(false) // Reset
                    }
                    .show()
            }
        }
        
        vm.error.observe(this) { msg ->
            msg?.let { errorMessage ->
                // Always show error in a dialog with the actual API response message
                AlertDialog.Builder(this)
                    .setTitle("❌ Confirmation Failed")
                    .setMessage(errorMessage)
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .setNeutralButton("Scan Again") { dialog, _ ->
                        dialog.dismiss()
                        startQRScanner()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }
}