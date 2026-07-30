package com.smouldering_durtles.wk.diagnostics

/**
 * A Crashlytics-reportable summary of one `ApplicationExitInfo` record.
 *
 * @param groupKey a short, stable identifier used to group same-cause exits together in
 * the Crashlytics dashboard
 * @param detail human-readable detail included in the non-fatal report body
 */
data class ExitReport(val groupKey: String, val detail: String)

/**
 * Decides which `android.app.ApplicationExitInfo` records are worth reporting to
 * Crashlytics, and how.
 *
 * Pure Kotlin, no Android imports: it takes the primitive fields off the exit record
 * rather than the Android type itself, so the decision logic is plain-JVM-testable.
 */
object ExitReasonClassifier {
    // android.app.ApplicationExitInfo reason codes (API 30+), mirrored here as plain
    // ints so this file stays free of Android imports.
    private const val REASON_LOW_MEMORY = 3
    private const val REASON_CRASH_NATIVE = 5
    private const val REASON_ANR = 6
    private const val REASON_EXCESSIVE_RESOURCE_USAGE = 9
    private const val REASON_OTHER = 13

    private const val MEMORY_LIMITER_MARKER = "MemoryLimiter"

    /**
     * Classify one exit record.
     *
     * @param reason the `ApplicationExitInfo.getReason()` value
     * @param description the `ApplicationExitInfo.getDescription()` value, if any
     * @param importance the `ApplicationExitInfo.getImportance()` value
     * @return a report to send to Crashlytics, or `null` if this exit is not worth reporting
     */
    fun classify(reason: Int, description: String?, importance: Int): ExitReport? {
        val groupKey = when (reason) {
            REASON_ANR -> "anr"
            REASON_LOW_MEMORY -> "low_memory"
            REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive_resource_usage"
            REASON_CRASH_NATIVE -> "crash_native"
            REASON_OTHER -> if (description != null && description.contains(MEMORY_LIMITER_MARKER)) {
                "memory_limiter"
            } else {
                "other"
            }
            else -> return null
        }
        val detail = "reason=$reason importance=$importance description=${description ?: "<none>"}"
        return ExitReport(groupKey, detail)
    }
}
