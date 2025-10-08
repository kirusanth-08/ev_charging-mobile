package com.example.evcharger.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.evcharger.R
import com.example.evcharger.databinding.FragmentStationDetailsBinding
import com.example.evcharger.model.Station
import com.example.evcharger.model.BackendSlot
import com.example.evcharger.ui.activities.SlotAdapter
import com.example.evcharger.ui.activities.ReservationFormActivity
import com.example.evcharger.auth.UserSessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import androidx.recyclerview.widget.LinearLayoutManager

class StationDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentStationDetailsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_STATION = "arg_station"
        fun newInstance(station: Station): StationDetailsBottomSheet {
            val b = Bundle()
            b.putParcelable(ARG_STATION, station)
            val f = StationDetailsBottomSheet()
            f.arguments = b
            return f
        }
    }

    private var station: Station? = null
    private var slotsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        station = arguments?.getParcelable(ARG_STATION, Station::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStationDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        station?.let { s ->
            // Set station name
            binding.txtStationName.text = s.name
            
            // Set status with color
            binding.txtStationStatus.text = when(s.status?.lowercase()) {
                "available" -> "Available Now"
                "busy" -> "Busy"
                "offline" -> "Offline"
                else -> s.status ?: "Unknown"
            }
            
            // Set address
            binding.txtStationAddress.text = s.address ?: "No address available"
            
            // Set distance
            s.distanceMeters?.let { distance ->
                binding.txtDistance.text = when {
                    distance < 1000 -> "${distance}m"
                    else -> String.format("%.1fkm", distance / 1000.0)
                }
            } ?: run {
                binding.txtDistance.visibility = View.GONE
            }
            
            // Add connector type chips
            binding.chipGroupConnectors.removeAllViews()
            s.connectorTypes.forEach { connectorType ->
                val chip = Chip(requireContext()).apply {
                    text = connectorType
                    isCheckable = false
                    setChipBackgroundColorResource(R.color.primary_light)
                    setTextColor(resources.getColor(R.color.text_primary, null))
                }
                binding.chipGroupConnectors.addView(chip)
            }
            
            // Set station details
            val details = buildString {
                s.chargingPowerKw?.let { 
                    append("⚡ Power: ${it}kW\n") 
                }
                append("📊 Status: ${s.status ?: "Unknown"}\n")
                s.lastUpdated?.let { 
                    append("🕒 Updated: ${it.take(10)}") 
                }
            }
            binding.txtStationDetails.text = details.trim()

            // Get user NIC from session
            val sessionManager = UserSessionManager(requireContext())
            val session = sessionManager.loadSession()
            val userNic = session.username ?: ""

            // Navigate button
            binding.btnNavigate.setOnClickListener {
                val uri = Uri.parse("google.navigation:q=${s.latitude},${s.longitude}&mode=d")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply { 
                    setPackage("com.google.android.apps.maps") 
                }
                if (intent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(intent)
                } else {
                    // Fallback to browser maps
                    val web = Intent(Intent.ACTION_VIEW, 
                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${s.latitude},${s.longitude}"))
                    startActivity(web)
                }
            }

            // Quick Reserve button - goes directly to reservation form
            binding.btnReserve.setOnClickListener {
                val intent = Intent(requireContext(), ReservationFormActivity::class.java).apply {
                    putExtra("NIC", userNic)
                    putExtra("stationId", s.id)
                    putExtra("StationId", s.id)
                    putExtra("StationName", s.name)
                    putExtra("StationAddress", s.address)
                }
                startActivity(intent)
                dismiss()
            }

            // View Slots button - shows available slots
            binding.btnViewSlots.setOnClickListener {
                toggleSlotsView(s, userNic)
            }
        }
    }

    private fun toggleSlotsView(station: Station, userNic: String) {
        slotsVisible = !slotsVisible
        
        if (slotsVisible) {
            // Show slots
            binding.cardSlots.visibility = View.VISIBLE
            binding.btnViewSlots.text = "Hide Slots"
            binding.btnViewSlots.icon = resources.getDrawable(R.drawable.ic_arrow_back, null)
            
            // Create mock slots from connector types
            // In a real app, you'd fetch actual slot availability from the API
            val backendSlots = station.connectorTypes.mapIndexed { idx, ct ->
                BackendSlot(
                    slotNumber = idx + 1,
                    connectorType = ct,
                    isAvailable = true, // Mock: assume all slots available
                    powerRating = station.chargingPowerKw ?: 50
                )
            }
            
            // Set up RecyclerView
            binding.rvSlots.layoutManager = LinearLayoutManager(requireContext())
            binding.rvSlots.adapter = SlotAdapter(backendSlots) { slot ->
                // When user selects a slot, launch reservation form with slot number
                val intent = Intent(requireContext(), ReservationFormActivity::class.java).apply {
                    putExtra("NIC", userNic)
                    putExtra("stationId", station.id)
                    putExtra("StationId", station.id)
                    putExtra("StationName", station.name)
                    putExtra("StationAddress", station.address)
                    putExtra("SlotNumber", slot.slotNumber)
                }
                startActivity(intent)
                dismiss()
            }
        } else {
            // Hide slots
            binding.cardSlots.visibility = View.GONE
            binding.btnViewSlots.text = "View Available Slots"
            binding.btnViewSlots.icon = resources.getDrawable(R.drawable.ic_charging_station, null)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
