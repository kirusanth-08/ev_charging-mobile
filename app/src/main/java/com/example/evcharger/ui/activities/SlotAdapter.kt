package com.example.evcharger.ui.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.R
import com.example.evcharger.model.BackendSlot
import android.widget.TextView

class SlotAdapter(private val items: List<BackendSlot>, private val onClick: (BackendSlot) -> Unit) : RecyclerView.Adapter<SlotAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val txtSlot: TextView = view.findViewById(R.id.txtSlot)
        val txtSlotDetails: TextView = view.findViewById(R.id.txtSlotDetails)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        val badgeStatus: LinearLayout = view.findViewById(R.id.badgeStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_slot, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        val context = holder.itemView.context
        
        // Set slot number
        holder.txtSlot.text = "Slot ${s.slotNumber}"
        
        // Set slot details
        val details = buildString {
            append(s.connectorType)
            append(" • ${s.powerRating}kW")
        }
        holder.txtSlotDetails.text = details
        
        // Set status badge
        if (s.isAvailable) {
            holder.txtStatus.text = "Available"
            holder.badgeStatus.setBackgroundColor(context.getColor(R.color.ev_available))
        } else {
            holder.txtStatus.text = "Busy"
            holder.badgeStatus.setBackgroundColor(context.getColor(R.color.ev_busy))
        }
        
        // Click listener
        holder.itemView.setOnClickListener { onClick(s) }
    }

    override fun getItemCount(): Int = items.size
}
