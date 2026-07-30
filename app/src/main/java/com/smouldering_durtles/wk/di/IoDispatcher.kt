package com.smouldering_durtles.wk.di

import javax.inject.Qualifier

/**
 * Qualifies the dispatcher for blocking I/O — disk, network, and binder calls.
 *
 * Injected rather than referenced as `Dispatchers.IO` directly so tests can substitute a
 * test dispatcher and drive the work deterministically.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
