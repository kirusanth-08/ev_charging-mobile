package com.example.evcharger.ui.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.databinding.ItemStationManageBinding
import com.example.evcharger.model.BackendStationV2

class StationsAdapter(
    private val items: List<BackendStationV2>,
    private val onToggle: (com.example.evcharger.model.BackendSlot, Boolean) -> Unit
) : RecyclerView.Adapter<StationsAdapter.VH>() {

    inner class VH(private val b: ItemStationManageBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(s: BackendStationV2) {
            b.txtStationNameManage.text = s.name
            b.txtStationAddrManage.text = s.location.address ?: ""
            val slots = s.slots
            b.rvStationSlots.layoutManager = LinearLayoutManager(b.root.context)
            b.rvStationSlots.adapter = SlotManageAdapter(slots, { slot, enabled -> onToggle(slot, enabled) })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemStationManageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
