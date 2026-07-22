package com.smouldering_durtles.wk.di

import com.smouldering_durtles.wk.domain.CoreSmoke
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Phase 0 smoke check: an entry point into `:app`'s Hilt `SingletonComponent` graph for the
 * `:core` [CoreSmoke] class. Its presence makes Hilt's aggregating processor resolve CoreSmoke's
 * constructor-`@Inject` binding at compile time, proving `:app` can inject a plain `@Inject`
 * `:core` type without a Hilt/Dagger processor in `:core`. No caller yet — real consumption
 * arrives with Phase 3 domain classes.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CoreSmokeEntryPoint {
    fun coreSmoke(): CoreSmoke
}
