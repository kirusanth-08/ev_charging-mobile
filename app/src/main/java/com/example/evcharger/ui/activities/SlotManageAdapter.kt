package com.example.evcharger.ui.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.databinding.ItemSlotBinding
import com.example.evcharger.model.BackendSlot

class SlotManageAdapter(
    private val items: List<BackendSlot>,
    private val onToggle: (BackendSlot, Boolean) -> Unit
) : RecyclerView.Adapter<SlotManageAdapter.VH>() {

    inner class VH(private val b: ItemSlotBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(s: BackendSlot) {
            b.txtSlot.text = "Slot ${s.slotNumber} - ${s.connectorType ?: "N/A"}"
            b.txtStatus.text = if (s.isAvailable) "Free" else "Occupied"
            // Replace status TextView with a click to toggle (simple behavior)
            b.root.setOnClickListener {
                val new = !s.isAvailable
                onToggle(s, new)
                b.txtStatus.text = if (new) "Free" else "Occupied"
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
}
