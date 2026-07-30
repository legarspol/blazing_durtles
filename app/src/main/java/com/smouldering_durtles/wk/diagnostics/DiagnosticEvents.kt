package com.smouldering_durtles.wk.diagnostics

/**
 * Names of the analytics events the app reports, and of their parameters.
 *
 * Firebase constrains both: event and parameter names must be alphanumeric with underscores, event
 * names are limited to 40 characters and must not start with a `firebase_`, `google_` or `ga_`
 * prefix. String parameter values are truncated at 100 characters.
 *
 * These names are the identity of the event in the Firebase console — renaming one starts a new,
 * unrelated series there, so treat them as fixed once they have shipped.
 */
object DiagnosticEvents {
    /**
     * A persisted API task was discarded because its stored type key no longer maps to any known
     * task type. Reported alongside a Crashlytics non-fatal; see `ApiTaskService`.
     */
    const val API_TASK_DROPPED = "api_task_dropped"

    /** The stored task type key of the discarded task. */
    const val PARAM_TASK_KEY = "task_key"

    /** The priority of the discarded task. */
    const val PARAM_PRIORITY = "priority"
}
