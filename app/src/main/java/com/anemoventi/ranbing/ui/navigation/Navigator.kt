package com.anemoventi.ranbing.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * Small wrapper around the miuix back stack, mirroring the reference project's navigator
 * (idempotent push, debounced pop).
 */
class Navigator(val backStack: NavBackStack) {

    private val resultBus = mutableMapOf<String, MutableSharedFlow<Any>>()
    private var lastPopTime = 0L

    fun push(key: NavKey) {
        if (backStack.lastOrNull() == key) return
        backStack.add(key)
    }

    fun pop() {
        val now = System.currentTimeMillis()
        if (now - lastPopTime < 100) return
        if (backStack.size <= 1) return
        lastPopTime = now
        backStack.removeLastOrNull()
    }

    fun popUntil(predicate: (NavKey) -> Boolean) {
        while (backStack.isNotEmpty() && !predicate(backStack.last())) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun current(): NavKey? = backStack.lastOrNull()

    fun backStackSize(): Int = backStack.size

    private fun ensureChannel(key: String): MutableSharedFlow<Any> = resultBus.getOrPut(key) {
        MutableSharedFlow(replay = 1)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> observeResult(key: String): SharedFlow<T> = ensureChannel(key) as SharedFlow<T>

    fun <T : Any> setResult(key: String, value: T) {
        ensureChannel(key).tryEmit(value)
        pop()
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("LocalNavigator not provided")
}
