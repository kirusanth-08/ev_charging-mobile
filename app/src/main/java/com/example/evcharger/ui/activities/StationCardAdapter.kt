package com.example.evcharger.ui.activities

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.databinding.ItemStationCardBinding
import com.example.evcharger.model.BackendStationV2

class StationCardAdapter(
    private val items: List<BackendStationV2>
) : RecyclerView.Adapter<StationCardAdapter.VH>() {

    inner class VH(private val b: ItemStationCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(s: BackendStationV2) {
            b.txtCardStationName.text = s.name
            val total = s.slots.size
            val available = s.slots.count { it.isAvailable }
            b.txtCardStationMeta.text = "$total slots • $available available"
            b.root.setOnClickListener {
                val ctx = b.root.context
                val i = Intent(ctx, StationDetailActivity::class.java)
                i.putExtra("stationId", s.stationId)
                ctx.startActivity(i)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemStationCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
