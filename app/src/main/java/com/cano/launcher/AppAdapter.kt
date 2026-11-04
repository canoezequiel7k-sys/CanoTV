package com.cano.launcher

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cano.launcher.databinding.ItemAppBinding

class AppAdapter(
    private val onAppSelected: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    private val items = mutableListOf<AppEntry>()
    private var selectedPosition: Int = RecyclerView.NO_POSITION

    fun submitItems(newItems: List<AppEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
        if (items.isNotEmpty()) {
            selectedPosition = 0
            notifyItemChanged(0)
        } else {
            selectedPosition = RecyclerView.NO_POSITION
        }
    }

    fun moveSelection(delta: Int) {
        if (items.isEmpty()) return
        val next = (selectedPosition + delta).coerceIn(0, items.lastIndex)
        if (next == selectedPosition) return
        val previous = selectedPosition
        selectedPosition = next
        if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous)
        notifyItemChanged(next)
    }

    fun getSelectedPosition(): Int = selectedPosition

    fun getSelectedApp(): AppEntry? {
        if (selectedPosition == RecyclerView.NO_POSITION || selectedPosition >= items.size) return null
        return items[selectedPosition]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemAppBinding.inflate(inflater, parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = items[position]
        holder.bind(app, position == selectedPosition)
    }

    override fun getItemCount(): Int = items.size

    inner class AppViewHolder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppEntry, isSelected: Boolean) {
            binding.appIcon.setImageDrawable(app.icon)
            binding.appLabel.text = app.label
            binding.root.isActivated = isSelected
            binding.root.setOnClickListener { onAppSelected(app) }
        }
    }
}
