package com.cano.launcher

import android.view.LayoutInflater
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cano.launcher.databinding.ItemAppBinding

class AppAdapter(
    private val onAppSelected: (AppEntry) -> Unit,
    private val onFavoriteToggle: (AppEntry) -> Boolean,
    private val onGridKey: (KeyEvent) -> Boolean,
    private var palette: ThemePalette
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    private val items = mutableListOf<AppEntry>()
    private var favorites = emptySet<String>()
    private var selectedPosition: Int = RecyclerView.NO_POSITION
    private var horizontalItems = false

    fun submitItems(newItems: List<AppEntry>, favoriteKeys: Set<String>) {
        val selected = items.getOrNull(selectedPosition)
        items.clear()
        items.addAll(newItems)
        favorites = favoriteKeys
        selectedPosition = selected?.let { previous ->
            items.indexOfFirst {
                it.packageName == previous.packageName && it.activityName == previous.activityName
            }.takeIf { it >= 0 }
        } ?: if (items.isNotEmpty()) 0 else RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun getSelectedPosition(): Int = selectedPosition

    fun selectPosition(position: Int) {
        if (position !in items.indices || position == selectedPosition) return
        val previous = selectedPosition
        selectedPosition = position
        if (previous in items.indices) notifyItemChanged(previous)
        notifyItemChanged(position)
    }

    fun getItem(position: Int): AppEntry? = items.getOrNull(position)

    fun setPalette(palette: ThemePalette) {
        this.palette = palette
        notifyDataSetChanged()
    }

    fun setHorizontalItems(horizontal: Boolean) {
        if (horizontalItems == horizontal) return
        horizontalItems = horizontal
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemAppBinding.inflate(inflater, parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = items[position]
        holder.bind(app, position == selectedPosition, horizontalItems)
    }

    override fun getItemCount(): Int = items.size

    inner class AppViewHolder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppEntry, isSelected: Boolean, horizontal: Boolean) {
            val density = binding.root.resources.displayMetrics.density
            binding.root.layoutParams = binding.root.layoutParams.apply {
                width = if (horizontal) {
                    (120 * density).toInt()
                } else {
                    ViewGroup.LayoutParams.MATCH_PARENT
                }
                height = ((if (horizontal) 112 else 148) * density).toInt()
            }
            binding.appIcon.layoutParams = binding.appIcon.layoutParams.apply {
                width = ((if (horizontal) 64 else 80) * density).toInt()
                height = ((if (horizontal) 64 else 80) * density).toInt()
            }
            binding.appLabel.layoutParams = (binding.appLabel.layoutParams as ViewGroup.MarginLayoutParams)
                .apply {
                    topMargin = ((if (horizontal) 4 else 8) * density).toInt()
                }
            binding.appLabel.textSize = if (horizontal) 16f else 18f
            binding.appIcon.setImageDrawable(app.banner ?: app.icon)
            binding.appIcon.scaleType = if (app.banner != null) {
                android.widget.ImageView.ScaleType.CENTER_CROP
            } else {
                android.widget.ImageView.ScaleType.CENTER_INSIDE
            }
            val favoriteKey = "${app.packageName}/${app.activityName}"
            binding.appLabel.text = if (favoriteKey in favorites) "★ ${app.label}" else app.label
            binding.root.isActivated = isSelected
            binding.root.background = palette.appCardBackground()
            binding.appLabel.setTextColor(palette.text)
            binding.root.setOnClickListener { onAppSelected(app) }
            binding.root.setOnLongClickListener {
                onFavoriteToggle(app)
                true
            }
            binding.root.setOnKeyListener { _, _, event ->
                onGridKey(event)
            }
            binding.root.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) selectedPosition = position
                } else {
                    binding.root.isActivated = false
                }
                if (hasFocus) binding.root.isActivated = true
            }
        }
    }
}
