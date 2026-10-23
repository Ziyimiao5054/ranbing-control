package com.anemoventi.ranbing.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * Navigation destinations. Must be `@Serializable` data objects/classes so the miuix back stack
 * can persist itself across process death.
 */
@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Main : Route

    @Serializable
    data object Theme : Route

    @Serializable
    data object About : Route
}
