package com.cano.launcher

import android.view.LayoutInflater
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cano.launcher.databinding.ItemAppBinding

class AppAdapter(
    private val onAppSelected: (AppEntry) -> Unit,
    private val onFavoriteToggle: (AppEntry) -> Boolean,
    private var palette: ThemePalette
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    private val items = mutableListOf<AppEntry>()
    private var favorites = emptySet<String>()
    private var selectedPosition: Int = RecyclerView.NO_POSITION

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

    fun setPalette(palette: ThemePalette) {
        this.palette = palette
        notifyDataSetChanged()
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
        private var longPressTriggered = false
        private var keyDownTime = 0L

        fun bind(app: AppEntry, isSelected: Boolean) {
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
            binding.root.setOnKeyListener { view, keyCode, event ->
                if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER &&
                    keyCode != KeyEvent.KEYCODE_ENTER &&
                    keyCode != KeyEvent.KEYCODE_MENU &&
                    keyCode != KeyEvent.KEYCODE_STAR
                ) {
                    return@setOnKeyListener false
                }
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            keyDownTime = event.downTime
                            longPressTriggered = false
                        }
                        true
                    }
                    KeyEvent.ACTION_UP -> {
                        val duration = event.eventTime - keyDownTime
                        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_STAR ||
                            duration >= ViewConfiguration.getLongPressTimeout()
                        ) {
                            onFavoriteToggle(app)
                            longPressTriggered = true
                        } else if (!longPressTriggered) {
                            view.performClick()
                        }
                        true
                    }
                    else -> false
                }
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
