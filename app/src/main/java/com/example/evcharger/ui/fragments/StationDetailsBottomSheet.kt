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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        station = arguments?.getParcelable(ARG_STATION)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentStationDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        station?.let { s ->
            binding.txtStationName.text = s.name
            binding.txtStationAddress.text = s.address ?: ""
            val details = buildString {
                s.connectorTypes.takeIf { it.isNotEmpty() }?.let { append("Connectors: ${it.joinToString()}") }
                s.chargingPowerKw?.let { if (isNotEmpty()) append(" • ") ; append("Power: ${it}kW") }
                s.status?.let { if (isNotEmpty()) append(" • ") ; append("Status: $it") }
                s.distanceMeters?.let { if (isNotEmpty()) append(" • ") ; append("${it}m away") }
            }
            binding.txtStationDetails.text = details

            binding.btnNavigate.setOnClickListener {
                val uri = Uri.parse("google.navigation:q=${s.latitude},${s.longitude}&mode=d")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
                if (intent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(intent)
                } else {
                    // fallback to browser maps
                    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${s.latitude},${s.longitude}"))
                    startActivity(web)
                }
            }

            binding.btnReserve.setOnClickListener {
                // Launch ReservationFormActivity with station id
                val i = Intent(requireContext(), com.example.evcharger.ui.activities.ReservationFormActivity::class.java)
                i.putExtra("NIC", "") // keeper: NIC should be filled from user session or login
                i.putExtra("stationId", s.id)
                startActivity(i)
                dismiss()
            }

            binding.btnViewSlots.setOnClickListener {
                // Convert station connector info into BackendSlot objects to show inside this bottom sheet
                val backendSlots = s.connectorTypes.mapIndexed { idx, ct ->
                    com.example.evcharger.model.BackendSlot(
                        slotNumber = idx + 1,
                        connectorType = ct,
                        isAvailable = true,
                        powerRating = s.chargingPowerKw ?: 0
                    )
                }

                // populate rvSlots inside this fragment and make it visible
                val rv = binding.root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSlots)
                rv?.let {
                    it.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
                    it.adapter = com.example.evcharger.ui.activities.SlotAdapter(backendSlots) { slot ->
                        // launch reservation form with selected slot
                        val i = Intent(requireContext(), com.example.evcharger.ui.activities.ReservationFormActivity::class.java)
                        i.putExtra("NIC", "")
                        i.putExtra("stationId", s.id)
                        i.putExtra("SlotNumber", slot.slotNumber)
                        startActivity(i)
                        dismiss()
                    }
                    it.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
