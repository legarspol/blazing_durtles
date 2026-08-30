package com.smouldering_durtles.wk.ui.sync

/** Every string the first-sync screen shows, as plain Kotlin. */
object SyncStrings {
    const val title = "Setting things up…"

    /**
     * Absorbs what the retired FirstTimeSetupView used to say ("Please wait while we prepare the
     * app for your account. This may take a minute or more...") — same promise, fewer words, and
     * the checklist below it now shows the waiting rather than just asserting it.
     */
    const val subtitle = "Pulling your progress from WaniKani. This only happens once."

    const val syncing = "Syncing"
    const val done = "Done"
    const val waiting = "Waiting"

    const val rowProfile = "Profile & level"
    const val rowSubjects = "Subjects & mnemonics"
    const val rowAssignments = "Assignments"
    const val rowForecast = "Review forecast"

    const val runningDescription = "Syncing"

    fun items(synced: Int, total: Int): String = "${count(synced)} / ${count(total)} items"

    fun ratio(processed: Int, total: Int): String = "${count(processed)} / ${count(total)}"

    /** 2431 -> "2,431". Written out rather than using NumberFormat, which is locale-sensitive. */
    fun count(value: Int): String {
        val digits = value.toString()
        return buildString {
            digits.forEachIndexed { index, char ->
                if (index > 0 && (digits.length - index) % 3 == 0) append(',')
                append(char)
            }
        }
    }
}
