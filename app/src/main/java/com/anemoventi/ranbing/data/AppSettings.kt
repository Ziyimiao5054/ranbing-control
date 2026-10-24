package com.anemoventi.ranbing.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UiStyle { AUTO, MATERIAL3, MIUIX }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class SettingsState(
    val uiStyle: UiStyle = UiStyle.AUTO,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accentColor: Long = 0xFF287AF9,
    val lastDeviceId: String? = null,
    val lastDeviceName: String? = null,
    val lastDeviceModel: String? = null,
    val lastDeviceFirmware: String? = null,
    val experimentalOverclock: Boolean = false,
)

/**
 * Tiny SharedPreferences backed settings store. Kept deliberately small: the app owns its
 * preferences, there is no account or sync.
 */
class AppSettings(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private fun load(): SettingsState = SettingsState(
        uiStyle = prefs.getString(KEY_UI_STYLE, null).toEnumOr(UiStyle.AUTO),
        themeMode = prefs.getString(KEY_THEME_MODE, null).toEnumOr(ThemeMode.SYSTEM),
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
        accentColor = prefs.getLong(KEY_ACCENT_COLOR, 0xFF287AF9),
        lastDeviceId = prefs.getString(KEY_LAST_DEVICE_ID, null),
        lastDeviceName = prefs.getString(KEY_LAST_DEVICE_NAME, null),
        lastDeviceModel = prefs.getString(KEY_LAST_DEVICE_MODEL, null),
        lastDeviceFirmware = prefs.getString(KEY_LAST_DEVICE_FIRMWARE, null),
        experimentalOverclock = prefs.getBoolean(KEY_EXPERIMENTAL_OVERCLOCK, false),
    )

    fun setUiStyle(value: UiStyle) = mutate { it.copy(uiStyle = value) }

    fun setThemeMode(value: ThemeMode) = mutate { it.copy(themeMode = value) }

    fun setDynamicColor(value: Boolean) = mutate { it.copy(dynamicColor = value) }

    fun setAccentColor(value: Long) = mutate { it.copy(accentColor = value) }

    fun setExperimentalOverclock(value: Boolean) = mutate { it.copy(experimentalOverclock = value) }

    fun setLastDevice(id: String?, name: String?, model: String? = null, firmware: String? = null) = mutate {
        it.copy(
            lastDeviceId = id,
            lastDeviceName = name,
            lastDeviceModel = model,
            lastDeviceFirmware = firmware,
        )
    }

    private fun mutate(block: (SettingsState) -> SettingsState) {
        val next = block(_state.value)
        prefs.edit()
            .putString(KEY_UI_STYLE, next.uiStyle.name)
            .putString(KEY_THEME_MODE, next.themeMode.name)
            .putBoolean(KEY_DYNAMIC_COLOR, next.dynamicColor)
            .putLong(KEY_ACCENT_COLOR, next.accentColor)
            .putString(KEY_LAST_DEVICE_ID, next.lastDeviceId)
            .putString(KEY_LAST_DEVICE_NAME, next.lastDeviceName)
            .putString(KEY_LAST_DEVICE_MODEL, next.lastDeviceModel)
            .putString(KEY_LAST_DEVICE_FIRMWARE, next.lastDeviceFirmware)
            .putBoolean(KEY_EXPERIMENTAL_OVERCLOCK, next.experimentalOverclock)
            .apply()
        _state.value = next
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOr(fallback: T): T =
        this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    private companion object {
        const val PREFS = "app_settings"
        const val KEY_UI_STYLE = "ui_style"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_ACCENT_COLOR = "accent_color"
        const val KEY_LAST_DEVICE_ID = "last_device_id"
        const val KEY_LAST_DEVICE_NAME = "last_device_name"
        const val KEY_LAST_DEVICE_MODEL = "last_device_model"
        const val KEY_LAST_DEVICE_FIRMWARE = "last_device_firmware"
        const val KEY_EXPERIMENTAL_OVERCLOCK = "experimental_overclock"
    }
}
