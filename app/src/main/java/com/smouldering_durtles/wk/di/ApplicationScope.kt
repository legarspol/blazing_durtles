package com.smouldering_durtles.wk.di

import javax.inject.Qualifier

/**
 * Qualifies the single application-lifetime [kotlinx.coroutines.CoroutineScope].
 *
 * Use it for fire-and-forget work that must outlive the screen that started it and has no
 * other owner — not for work a `ViewModel` or a `Lifecycle` already scopes, which belongs
 * on `viewModelScope`/`lifecycleScope` so it gets cancelled with its owner.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
