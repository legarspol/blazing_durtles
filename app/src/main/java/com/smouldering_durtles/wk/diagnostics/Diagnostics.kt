package com.smouldering_durtles.wk.diagnostics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.WkApplication

/**
 * Central switch for the app's optional diagnostics: Firebase Crashlytics (crash and
 * non-fatal reporting) and Firebase Analytics (usage analytics).
 *
 * Nothing is collected until the user has answered the first-run consent prompt (see
 * `MainActivity`). Auto-collection is disabled in the manifest, so both SDKs stay
 * dormant until [applyConsentState] explicitly enables them for a user who opted in.
 * The user can revisit the choice at any time in Settings, which calls back here.
 */
object Diagnostics {
    private const val TAG = "Diagnostics"

    /**
     * Push the user's current consent choices into the Firebase SDKs. Safe to call on
     * every app start and whenever the settings toggles change. Until the first-run
     * consent prompt has been answered, both collectors are forced off regardless of
     * the stored toggle values.
     */
    @JvmStatic
    fun applyConsentState() {
        val consentRequested = GlobalSettings.Diagnostics.getConsentRequested()
        val crashEnabled = consentRequested && GlobalSettings.Diagnostics.isCrashReportingEnabled()
        val analyticsEnabled = consentRequested && GlobalSettings.Diagnostics.isAnalyticsEnabled()

        // Telemetry must never take the app down — least of all the crash reporter.
        // Report the failure to logcat and carry on rather than propagating it.
        try {
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = crashEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply crash reporting consent", e)
        }
        try {
            FirebaseAnalytics.getInstance(WkApplication.getInstance())
                .setAnalyticsCollectionEnabled(analyticsEnabled)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply analytics consent", e)
        }
    }

    /**
     * Record a caught (non-fatal) exception to Crashlytics. A no-op unless the user has
     * consented to crash reporting. Never throws — a failure to report must not become a
     * second failure.
     *
     * @param throwable the caught exception to report
     * @param message optional context logged alongside the exception
     */
    @JvmStatic
    @JvmOverloads
    fun logException(throwable: Throwable, message: String? = null) {
        try {
            if (!GlobalSettings.Diagnostics.getConsentRequested()
                || !GlobalSettings.Diagnostics.isCrashReportingEnabled()) {
                return
            }
            val crashlytics = FirebaseCrashlytics.getInstance()
            if (message != null) {
                crashlytics.log(message)
            }
            crashlytics.recordException(throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record exception", e)
        }
    }

    /**
     * Record an analytics event. A no-op unless the user has consented to analytics. Never throws —
     * telemetry must not be able to break the flow that reports it.
     *
     * @param name the event name, see [DiagnosticEvents]
     * @param params optional event parameters, keys as in [DiagnosticEvents]
     */
    @JvmStatic
    @JvmOverloads
    fun logEvent(name: String, params: Bundle? = null) {
        try {
            if (!GlobalSettings.Diagnostics.getConsentRequested()
                || !GlobalSettings.Diagnostics.isAnalyticsEnabled()) {
                return
            }
            FirebaseAnalytics.getInstance(WkApplication.getInstance()).logEvent(name, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record event $name", e)
        }
    }

    /**
     * Scan `ActivityManager.getHistoricalProcessExitReasons()` in the background and
     * report the interesting exits (ANRs, low-memory kills, the memory limiter, ...) as
     * Crashlytics non-fatals. A no-op below API 30 or without crash-reporting consent;
     * see [ExitInfoReporter] for the full gating and dedupe behaviour.
     *
     * @param context used to obtain the `ActivityManager`
     */
    @JvmStatic
    fun reportHistoricalExits(context: Context) {
        ExitInfoReporter.report(context)
    }
}