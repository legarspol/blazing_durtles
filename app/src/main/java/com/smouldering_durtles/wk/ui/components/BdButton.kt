package com.smouldering_durtles.wk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.ui.theme.BdTheme
import com.smouldering_durtles.wk.ui.theme.BlazingDurtlesTheme

/**
 * How much visual weight a call to action carries.
 *
 * A screen with two calls to action sometimes needs to move the filled treatment from one button
 * to the other — onboarding's Connect screen does it once the user has been sent to WaniKani — so
 * emphasis is a parameter rather than a choice baked into two separate composables. Only the fill
 * moves; the buttons keep their positions, so nothing jumps under the user's thumb between taps.
 */
enum class ButtonEmphasis { Filled, Tonal }

/** The app's full-width call-to-action button. */
@Composable
fun BdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: ButtonEmphasis = ButtonEmphasis.Filled,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    val colors = when (emphasis) {
        ButtonEmphasis.Filled -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = BdTheme.colors.placeholderButton,
            disabledContentColor = BdTheme.colors.textDisabled,
        )
        ButtonEmphasis.Tonal -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = BdTheme.colors.emberInk,
            disabledContainerColor = BdTheme.colors.placeholderButton,
            disabledContentColor = BdTheme.colors.textDisabled,
        )
    }
    val shape = RoundedCornerShape(28.dp)
    // Only the live primary action lifts off the page — a tonal or disabled button stays flat,
    // as the design boards have them. The shadow is tinted with primary rather than left black:
    // in light theme that warms it, and in dark theme it is what makes the ember glow under the
    // button. API 27 and below ignore the tint and draw an ordinary black shadow.
    val lift = if (emphasis == ButtonEmphasis.Filled && enabled) 12.dp else 0.dp
    val shadowColor = MaterialTheme.colorScheme.primary

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(lift, shape, ambientColor = shadowColor, spotColor = shadowColor),
        shape = shape,
        colors = colors,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
        if (trailingIcon != null && enabled) {
            Icon(
                trailingIcon,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp).size(20.dp),
            )
        }
    }
}

@Preview(name = "BdButton — light", showBackground = true)
@Composable
private fun BdButtonPreviewLight() = BdButtonPreview(darkTheme = false)

@Preview(name = "BdButton — dark", showBackground = true)
@Composable
private fun BdButtonPreviewDark() = BdButtonPreview(darkTheme = true)

@Composable
private fun BdButtonPreview(darkTheme: Boolean) {
    BlazingDurtlesTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BdButton("Filled", {}, emphasis = ButtonEmphasis.Filled)
                BdButton("Filled with icon", {}, trailingIcon = Icons.Rounded.OpenInNew)
                BdButton("Tonal", {}, emphasis = ButtonEmphasis.Tonal)
                BdButton("Disabled", {}, enabled = false)
            }
        }
    }
}
