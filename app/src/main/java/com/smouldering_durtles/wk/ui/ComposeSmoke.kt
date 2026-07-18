package com.smouldering_durtles.wk.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Phase 0 smoke check: proves the Jetpack Compose toolchain (compiler plugin + runtime) compiles
 * as part of `:app`. It is intentionally not wired into any Activity/Fragment or the live UI —
 * real Compose screens arrive in Phase 4. Safe to delete or replace once that work begins.
 */
@Composable
fun ComposeSmoke() {
    Text(text = "Compose is wired up")
}

@Preview
@Composable
private fun ComposeSmokePreview() {
    ComposeSmoke()
}
