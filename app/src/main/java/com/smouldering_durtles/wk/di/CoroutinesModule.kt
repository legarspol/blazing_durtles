package com.smouldering_durtles.wk.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Provides the app's coroutine primitives: the application-lifetime scope and the named
 * dispatchers.
 *
 * Dispatchers are provided rather than referenced as `Dispatchers.*` at each use site so
 * that tests can substitute deterministic ones, and so the choice of dispatcher for a
 * given kind of work is made in one place.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {
    /**
     * The single scope for work tied to the process rather than to any UI component.
     *
     * `SupervisorJob` (not `Job`) because a plain `Job` propagates a child's failure upwards:
     * the parent cancels, every sibling cancels with it, and — since a cancelled `Job` cannot
     * be reused — the scope is dead for the rest of the process, silently dropping every
     * later `launch`. One unlucky failure would disable all background work until restart.
     *
     * Note what this does *not* do: it isolates cancellation, not failure. An uncaught
     * exception in a child still reaches the thread's default handler and crashes the app.
     * That is deliberate and no `CoroutineExceptionHandler` is installed here — per the
     * crash-fast rule, such a crash belongs in Crashlytics rather than swallowed. Work that
     * genuinely must not take the app down catches its own exceptions at that boundary and
     * reports them (see `ExitInfoReporter.scanAndReport`).
     *
     * Nothing cancels this scope; it lives until the process dies, which is why only
     * genuinely ownerless work belongs on it.
     *
     * Built on [Dispatchers.Default], the right choice for the CPU-bound work that most
     * consumers do; callers doing blocking I/O should use [IoDispatcher] rather than assume
     * this scope's dispatcher.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** @see IoDispatcher */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
