package com.smouldering_durtles.wk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.ui.theme.BdTheme
import com.smouldering_durtles.wk.ui.theme.BlazingDurtlesTheme

/** Back-titled top app bar. [onBack] is null on screens that are a start destination. */
@Composable
fun BdTopBar(title: String, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack == null) {
            Spacer24()
        } else {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = ComponentStrings.back,
                    tint = BdTheme.colors.textPrimary,
                )
            }
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = BdTheme.colors.textPrimary)
    }
}

/** Keeps the title aligned with screens that do show a back button. */
@Composable
private fun Spacer24() = Box(Modifier.size(20.dp))

@Preview(name = "BdTopBar — light", showBackground = true)
@Composable
private fun BdTopBarPreviewLight() = BdTopBarPreview(darkTheme = false)

@Preview(name = "BdTopBar — dark", showBackground = true)
@Composable
private fun BdTopBarPreviewDark() = BdTopBarPreview(darkTheme = true)

@Composable
private fun BdTopBarPreview(darkTheme: Boolean) {
    BlazingDurtlesTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BdTopBar("With back button", onBack = {})
                BdTopBar("No back button")
            }
        }
    }
}
