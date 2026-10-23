package com.anemoventi.ranbing.ui.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.anemoventi.ranbing.data.AppSettings
import com.anemoventi.ranbing.data.SettingsState
import com.anemoventi.ranbing.device.DeviceRepository
import com.anemoventi.ranbing.ui.page.AboutPage
import com.anemoventi.ranbing.ui.page.MainPage
import com.anemoventi.ranbing.ui.page.ThemePage
import com.anemoventi.ranbing.ui.theme.LocalUseMiuix
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppNavContainer(
    prefs: SettingsState,
    settings: AppSettings,
    device: DeviceRepository,
) {
    val backStack = rememberNavBackStack<Route>(Route.Main)
    val navigator = remember(backStack) { Navigator(backStack) }
    val onBack = remember(navigator) { { navigator.pop() } }

    val backdrop = if (LocalUseMiuix.current) {
        MiuixTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val effects = remember(backdrop) {
        NavDisplayEffects(
            enableCornerClip = true,
            cornerClipRadius = 0.dp,
            dimAmount = 0.5f,
            backdropColor = backdrop,
            blockInputDuringTransition = false,
        )
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavDisplay(
            backStack = backStack,
            onBack = onBack,
            transition = NavTransitions.MiuixDefault,
            effects = effects,
        ) {
            entry<Route.Main> {
                MainPage(prefs = prefs, settings = settings, device = device)
            }
            entry<Route.Theme> {
                ThemePage(prefs = prefs, settings = settings)
            }
            entry<Route.About> {
                AboutPage()
            }
        }
    }
}
