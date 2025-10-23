package com.example.evcharger.ui.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.evcharger.R
import com.example.evcharger.databinding.ItemStationSlotBinding
import com.example.evcharger.model.BackendSlot

/**
 * Adapter for displaying charging station slots
 */
class StationSlotsAdapter : ListAdapter<BackendSlot, StationSlotsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStationSlotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemStationSlotBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(slot: BackendSlot) {
            binding.tvSlotNumber.text = binding.root.context.getString(
                R.string.slot_format,
                slot.slotNumber
            )
            
            // Build slot details string
            val details = buildString {
                if (!slot.connectorType.isNullOrEmpty()) {
                    append(slot.connectorType)
                }
                if (slot.powerRating != null) {
                    if (isNotEmpty()) append(" • ")
                    append("${slot.powerRating} kW")
                }
            }
            
            binding.tvSlotDetails.text = details.ifEmpty { 
                binding.root.context.getString(R.string.not_specified) 
            }
            
            // Set availability badge
            if (slot.isAvailable) {
                binding.tvAvailability.text = binding.root.context.getString(R.string.available)
                binding.badgeAvailability.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.success)
                )
            } else {
                binding.tvAvailability.text = binding.root.context.getString(R.string.unavailable)
                binding.badgeAvailability.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.error)
                )
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<BackendSlot>() {
        override fun areItemsTheSame(oldItem: BackendSlot, newItem: BackendSlot): Boolean {
            return oldItem.slotNumber == newItem.slotNumber
        }

        override fun areContentsTheSame(oldItem: BackendSlot, newItem: BackendSlot): Boolean {
            return oldItem == newItem
        }
    }
}
