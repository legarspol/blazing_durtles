package com.smouldering_durtles.wk.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Phase 0 smoke check: proves `:core` (pure Kotlin/JVM, no Android SDK) builds, is
 * constructor-`@Inject`-able (`javax.inject`, no Hilt/Dagger processor in `:core`), and that
 * `:app`'s Hilt graph can resolve it. Moves no logic — see docs/MIGRATION_PLAN.md Phase 0. Safe
 * to delete once Phase 3 domain classes land.
 */
class CoreSmoke @Inject constructor() {
    val status: String = "core is wired up"

    fun wiredAt(): Instant = Clock.System.now()
}
