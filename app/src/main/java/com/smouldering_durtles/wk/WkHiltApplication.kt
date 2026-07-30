package com.smouldering_durtles.wk

import com.smouldering_durtles.wk.diagnostics.ExitInfoReporter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Hilt entry point for the application.
 *
 * `@HiltAndroidApp` must sit on a Kotlin class so KSP can process it (KSP does not process Java
 * sources, and Hilt cannot be split across two annotation processors). The real application logic
 * stays in the legacy Java [WkApplication], which this thin subclass extends until that god-class
 * is decomposed. The manifest registers this class as `android:name`.
 */
@HiltAndroidApp
class WkHiltApplication : WkApplication() {
    @Inject
    lateinit var exitInfoReporter: ExitInfoReporter

    /**
     * The exit-reason scan is kicked off here rather than from [WkApplication]'s `onCreateLocal()`
     * because it needs an injected dependency, and members are only injected once
     * `super.onCreate()` has run. The alternative — reaching the graph from the Java superclass via
     * `EntryPointAccessors` — would work, but this keeps the wiring ordinary constructor/field
     * injection, and the scan's timing relative to the rest of app init does not matter (nothing
     * waits on it, and it is idempotent).
     */
    override fun onCreate() {
        super.onCreate()
        exitInfoReporter.report()
    }
}
