package com.smouldering_durtles.wk.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.util.AsyncTask

/**
 * Reads `ActivityManager.getHistoricalProcessExitReasons()` on app start and reports the
 * interesting exits (ANRs, low-memory kills, the Android 17 memory limiter, ...) to
 * Crashlytics as non-fatals. None of these produce a Java exception, so without this the
 * process simply disappears and Crashlytics never learns why.
 *
 * No-op below API 30 (`getHistoricalProcessExitReasons` requires R; `minSdk` is 23).
 * Consent-gated exactly like [Diagnostics.logException]. A failure here is caught,
 * logged to logcat, and never allowed to take the app down.
 */
object ExitInfoReporter {
    private const val TAG = "ExitInfoReporter"

    /**
     * Kick off the exit-reason scan in the background. Safe to call unconditionally from
     * `WkApplication.onCreateLocal()`.
     *
     * @param context used to obtain the `ActivityManager`
     */
    @JvmStatic
    fun report(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        if (!GlobalSettings.Diagnostics.getConsentRequested()
            || !GlobalSettings.Diagnostics.isCrashReportingEnabled()) {
            return
        }

        val appContext = context.applicationContext

        object : AsyncTask<Void>() {
            override fun doInBackground(): Void? {
                scanAndReport(appContext)
                return null
            }

            override fun onPostExecute(result: Void?) {
                // Nothing to do on the UI thread.
            }

            override fun onProgressUpdate(values: Array<Any>) {
                // Not used.
            }
        }.execute()
    }

    private fun scanAndReport(context: Context) {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val exitInfos = activityManager.getHistoricalProcessExitReasons(null, 0, 0)
            val watermark = GlobalSettings.Diagnostics.getLastReportedExitTimestamp()
            var highestSeen = watermark

            for (exitInfo in exitInfos) {
                val timestamp = exitInfo.timestamp
                if (timestamp <= watermark) {
                    continue
                }
                if (timestamp > highestSeen) {
                    highestSeen = timestamp
                }

                val report = ExitReasonClassifier.classify(exitInfo.reason, exitInfo.description, exitInfo.importance)
                if (report != null) {
                    Diagnostics.logException(groupedThrowable(report), report.detail)
                }
            }

            if (highestSeen > watermark) {
                GlobalSettings.Diagnostics.setLastReportedExitTimestamp(highestSeen)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report historical process exit reasons", e)
        }
    }

    /**
     * Crashlytics groups non-fatals primarily by the throwable's top stack frame, not its
     * message. A synthetic stack frame whose "class name" is the report's group key keeps
     * distinct exit causes (ANR, low-memory, memory-limiter, ...) as distinct Crashlytics
     * issues instead of collapsing them all into one, since every report is otherwise
     * thrown from this same call site.
     */
    private fun groupedThrowable(report: ExitReport): Throwable {
        val throwable = Throwable(report.detail)
        throwable.stackTrace = arrayOf(StackTraceElement("ExitInfo", report.groupKey, "ExitInfo", 0))
        return throwable
    }
}
