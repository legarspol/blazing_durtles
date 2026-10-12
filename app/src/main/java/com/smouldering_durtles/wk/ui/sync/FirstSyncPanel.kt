package com.smouldering_durtles.wk.ui.sync

import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smouldering_durtles.wk.ui.theme.BlazingDurtlesTheme

/**
 * Drops the first-sync screen into a `ComposeView` declared in an XML layout.
 *
 * This is the one place the legacy View world and Compose meet, and it exists so `MainActivity`
 * stays a one-liner: `setContent` takes a composable lambda, which Java cannot write. The panel
 * also owns its own visibility rather than making the activity observe `LiveFirstTimeSetup` a
 * second time - the state driving the screen already knows whether the first sync is running, so
 * having Java re-derive it would be two sources of truth for one boolean.
 */
object FirstSyncPanel {

    @JvmStatic
    fun install(view: ComposeView) {
        view.setContent {
            BlazingDurtlesTheme(darkTheme = isSystemInDarkTheme()) {
                val viewModel: SyncViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()

                // In an effect rather than in composition: this reaches outside Compose to the
                // host View, and doing that while composing would be a side effect on every
                // recomposition.
                LaunchedEffect(state.visible) {
                    view.visibility = if (state.visible) View.VISIBLE else View.GONE
                }

                if (state.visible) {
                    FirstSyncScreen(state)
                }
            }
        }
    }
}
