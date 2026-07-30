package com.smouldering_durtles.wk.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.di.ApplicationScope
import com.smouldering_durtles.wk.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plain-data view of one `android.app.ApplicationExitInfo` record, so the scan logic can be
 * exercised without the Android framework type (which cannot be constructed in a JVM test).
 */
internal data class ExitRecord(
    val timestamp: Long,
    val reason: Int,
    val description: String?,
    val importance: Int,
)

/**
 * What a scan decided to do: the reports to send, and the watermark to persist afterwards
 * (`null` when there was nothing new and the stored value should be left alone).
 */
internal data class ExitScanPlan(
    val reports: List<ExitReport>,
    val newWatermark: Long?,
)

/**
 * Reads `ActivityManager.getHistoricalProcessExitReasons()` on app start and reports the
 * interesting exits (ANRs, low-memory kills, the Android 17 memory limiter, ...) to
 * Crashlytics as non-fatals. None of these produce a Java exception, so without this the
 * process simply disappears and Crashlytics never learns why.
 *
 * No-op below API 30 (`getHistoricalProcessExitReasons` requires R; `minSdk` is 23).
 * Consent-gated exactly like [Diagnostics.logException]. A failure here is caught, logged
 * to logcat, and never allowed to take the app down.
 *
 * `@Singleton` because it owns the dedupe watermark: two instances scanning concurrently
 * could both read the same watermark and report the same exit twice.
 */
@Singleton
class ExitInfoReporter @Inject constructor(
    // Explicit `@param:` targets: Dagger reads qualifiers off the constructor parameter, and
    // Kotlin is changing what an untargeted annotation here applies to (KT-73255).
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val scope: CoroutineScope,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Kick off the exit-reason scan in the background. Safe to call unconditionally.
     *
     * Fire-and-forget on the application scope: there is no lifecycle to tie it to and no
     * result anyone waits for. Nothing cancels it, and nothing needs to — if the process
     * is going away, dropping the scan is the right outcome, and the timestamp watermark
     * makes a re-run idempotent.
     */
    fun report() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        if (!GlobalSettings.Diagnostics.getConsentRequested()
            || !GlobalSettings.Diagnostics.isCrashReportingEnabled()) {
            return
        }

        scope.launch(ioDispatcher) {
            scanAndReport()
        }
    }

    /**
     * Blocking work: a binder call to `ActivityManager` plus `SharedPreferences` I/O, hence
     * [ioDispatcher]. Runs behind [report]'s API-level guard.
     *
     * This is a genuine process boundary — telemetry must not be able to take the app down,
     * and an uncaught throw inside `launch` would reach the thread's uncaught handler and do
     * exactly that. So the failure is reported to logcat rather than propagated; it is not a
     * silent `safe()`-style swallow.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun scanAndReport() {
        try {
            val watermark = GlobalSettings.Diagnostics.getLastReportedExitTimestamp()
            val plan = planScan(readExitRecords(), watermark)

            for (report in plan.reports) {
                Diagnostics.logException(groupedThrowable(report), report.detail)
            }
            plan.newWatermark?.let(GlobalSettings.Diagnostics::setLastReportedExitTimestamp)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report historical process exit reasons", e)
        }
    }

    /** Pull the exit records off `ActivityManager` and flatten them to [ExitRecord]s. */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun readExitRecords(): List<ExitRecord> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.getHistoricalProcessExitReasons(null, 0, 0).map {
            ExitRecord(it.timestamp, it.reason, it.description, it.importance)
        }
    }

    internal companion object {
        private const val TAG = "ExitInfoReporter"

        /**
         * Decide what to report from a batch of exit records, given the watermark of what has
         * already been reported. Pure, so the dedupe behaviour is testable on the JVM.
         *
         * The watermark advances past every record newer than it, *including* ones that turn
         * out not to be worth reporting — otherwise an uninteresting-but-recent exit would be
         * re-examined on every app start forever.
         */
        internal fun planScan(records: List<ExitRecord>, watermark: Long): ExitScanPlan {
            val reports = mutableListOf<ExitReport>()
            var highestSeen = watermark

            for (record in records) {
                if (record.timestamp <= watermark) {
                    continue
                }
                if (record.timestamp > highestSeen) {
                    highestSeen = record.timestamp
                }
                ExitReasonClassifier.classify(record.reason, record.description, record.importance)
                    ?.let(reports::add)
            }

            return ExitScanPlan(reports, highestSeen.takeIf { it > watermark })
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
}
