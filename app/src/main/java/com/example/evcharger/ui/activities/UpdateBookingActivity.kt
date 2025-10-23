package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.android.material.snackbar.Snackbar
import com.example.evcharger.databinding.ActivityUpdateBookingBinding
import com.example.evcharger.model.BookingResponseData
import com.example.evcharger.repository.ReservationRepository
import com.example.evcharger.utils.StatusBarUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Activity to update a pending booking's ReservationDateTime and Duration
 */
class UpdateBookingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityUpdateBookingBinding
    private val repo = ReservationRepository()
    
    private lateinit var booking: BookingResponseData
    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    private var selectedDuration: Int? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set status bar color
        StatusBarUtil.setGreen(this)
        
        // Setup toolbar
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        // Get booking data from intent
        booking = BookingResponseData(
            bookingId = intent.getStringExtra("bookingId") ?: "",
            evOwnerNic = intent.getStringExtra("evOwnerNic"),
            stationId = intent.getStringExtra("stationId"),
            stationName = intent.getStringExtra("stationName"),
            stationLocation = intent.getStringExtra("stationLocation"),
            slotNumber = intent.getIntExtra("slotNumber", 0),
            reservationDateTime = intent.getStringExtra("reservationDateTime"),
            duration = intent.getIntExtra("duration", 0),
            status = intent.getStringExtra("status"),
            qrCode = null,
            energyConsumed = null,
            cost = null,
            cancelReason = null,
            approvedBy = null,
            approvedAt = null,
            confirmedAt = null,
            completedAt = null,
            cancelledAt = null,
            createdAt = null,
            updatedAt = null,
            canModify = null,
            canCancel = null,
            timeUntilReservation = null,
            isExpired = null
        )
        
        setupUI()
        displayCurrentDetails()
    }
    
    private fun setupUI() {
        // Date picker
        binding.btnPickDate.setOnClickListener {
            showDatePicker()
        }
        
        // Time picker
        binding.btnPickTime.setOnClickListener {
            showTimePicker()
        }
        
        // Duration input change listener
        binding.edtDuration.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val durationText = binding.edtDuration.text.toString()
                selectedDuration = durationText.toIntOrNull()
                updateButtonState()
            }
        }
        
        // Update button
        binding.btnUpdate.setOnClickListener {
            validateAndUpdate()
        }
    }
    
    private fun displayCurrentDetails() {
        // Display booking info
        binding.txtBookingId.text = "Booking ID: ${booking.bookingId}"
        binding.txtStationName.text = "Station: ${booking.stationName ?: "N/A"}"
        binding.txtSlotNumber.text = "Slot: ${booking.slotNumber}"
        
        // Parse and display current reservation details
        val currentDateTime = try {
            ZonedDateTime.parse(booking.reservationDateTime)
        } catch (e: Exception) {
            null
        }
        
        if (currentDateTime != null) {
            val formatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy 'at' hh:mm a")
            binding.txtCurrentDateTime.text = "Date and Time: ${currentDateTime.format(formatter)}"
        } else {
            binding.txtCurrentDateTime.text = "Date and Time: ${booking.reservationDateTime}"
        }
        
        binding.txtCurrentDuration.text = "Duration: ${booking.duration} hours"
        
        // Pre-fill duration
        binding.edtDuration.setText(booking.duration.toString())
        selectedDuration = booking.duration
    }
    
    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select New Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        
        picker.addOnPositiveButtonClickListener { millis ->
            selectedDate = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            
            val formatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")
            binding.btnPickDate.text = selectedDate?.format(formatter) ?: "Choose Date"
            
            updateSelectedDisplay()
            updateButtonState()
        }
        
        picker.show(supportFragmentManager, "date_picker")
    }
    
    private fun showTimePicker() {
        val currentTime = LocalTime.now()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentTime.hour)
            .setMinute(currentTime.minute)
            .setTitleText("Select New Time")
            .build()
        
        picker.addOnPositiveButtonClickListener {
            selectedTime = LocalTime.of(picker.hour, picker.minute)
            
            val formatter = DateTimeFormatter.ofPattern("hh:mm a")
            binding.btnPickTime.text = selectedTime?.format(formatter) ?: "Choose Time"
            
            updateSelectedDisplay()
            updateButtonState()
        }
        
        picker.show(supportFragmentManager, "time_picker")
    }
    
    private fun updateSelectedDisplay() {
        val date = selectedDate
        val time = selectedTime
        
        if (date != null && time != null) {
            val dateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy 'at' hh:mm a")
            val dateTime = LocalDateTime.of(date, time)
            binding.txtSelectedDateTime.text = "New: ${dateTime.format(dateTimeFormatter)}"
            binding.txtSelectedDateTime.visibility = android.view.View.VISIBLE
        } else {
            binding.txtSelectedDateTime.visibility = android.view.View.GONE
        }
    }
    
    private fun updateButtonState() {
        val hasDate = selectedDate != null
        val hasTime = selectedTime != null
        val hasDuration = selectedDuration != null && selectedDuration!! > 0
        
        binding.btnUpdate.isEnabled = hasDate && hasTime && hasDuration
    }
    
    private fun validateAndUpdate() {
        val date = selectedDate
        val time = selectedTime
        val duration = selectedDuration
        
        if (date == null || time == null || duration == null) {
            Snackbar.make(binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        if (duration <= 0) {
            Snackbar.make(binding.root, "Duration must be greater than 0", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        val newDateTime = LocalDateTime.of(date, time)
        
        // Validate booking is in the future
        if (newDateTime.isBefore(LocalDateTime.now())) {
            Snackbar.make(binding.root, "Cannot book in the past", Snackbar.LENGTH_LONG).show()
            return
        }
        
        // Format datetime to ISO 8601 with Z suffix (UTC)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val reservationDateTime = newDateTime.format(formatter) + "Z"
        
        // Show progress
        binding.progressUpdate.visibility = android.view.View.VISIBLE
        binding.btnUpdate.isEnabled = false
        
        // Update booking
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repo.updateBooking(booking.bookingId, reservationDateTime, duration)
                }
                
                binding.progressUpdate.visibility = android.view.View.GONE
                
                if (result.isSuccess) {
                    Snackbar.make(
                        binding.root,
                        "✅ Booking updated successfully!",
                        Snackbar.LENGTH_LONG
                    ).show()
                    
                    // Delay and finish
                    binding.root.postDelayed({
                        setResult(RESULT_OK)
                        finish()
                    }, 1500)
                } else {
                    binding.btnUpdate.isEnabled = true
                    Snackbar.make(
                        binding.root,
                        "Error: ${result.exceptionOrNull()?.message}",
                        Snackbar.LENGTH_LONG
                    ).setBackgroundTint(getColor(android.R.color.holo_red_dark))
                        .show()
                }
            } catch (e: Exception) {
                binding.progressUpdate.visibility = android.view.View.GONE
                binding.btnUpdate.isEnabled = true
                Snackbar.make(
                    binding.root,
                    "Error: ${e.localizedMessage}",
                    Snackbar.LENGTH_LONG
                ).setBackgroundTint(getColor(android.R.color.holo_red_dark))
                    .show()
            }
        }
    }
}
