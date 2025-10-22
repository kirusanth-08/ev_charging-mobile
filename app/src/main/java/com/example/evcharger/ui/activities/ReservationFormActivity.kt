package com.example.evcharger.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.android.material.snackbar.Snackbar
import com.example.evcharger.databinding.ActivityReservationFormBinding
import com.example.evcharger.viewmodel.ReservationViewModel
import com.example.evcharger.model.BackendSlot
import com.example.evcharger.model.StationAvailabilitySlot
import com.example.evcharger.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Enhanced reservation form with date, time, and slot selection
 */
class ReservationFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservationFormBinding
    private val vm: ReservationViewModel by viewModels()
    
    private var nic: String = ""
    private var stationId: String = ""
    private var stationName: String = ""
    private var stationAddress: String = ""
    
    private var selectedDate: LocalDate? = null
    private var selectedStartTime: LocalTime? = null
    private var selectedEndTime: LocalTime? = null
    private var selectedSlotNumber: Int? = null
    
    private val availableSlots = mutableListOf<BackendSlot>()
    private var isLoadingAvailability = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        loadIntentData()
        
        // Check account status before allowing booking
        checkAccountStatusBeforeBooking()
        
        // Setup UI
        setupUI()
        setupObservers()
        loadAvailableSlots()
    }
    
    private fun loadIntentData() {
        // Get NIC from session or intent
        val intentNic = intent.getStringExtra("NIC")
        val sessionNic = com.example.evcharger.auth.UserSessionManager(this).loadSession().nic
        nic = intentNic ?: sessionNic ?: ""
        
        // Get station details
        stationId = intent.getStringExtra("StationId") ?: intent.getStringExtra("stationId") ?: "station-1"
        stationName = intent.getStringExtra("StationName") ?: "Charging Station"
        stationAddress = intent.getStringExtra("StationAddress") ?: "Location"
        
        // Update header
        binding.txtStationName.text = stationName
        binding.txtStationAddress.text = stationAddress
    }
    
    private fun checkAccountStatusBeforeBooking() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionManager = com.example.evcharger.auth.UserSessionManager(this@ReservationFormActivity)
                val session = sessionManager.loadSession()
                session.token?.let { token ->
                    RetrofitClient.setAuthToken(token)
                    
                    val profileRepo = com.example.evcharger.repository.ProfileRepository()
                    val response = profileRepo.getProfile(nic)
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val profile = response.body()?.data
                        val isActive = profile?.isActive ?: false
                        
                        if (!isActive) {
                            withContext(Dispatchers.Main) {
                                showAccountDeactivatedAndFinish()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue if check fails
            }
        }
    }
    
    private fun showAccountDeactivatedAndFinish() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Account Deactivated")
            .setMessage(
                "Your account is currently deactivated.\n\n" +
                "Booking features are disabled.\n\n" +
                "To reactivate your account, please contact:\n📧 contact@evcharge.com"
            )
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun setupUI() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Date picker
        binding.btnPickDate.setOnClickListener {
            showDatePicker()
        }
        
        // Start time picker
        binding.btnPickStartTime.setOnClickListener {
            showStartTimePicker()
        }
        
        // End time picker
        binding.btnPickEndTime.setOnClickListener {
            showEndTimePicker()
        }
        
        // Submit button
        binding.btnSubmit.setOnClickListener {
            validateAndSubmit()
        }
    }
    
    private fun loadAvailableSlots() {
        if (isLoadingAvailability) return
        
        isLoadingAvailability = true
        binding.progressSlots.visibility = android.view.View.VISIBLE
        binding.txtSlotStatus.text = "Checking availability..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.getStationAvailability(stationId)
                
                withContext(Dispatchers.Main) {
                    binding.progressSlots.visibility = android.view.View.GONE
                    isLoadingAvailability = false
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val availabilityData = response.body()?.data
                        
                        if (availabilityData != null) {
                            availableSlots.clear()
                            
                            val actuallyAvailableSlots = availabilityData.slotDetails.filter { it.isAvailable }
                            
                            if (actuallyAvailableSlots.isEmpty()) {
                                binding.txtSlotStatus.text = "No slots currently available"
                                binding.spinnerSlot.isEnabled = false
                                binding.btnSubmit.isEnabled = false
                                Snackbar.make(
                                    binding.root,
                                    "All slots are currently booked. Please try another station or time.",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            } else {
                                actuallyAvailableSlots.forEach { slot ->
                                    availableSlots.add(
                                        BackendSlot(
                                            slotNumber = slot.slotNumber,
                                            isAvailable = true,
                                            powerRating = slot.powerRating,
                                            connectorType = slot.connectorType
                                        )
                                    )
                                }
                                
                                binding.txtSlotStatus.text = "${availableSlots.size} slot(s) available"
                                setupSlotSpinner()
                            }
                        } else {
                            showAvailabilityError("No availability data received")
                        }
                    } else {
                        showAvailabilityError(response.body()?.message ?: "Failed to check availability")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressSlots.visibility = android.view.View.GONE
                    isLoadingAvailability = false
                    showAvailabilityError("Error: ${e.localizedMessage}")
                }
            }
        }
    }
    
    private fun showAvailabilityError(message: String) {
        binding.txtSlotStatus.text = "Unable to check availability"
        binding.spinnerSlot.isEnabled = false
        binding.btnSubmit.isEnabled = false
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(android.R.color.holo_red_dark))
            .show()
    }
    
    private fun setupSlotSpinner() {
        val slotOptions = availableSlots.map { slot ->
            "Slot ${slot.slotNumber} - ${slot.connectorType} (${slot.powerRating}kW)"
        }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, slotOptions)
        binding.spinnerSlot.setAdapter(adapter)
        binding.spinnerSlot.isEnabled = true
        
        binding.spinnerSlot.setOnItemClickListener { parent, view, position, id ->
            val slot = availableSlots[position]
            selectedSlotNumber = slot.slotNumber
            
            binding.layoutSlotDetails.visibility = android.view.View.VISIBLE
            binding.txtSlotDetails.text = "Connector: ${slot.connectorType} • Power: ${slot.powerRating}kW • Status: Available"
            
            updateSummary()
        }
    }
    
    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Reservation Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        
        picker.addOnPositiveButtonClickListener { millis ->
            selectedDate = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            
            val formatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")
            binding.txtSelectedDate.text = selectedDate?.format(formatter) ?: "No date selected"
            binding.btnPickDate.text = selectedDate?.format(formatter) ?: "Choose Date"
            
            updateSummary()
        }
        
        picker.show(supportFragmentManager, "date_picker")
    }
    
    private fun showStartTimePicker() {
        val currentTime = LocalTime.now()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentTime.hour)
            .setMinute(currentTime.minute)
            .setTitleText("Select Start Time")
            .build()
        
        picker.addOnPositiveButtonClickListener {
            selectedStartTime = LocalTime.of(picker.hour, picker.minute)
            
            val formatter = DateTimeFormatter.ofPattern("hh:mm a")
            binding.txtSelectedStartTime.text = selectedStartTime?.format(formatter) ?: "No time selected"
            binding.btnPickStartTime.text = selectedStartTime?.format(formatter) ?: "Select Start Time"
            
            updateDuration()
            updateSummary()
        }
        
        picker.show(supportFragmentManager, "start_time_picker")
    }
    
    private fun showEndTimePicker() {
        val currentTime = selectedStartTime?.plusHours(2) ?: LocalTime.now().plusHours(2)
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentTime.hour)
            .setMinute(currentTime.minute)
            .setTitleText("Select End Time")
            .build()
        
        picker.addOnPositiveButtonClickListener {
            selectedEndTime = LocalTime.of(picker.hour, picker.minute)
            
            val formatter = DateTimeFormatter.ofPattern("hh:mm a")
            binding.txtSelectedEndTime.text = selectedEndTime?.format(formatter) ?: "No time selected"
            binding.btnPickEndTime.text = selectedEndTime?.format(formatter) ?: "Select End Time"
            
            updateDuration()
            updateSummary()
        }
        
        picker.show(supportFragmentManager, "end_time_picker")
    }
    
    private fun updateDuration() {
        val start = selectedStartTime
        val end = selectedEndTime
        
        if (start != null && end != null) {
            val duration = ChronoUnit.HOURS.between(start, end)
            if (duration > 0) {
                binding.txtDuration.text = "$duration hours"
            } else if (duration == 0L) {
                val minutes = ChronoUnit.MINUTES.between(start, end)
                binding.txtDuration.text = "$minutes minutes"
            } else {
                binding.txtDuration.text = "Invalid (end before start)"
                binding.txtDuration.setTextColor(getColor(android.R.color.holo_red_dark))
                return
            }
            binding.txtDuration.setTextColor(getColor(com.example.evcharger.R.color.primary))
        } else {
            binding.txtDuration.text = "0 hours"
        }
    }
    
    private fun updateSummary() {
        val date = selectedDate
        val startTime = selectedStartTime
        val endTime = selectedEndTime
        val slotNum = selectedSlotNumber
        
        if (date != null && startTime != null && endTime != null && slotNum != null) {
            val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
            val slot = availableSlots.find { it.slotNumber == slotNum }
            
            val duration = ChronoUnit.HOURS.between(startTime, endTime)
            
            binding.cardSummary.visibility = android.view.View.VISIBLE
            binding.txtSummary.text = buildString {
                append("Station: $stationName\n")
                append("Date: ${date.format(dateFormatter)}\n")
                append("Time: ${startTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}\n")
                append("Duration: $duration hours\n")
                append("Slot: #$slotNum (${slot?.connectorType ?: "Unknown"}, ${slot?.powerRating ?: 0}kW)")
            }
            
            // Enable submit button if end time is after start time
            binding.btnSubmit.isEnabled = duration > 0
        } else {
            binding.cardSummary.visibility = android.view.View.GONE
            binding.btnSubmit.isEnabled = false
        }
    }
    
    private fun validateAndSubmit() {
        // Validate all fields
        if (selectedDate == null) {
            Snackbar.make(binding.root, "Please select a date", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        if (selectedStartTime == null) {
            Snackbar.make(binding.root, "Please select start time", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        if (selectedEndTime == null) {
            Snackbar.make(binding.root, "Please select end time", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        if (selectedSlotNumber == null) {
            Snackbar.make(binding.root, "Please select a charging slot", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        val startDateTime = LocalDateTime.of(selectedDate!!, selectedStartTime!!)
        val endDateTime = LocalDateTime.of(selectedDate!!, selectedEndTime!!)
        
        // Validate end time is after start time
        if (!endDateTime.isAfter(startDateTime)) {
            Snackbar.make(binding.root, "End time must be after start time", Snackbar.LENGTH_LONG).show()
            return
        }
        
        // Validate booking is in the future
        if (startDateTime.isBefore(LocalDateTime.now())) {
            Snackbar.make(binding.root, "Cannot book in the past", Snackbar.LENGTH_LONG).show()
            return
        }
        
        // Calculate duration in hours
        val duration = ChronoUnit.HOURS.between(startDateTime, endDateTime).toInt()
        
        // Format datetime to ISO 8601 with Z suffix (UTC)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val reservationDateTime = startDateTime.format(formatter) + "Z"
        
        // Show progress
        binding.progressBooking.visibility = android.view.View.VISIBLE
        
        // Submit booking using new API format
        vm.createBooking(stationId, selectedSlotNumber!!, reservationDateTime, duration)
    }
    
    private fun setupObservers() {
        vm.result.observe(this) { res ->
            res?.let {
                binding.progressBooking.visibility = android.view.View.GONE
                Snackbar.make(
                    binding.root, 
                    "Reservation confirmed! ID: ${it.id}", 
                    Snackbar.LENGTH_LONG
                ).show()
                
                // Delay and finish
                binding.root.postDelayed({
                    finish()
                }, 2000)
            }
        }
        
        // Observer for new booking API response
        vm.bookingResult.observe(this) { bookingData ->
            bookingData?.let {
                binding.progressBooking.visibility = android.view.View.GONE
                
                // Show success dialog with pending status information
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("✅ Booking Requested")
                    .setMessage(
                        "Your booking request has been submitted successfully!\n\n" +
                        "📋 Booking ID: ${it.bookingId}\n" +
                        "📍 Station: ${it.stationId}\n" +
                        "🔌 Slot: ${it.slotNumber}\n\n" +
                        "⏳ Status: Pending Approval\n\n" +
                        "Your booking is now pending and waiting for the station operator to accept it. " +
                        "You can view your pending bookings in the 'My Bookings' section."
                    )
                    .setPositiveButton("View Pending Bookings") { _, _ ->
                        // Navigate to pending bookings
                        val intent = Intent(this, PendingBookingsActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("OK") { _, _ ->
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
        
        vm.error.observe(this) { msg ->
            binding.progressBooking.visibility = android.view.View.GONE
            msg?.let { 
                Snackbar.make(binding.root, "Error: $it", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getColor(android.R.color.holo_red_dark))
                    .show() 
            }
        }
    }
}