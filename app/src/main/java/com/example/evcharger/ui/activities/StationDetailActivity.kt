package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.evcharger.databinding.ActivityStationDetailBinding
import com.example.evcharger.repository.StationRepository
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStationDetailBinding
    private val stationRepo = StationRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val stationId = intent.getStringExtra("stationId")
        if (stationId == null) {
            finish()
            return
        }

        binding.rvStationDetailSlots.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { stationRepo.getOperatorStations() }
                if (res.isSuccessful) {
                    val stations = res.body()?.data ?: emptyList()
                    val station = stations.find { it.stationId == stationId }
                    if (station != null) {
                        binding.txtStationDetailName.text = station.name
                        val total = station.slots.size
                        val available = station.slots.count { it.isAvailable }
                        binding.txtStationDetailMeta.text = "$total slots • $available available"
                        var slotAdapter: SlotManageAdapter? = null
                        val adapter = SlotManageAdapter(station.slots) { slot, enabled ->
                            lifecycleScope.launch {
                                try {
                                    val r = withContext(Dispatchers.IO) { stationRepo.updateSlotAvailability(station.stationId, slot.slotNumber, enabled) }
                                    if (r.isSuccessful) {
                                        slotAdapter?.updateSlotAvailability(slot.slotNumber, enabled)
                                        Snackbar.make(binding.root, "Slot ${slot.slotNumber} updated", Snackbar.LENGTH_SHORT).show()
                                    } else {
                                        Snackbar.make(binding.root, "Failed to update slot", Snackbar.LENGTH_LONG).show()
                                    }
                                } catch (t: Throwable) {
                                    Snackbar.make(binding.root, t.localizedMessage ?: "Error", Snackbar.LENGTH_LONG).show()
                                }
                            }
                        }
                        slotAdapter = adapter
                        binding.rvStationDetailSlots.adapter = adapter
                    } else {
                        Snackbar.make(binding.root, "Station not found", Snackbar.LENGTH_LONG).show()
                        finish()
                    }
                } else {
                    Snackbar.make(binding.root, "Failed to load stations", Snackbar.LENGTH_LONG).show()
                    finish()
                }
            } catch (t: Throwable) {
                Snackbar.make(binding.root, t.localizedMessage ?: "Error", Snackbar.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
