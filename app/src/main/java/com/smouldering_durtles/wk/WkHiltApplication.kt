package com.smouldering_durtles.wk

import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt entry point for the application.
 *
 * `@HiltAndroidApp` must sit on a Kotlin class so KSP can process it (KSP does not process Java
 * sources, and Hilt cannot be split across two annotation processors). The real application logic
 * stays in the legacy Java [WkApplication], which this thin subclass extends until that god-class
 * is decomposed. The manifest registers this class as `android:name`.
 */
@HiltAndroidApp
class WkHiltApplication : WkApplication()
