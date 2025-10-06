package com.example.evcharger.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.evcharger.databinding.FragmentUpcomingBookingBinding
import com.example.evcharger.repository.ReservationRepository
import com.example.evcharger.ui.activities.BookingListActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpcomingBookingFragment : Fragment() {

    private var _binding: FragmentUpcomingBookingBinding? = null
    private val binding get() = _binding!!
    private val repo = ReservationRepository()
    private var nic: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nic = arguments?.getString("NIC") ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentUpcomingBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUpcoming()
        binding.btnManageBooking.setOnClickListener {
            startActivity(Intent(requireContext(), BookingListActivity::class.java).putExtra("NIC", nic))
        }
    }

    private fun loadUpcoming() {
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { repo.getUpcoming(nic) }
                if (res.isSuccessful) {
                    val list = res.body()?.data
                    if (!list.isNullOrEmpty()) {
                        val next = list.first()
                        binding.txtUpcomingContent.text = "Station: ${'$'}{next.stationName}\nStart: ${'$'}{next.startTime}"
                    } else {
                        binding.txtUpcomingContent.text = "No upcoming bookings"
                    }
                } else {
                    binding.txtUpcomingContent.text = "Failed to load"
                }
            } catch (t: Throwable) {
                binding.txtUpcomingContent.text = t.localizedMessage ?: "Error"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(nic: String): UpcomingBookingFragment {
            val f = UpcomingBookingFragment()
            val b = Bundle()
            b.putString("NIC", nic)
            f.arguments = b
            return f
        }
    }
}
