package com.example.evcharger.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.example.evcharger.databinding.ActivityPendingBookingsBinding
import com.example.evcharger.model.Reservation
import com.example.evcharger.repository.ReservationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PendingBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPendingBookingsBinding
    private val adapter = ReservationAdapter()
    private val repo = ReservationRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPendingBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadData() }

        loadData()
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            val res = repo.getPending()
            withContext(Dispatchers.Main) {
                binding.swipeRefresh.isRefreshing = false
                if (res.isSuccessful) {
                    adapter.submitList(res.body()?.data ?: emptyList<Reservation>())
                } else {
                    Snackbar.make(binding.root, res.body()?.message ?: "Failed to load pending", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}
