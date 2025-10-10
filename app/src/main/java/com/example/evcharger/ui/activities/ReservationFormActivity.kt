package com.example.evcharger.ui.activities

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        loadIntentData()
        
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
        // For now, create mock slots - in production, fetch from API based on station
        availableSlots.clear()
        availableSlots.addAll(listOf(
            BackendSlot(1, true, 50, "Type 2"),
            BackendSlot(2, true, 50, "Type 2"),
            BackendSlot(3, true, 22, "Type 1"),
            BackendSlot(4, true, 150, "CCS"),
            BackendSlot(5, false, 50, "Type 2")
        ))
        
        setupSlotSpinner()
    }
    
    private fun setupSlotSpinner() {
        val slotOptions = availableSlots.map { slot ->
            "Slot ${slot.slotNumber} - ${slot.connectorType} (${slot.powerRating}kW) ${if (slot.isAvailable) "Available" else "Unavailable"}"
        }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, slotOptions)
        binding.spinnerSlot.setAdapter(adapter)
        
        binding.spinnerSlot.setOnItemClickListener { parent, view, position, id ->
            val slot = availableSlots[position]
            selectedSlotNumber = slot.slotNumber
            
            // Show slot details
            binding.layoutSlotDetails.visibility = android.view.View.VISIBLE
            binding.txtSlotDetails.text = "Connector: ${slot.connectorType} • Power: ${slot.powerRating}kW • " +
                    "Status: ${if (slot.isAvailable) "Available" else "Occupied"}"
            
            // Update summary
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
        
        // Submit reservation
        vm.create(nic, stationId, startDateTime)
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