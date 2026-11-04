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
        val selected = items.getOrNull(selectedPosition)
        items.clear()
        items.addAll(newItems)
        selectedPosition = selected?.let { previous ->
            items.indexOfFirst {
                it.packageName == previous.packageName && it.activityName == previous.activityName
            }.takeIf { it >= 0 }
        } ?: if (items.isNotEmpty()) 0 else RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int = selectedPosition

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
            binding.appIcon.setImageDrawable(app.banner ?: app.icon)
            binding.appIcon.scaleType = if (app.banner != null) {
                android.widget.ImageView.ScaleType.CENTER_CROP
            } else {
                android.widget.ImageView.ScaleType.CENTER_INSIDE
            }
            binding.appLabel.text = app.label
            binding.root.isActivated = isSelected
            binding.root.setOnClickListener { onAppSelected(app) }
            binding.root.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) selectedPosition = position
                } else {
                    binding.root.isActivated = false
                }
                if (hasFocus) binding.root.isActivated = true
            }
            binding.root.setOnKeyListener { _, keyCode, event ->
                event.action == android.view.KeyEvent.ACTION_DOWN &&
                    (keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP ||
                        keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN)
            }
        }
    }
}
