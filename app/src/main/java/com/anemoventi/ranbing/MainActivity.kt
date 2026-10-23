package com.anemoventi.ranbing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anemoventi.ranbing.ui.navigation.AppNavContainer
import com.anemoventi.ranbing.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as App
        val settings = app.settings

        setContent {
            val state by settings.state.collectAsStateWithLifecycle()
            AppTheme(state = state) {
                AppNavContainer(
                    prefs = state,
                    settings = settings,
                    device = app.device,
                )
            }
        }
    }
}
