package com.anemoventi.ranbing.util

import android.os.Build
import com.anemoventi.ranbing.data.UiStyle

/**
 * Resolves the preferred UI skin. "Auto" follows the platform design language:
 * HyperOS/MIUI devices get the MIUIx skin, everything else gets Material 3.
 */
object DeviceUi {

    private val isMiuiFamily: Boolean by lazy {
        val propVersion = runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val getter = clazz.getMethod("get", String::class.java)
            (getter.invoke(null, "ro.miui.ui.version.name") as? String).orEmpty()
        }.getOrDefault("")

        if (propVersion.isNotBlank()) return@lazy true

        // Fallback when the hidden system properties are not readable.
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        (manufacturer in MI_FAMILY || brand in MI_FAMILY) && Build.VERSION.SDK_INT >= 29
    }

    fun resolve(style: UiStyle): Boolean = when (style) {
        UiStyle.AUTO -> isMiuiFamily
        UiStyle.MATERIAL3 -> false
        UiStyle.MIUIX -> true
    }

    private val MI_FAMILY = setOf("xiaomi", "redmi", "poco")
}
