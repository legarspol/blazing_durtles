package com.smouldering_durtles.wk.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smouldering_durtles.wk.data.sync.FirstSyncProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Holds the first-sync state for as long as the dashboard is alive.
 *
 * The tracker lives here rather than in the composition because it carries the running totals of
 * every finished stage. Remembered in a composable it would start again from zero on a rotation,
 * and the bar would jump backwards at exactly the moment the user is watching it.
 */
class SyncViewModel : ViewModel() {

    private val tracker = SyncProgressTracker()

    val state: StateFlow<SyncUiState> = FirstSyncProgress().snapshots()
        .map(tracker::accept)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SyncUiState())

    private companion object {
        /**
         * Long enough to ride out a rotation without tearing down the observers on the legacy
         * LiveData singletons, which would make ConservativeLiveData defer its updates again.
         */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
