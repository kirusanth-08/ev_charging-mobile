package com.example.evcharger.ui.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.databinding.ItemSlotBinding
import com.example.evcharger.model.BackendSlot
import com.example.evcharger.repository.StationRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlotManageAdapter(
    items: List<BackendSlot>,
    private val onToggle: (BackendSlot, Boolean) -> Unit
) : RecyclerView.Adapter<SlotManageAdapter.VH>() {

    // Keep a mutable copy so we can update individual slot states and notify changes
    private val items: MutableList<BackendSlot> = items.toMutableList()

    inner class VH(private val b: ItemSlotBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(s: BackendSlot) {
            b.root.isEnabled = true
            b.txtSlot.text = "Slot ${s.slotNumber} - ${s.connectorType ?: "N/A"}"
            b.txtStatus.text = if (s.isAvailable) "Free" else "Occupied"

            b.root.setOnClickListener {
                // Disable briefly to avoid rapid double-clicks; activity will update adapter when network completes
                b.root.isEnabled = false
                val new = !s.isAvailable
                onToggle(s, new)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    // Called by the hosting Activity when a server update completes successfully
    fun updateSlotAvailability(slotNumber: Int, isAvailable: Boolean) {
        val idx = items.indexOfFirst { it.slotNumber == slotNumber }
        if (idx >= 0) {
            val old = items[idx]
            items[idx] = old.copy(isAvailable = isAvailable)
            notifyItemChanged(idx)
        }
    }
}
