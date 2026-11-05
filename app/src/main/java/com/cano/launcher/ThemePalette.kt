package com.cano.launcher

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.StateSet
import androidx.annotation.DrawableRes

data class ThemePalette(
    val id: String,
    @DrawableRes val backgroundRes: Int,
    val canvas: Int,
    val sidebar: Int,
    val surface: Int,
    val surfaceSelected: Int,
    val border: Int,
    val accent: Int,
    val text: Int,
    val muted: Int,
    val overlay: Int,
    val gridBackground: Int,
    private val density: Float = 1f
) {
    fun navigationBackground(): StateListDrawable = StateListDrawable().apply {
        addState(
            intArrayOf(android.R.attr.state_focused),
            shape(surfaceSelected, accent, 2, 12)
        )
        addState(
            intArrayOf(android.R.attr.state_selected),
            shape(surfaceSelected, surfaceSelected, 0, 12)
        )
        addState(StateSet.WILD_CARD, shape(android.graphics.Color.TRANSPARENT, 0, 0, 12))
    }

    fun appCardBackground(): StateListDrawable = StateListDrawable().apply {
        addState(
            intArrayOf(android.R.attr.state_focused),
            shape(withAlpha(surfaceSelected, 0xF2), accent, 3, 14)
        )
        addState(
            intArrayOf(android.R.attr.state_activated),
            shape(withAlpha(surface, 0xE8), accent, 2, 12)
        )
        addState(StateSet.WILD_CARD, shape(withAlpha(surface, 0xD9), border, 1, 12))
    }

    fun settingsOptionBackground(showChecked: Boolean = false): StateListDrawable =
        StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_focused),
                shape(withAlpha(surfaceSelected, 0xF2), accent, 2, 8)
            )
            if (showChecked) {
                addState(
                    intArrayOf(android.R.attr.state_checked),
                    shape(withAlpha(surface, 0xE8), accent, 1, 8)
                )
            }
            addState(StateSet.WILD_CARD, shape(android.graphics.Color.TRANSPARENT, 0, 0, 8))
        }

    fun infoCardBackground() = shape(surface, border, 1, 10)

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha shl 24)

    private fun shape(color: Int, strokeColor: Int, strokeWidth: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius * density
            if (strokeWidth > 0) setStroke(strokeWidth.dp, strokeColor)
        }

    private val Int.dp: Int
        get() = (this * density).toInt()

    companion object {
        const val PURPLE = "purple"
        const val SAGE = "sage"
        const val BLUE_GOLD = "blue_gold"
        const val RED_GRAY = "red_gray"
        const val DARK_GRAY = "dark_gray"

        fun forId(id: String, density: Float = 1f): ThemePalette {
            val palette = when (id) {
                SAGE -> sage
                BLUE_GOLD -> blueGold
                RED_GRAY -> redGray
                DARK_GRAY -> darkGray
                else -> purple
            }
            return palette.copy(density = density)
        }

        private val purple = ThemePalette(
            PURPLE, R.drawable.theme_purple, 0xFF080D1C.toInt(), 0xFF101426.toInt(),
            0xFF19182A.toInt(), 0xFF422345.toInt(), 0xFF61405F.toInt(),
            0xFFE36AB1.toInt(), 0xFFF8F3FC.toInt(), 0xFFE0D4E8.toInt(), 0x52050915,
            0xFF282431.toInt()
        )
        private val sage = ThemePalette(
            SAGE, R.drawable.theme_sage, 0xFF111A17.toInt(), 0xFF14211B.toInt(),
            0xFF1B2921.toInt(), 0xFF354D39.toInt(), 0xFF557354.toInt(),
            0xFFB6CE83.toInt(), 0xFFF5F6EC.toInt(), 0xFFDCE6D1.toInt(), 0x9907110C.toInt(),
            0xFF26312A.toInt()
        )
        private val blueGold = ThemePalette(
            BLUE_GOLD, R.drawable.theme_bluegold, 0xFF07111F.toInt(), 0xFF0B1728.toInt(),
            0xFF14243A.toInt(), 0xFF263B55.toInt(), 0xFF425974.toInt(),
            0xFFE8B85A.toInt(), 0xFFF2F5FB.toInt(), 0xFFD0DCEB.toInt(), 0x50030A18,
            0xFF202A37.toInt()
        )
        private val redGray = ThemePalette(
            RED_GRAY, R.drawable.theme_redgray, 0xFF171719.toInt(), 0xFF202023.toInt(),
            0xFF2C2C30.toInt(), 0xFF484044.toInt(), 0xFF68666A.toInt(),
            0xFFE54B54.toInt(), 0xFFF7F5F5.toInt(), 0xFFD0CDCF.toInt(), 0x710D080A,
            0xFF353438.toInt()
        )
        private val darkGray = ThemePalette(
            DARK_GRAY, R.drawable.theme_darkgray, 0xFF17191C.toInt(), 0xFF1B1D20.toInt(),
            0xFF292C30.toInt(), 0xFF454A50.toInt(), 0xFF71767C.toInt(),
            0xFFD5D9DD.toInt(), 0xFFF4F5F6.toInt(), 0xFFD2D5D8.toInt(), 0x5005090D,
            0xFF303338.toInt()
        )
    }
}
