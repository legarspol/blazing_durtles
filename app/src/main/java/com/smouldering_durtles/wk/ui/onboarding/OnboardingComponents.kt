package com.smouldering_durtles.wk.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smouldering_durtles.wk.ui.theme.BdTheme
import com.smouldering_durtles.wk.ui.theme.BlazingDurtlesTheme
import com.smouldering_durtles.wk.ui.theme.Roboto

/**
 * The pieces that only onboarding has a use for. Anything reusable beyond this flow — the call to
 * action, the top bar — lives in `…/ui/components` instead.
 */

/** Numbered step badge used on the Connect screen. */
@Composable
fun StepBadge(number: Int) {
    Surface(
        shape = CircleShape,
        color = BdTheme.colors.container,
        modifier = Modifier.size(34.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "$number",
                style = MaterialTheme.typography.titleMedium,
                color = BdTheme.colors.emberInk,
            )
        }
    }
}

/** A permission scope pill: green check plus the scope name. */
@Composable
fun PermissionChip(scope: String) {
    Surface(shape = RoundedCornerShape(11.dp), color = BdTheme.colors.surfaceChip) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = BdTheme.colors.success,
                modifier = Modifier.size(15.dp),
            )
            Text(
                scope,
                fontFamily = Roboto,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = BdTheme.colors.emberInk,
            )
        }
    }
}

@Preview(name = "Components — light", showBackground = true)
@Composable
private fun ComponentsPreviewLight() = ComponentsPreview(darkTheme = false)

@Preview(name = "Components — dark", showBackground = true)
@Composable
private fun ComponentsPreviewDark() = ComponentsPreview(darkTheme = true)

@Composable
private fun ComponentsPreview(darkTheme: Boolean) {
    BlazingDurtlesTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepBadge(1)
                    StepBadge(2)
                }
                PermissionChip("assignments:start")
            }
        }
    }
}
