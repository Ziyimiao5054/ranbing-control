package com.anemoventi.ranbing.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.anemoventi.ranbing.data.SettingsState
import com.anemoventi.ranbing.data.ThemeMode
import com.anemoventi.ranbing.util.DeviceUi
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

/** True when the MIUIx skin is active, so leaf helpers can pick native widgets. */
val LocalUseMiuix = staticCompositionLocalOf { false }

/** Current dark mode, provided so MIUIx helpers can pick the reference card tints. */
val LocalIsDark = staticCompositionLocalOf { true }

private val MiuixAccent = Color(0xFF287AF9)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    state: SettingsState,
    content: @Composable () -> Unit,
) {
    val dark = when (state.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val useMiuix = remember(state.uiStyle) { DeviceUi.resolve(state.uiStyle) }

    val preservedContent = remember {
        movableContentOf<@Composable () -> Unit> { target -> target() }
    }

    val context = LocalContext.current
    val accent = Color(state.accentColor)
    val colorScheme: ColorScheme = remember(state.dynamicColor, dark, accent) {
        when {
            state.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

            else -> if (dark) {
                darkColorScheme(primary = accent)
            } else {
                lightColorScheme(primary = accent)
            }
        }
    }

    // MIUIx keeps a fixed HyperOS look: pure black background and #287AF9 accent, no Monet.
    val miuixColors = remember(dark) {
        val base = if (dark) miuixDarkColorScheme() else miuixLightColorScheme()
        base.copy(
            primary = MiuixAccent,
            primaryVariant = MiuixAccent,
            background = if (dark) Color.Black else Color.White,
            surface = if (dark) Color.Black else base.surface,
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalUseMiuix provides useMiuix,
        LocalIsDark provides dark,
    ) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            }
        }

        if (useMiuix) {
            MiuixTheme(colors = miuixColors) {
                preservedContent(content)
            }
        } else {
            MaterialExpressiveTheme(
                colorScheme = colorScheme,
                motionScheme = MotionScheme.expressive(),
            ) {
                preservedContent(content)
            }
        }
    }
}
