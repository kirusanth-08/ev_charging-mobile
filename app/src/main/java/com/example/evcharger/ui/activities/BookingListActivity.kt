package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.evcharger.databinding.ActivityBookingListBinding
import com.example.evcharger.model.Reservation
import com.example.evcharger.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shows upcoming and past bookings in a list (RecyclerView).
 */
class BookingListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingListBinding
    private lateinit var nic: String
    private val adapter = ReservationAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nic = intent.getStringExtra("NIC") ?: ""

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val upcoming = RetrofitClient.api.getUpcoming(nic)
            val history = RetrofitClient.api.getHistory(nic)
            withContext(Dispatchers.Main) {
                if (upcoming.isSuccessful && history.isSuccessful) {
                    val list = mutableListOf<Reservation>()
                    upcoming.body()?.data?.let { list.addAll(it) }
                    history.body()?.data?.let { list.addAll(it) }
                    adapter.submitList(list)
                } else {
                    Snackbar.make(binding.root, "Failed to load bookings", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}