package com.smouldering_durtles.wk.ui.onboarding

import androidx.lifecycle.ViewModel
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.jobs.SettingChangedJob
import com.smouldering_durtles.wk.services.JobRunnerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Everything the onboarding screens render from. */
data class OnboardingUiState(
    val settingsTapped: Boolean = false,
    val token: String = "",
    val finished: Boolean = false,
) {
    val canContinue: Boolean get() = isValidApiToken(token)
}

/**
 * Onboarding's state.
 *
 * The token lives here rather than in `rememberSaveable`: that is backed by the saved-instance
 * state Bundle, which the system writes to disk for task restore, and putting a credential there
 * would be a real regression against the EncryptedSharedPreferences it is headed for. Held in
 * memory it simply does not survive process death, which costs the user one paste.
 */
class OnboardingViewModel : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    /**
     * Resolved once, eagerly. SharedPreferences reads are synchronous, so the graph can be
     * composed with its real start destination rather than opening on Welcome and correcting
     * itself with a visible jump.
     */
    val startDestination: OnboardingDestination =
        startDestinationFor(GlobalSettings.Tutorials.getWelcomeDismissed())

    /** Called when Welcome is shown; it is brand recognition, and once is enough. */
    fun onWelcomeShown() {
        GlobalSettings.Tutorials.setWelcomeDismissed(true)
    }

    fun onOpenSettingsTapped() = _state.update { it.copy(settingsTapped = true) }

    fun onTokenChange(raw: String) = _state.update { it.copy(token = raw) }

    /**
     * Stores the token and hands off to the dashboard.
     *
     * Scheduling [SettingChangedJob] is what actually starts the app working again: it clears
     * `api_key_rejected` and `api_in_error`, zeroes both sync dates and asserts a `GetUserTask`.
     * That branch exists today but never fires for the token, because it is driven by a listener
     * on the *default* SharedPreferences while `api_key` is written to a separate encrypted file
     * — so saving a fresh key currently leaves the rejected flag set and the app waits for
     * housekeeping to notice. Reusing the job fixes that rather than restating its six writes in
     * a ViewModel, where database work does not belong.
     */
    fun onContinue() {
        val token = normalizeApiToken(_state.value.token)
        if (!isValidApiToken(token) || _state.value.finished) {
            return
        }
        GlobalSettings.Api.setApiKey(token)
        JobRunnerService.schedule(SettingChangedJob::class.java, "api_key")
        _state.update { it.copy(finished = true) }
    }
}
