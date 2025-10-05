package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.example.evcharger.databinding.ActivityReservationFormBinding
import com.example.evcharger.viewmodel.ReservationViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Create/Modify reservation form.
 * Includes date/time picking and server submit.
 */
class ReservationFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservationFormBinding
    private val vm: ReservationViewModel by viewModels()
    private lateinit var nic: String
    private var stationId: String = "station-1" // placeholder
    private var pickedDateTime: LocalDateTime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

    // Read NIC from intent if provided, otherwise try to load from secure session
    val intentNic = intent.getStringExtra("NIC")
    val sessionNic = com.example.evcharger.auth.UserSessionManager(this).loadSession().username
    nic = intentNic ?: sessionNic ?: ""
    // Read stationId from intent (passed by StationDetailsBottomSheet)
    stationId = intent.getStringExtra("stationId") ?: stationId

        binding.btnPickDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().build()
            picker.addOnPositiveButtonClickListener { millis ->
                val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                pickedDateTime = dt.withHour(10).withMinute(0) // example hour selection
                binding.txtPicked.text = pickedDateTime.toString()
            }
            picker.show(supportFragmentManager, "date")
        }

        binding.btnSubmit.setOnClickListener {
            val dt = pickedDateTime
            if (dt == null) {
                Snackbar.make(binding.root, "Pick date/time", Snackbar.LENGTH_SHORT).show()
            } else {
                vm.create(nic, stationId, dt)
            }
        }

        vm.result.observe(this) { res ->
            res?.let {
                Snackbar.make(binding.root, "Reservation created: ${it.id}", Snackbar.LENGTH_LONG).show()
                // Navigate to a summary screen with QR if needed
            }
        }
        vm.error.observe(this) { msg ->
            msg?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }
    }
}