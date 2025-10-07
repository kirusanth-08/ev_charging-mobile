package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.evcharger.databinding.ActivityManageStationBinding
import com.example.evcharger.model.BackendSlot
import com.example.evcharger.repository.ReservationRepository
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageStationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageStationBinding
    private val repo = ReservationRepository()
    private val stationRepo = com.example.evcharger.repository.StationRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageStationBinding.inflate(layoutInflater)
        setContentView(binding.root)

    binding.rvStations.layoutManager = LinearLayoutManager(this)

        // Load stations assigned to operator (requires JWT token set in Retrofit)
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { stationRepo.getOperatorStations() }
                if (res.isSuccessful) {
                    val stations = res.body()?.data ?: emptyList()
                    val adapter = StationsAdapter(stations) { slot, enabled ->
                        // Find station id for this slot (slots belong to stations) — we must search stations list
                        val station = stations.find { it.slots.any { st -> st.slotNumber == slot.slotNumber } }
                        val sid = station?.stationId
                        if (sid.isNullOrEmpty()) {
                            Snackbar.make(binding.root, "Station not found for slot", Snackbar.LENGTH_LONG).show()
                            return@StationsAdapter
                        }

                        // Perform network update in lifecycleScope
                        lifecycleScope.launch {
                            try {
                                val r = withContext(Dispatchers.IO) { stationRepo.updateSlotAvailability(sid, slot.slotNumber, enabled) }
                                if (r.isSuccessful) {
                                    // update UI in adapter
                                    // StationsAdapter uses nested SlotManageAdapter; we can refresh the whole list for simplicity
                                    binding.rvStations.adapter = StationsAdapter(stations) { s2, en2 -> /* no-op for brevity */ }
                                    Snackbar.make(binding.root, "Slot ${slot.slotNumber} updated", Snackbar.LENGTH_SHORT).show()
                                } else {
                                    Snackbar.make(binding.root, "Failed to update slot", Snackbar.LENGTH_LONG).show()
                                }
                            } catch (t: Throwable) {
                                Snackbar.make(binding.root, t.localizedMessage ?: "Error", Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                    binding.rvStations.adapter = adapter
                } else {
                    Snackbar.make(binding.root, "Failed to load operator stations", Snackbar.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                Snackbar.make(binding.root, t.localizedMessage ?: "Error", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
