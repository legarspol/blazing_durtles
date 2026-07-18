package com.smouldering_durtles.wk.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

object ComposeSmokeStrings {
    const val text = "Compose smoke test"
}

@Composable
fun ComposeSmoke() {
    Text(ComposeSmokeStrings.text)
}

@Preview
@Composable
private fun ComposeSmokePreview() {
    ComposeSmoke()
}
