package com.example.evcharger.ui.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.R
import com.example.evcharger.model.BackendSlot
import android.widget.TextView

class SlotAdapter(private val items: List<BackendSlot>, private val onClick: (BackendSlot) -> Unit) : RecyclerView.Adapter<SlotAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val txtSlot: TextView = view.findViewById(R.id.txtSlot)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_slot, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        holder.txtSlot.text = "Slot ${s.slotNumber} - ${s.connectorType ?: ""}"
        holder.txtStatus.text = if (s.isAvailable == true) "Free" else "Busy"
        holder.itemView.setOnClickListener { onClick(s) }
    }

    override fun getItemCount(): Int = items.size
}
