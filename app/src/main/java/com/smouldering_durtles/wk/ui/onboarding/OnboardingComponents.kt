package com.smouldering_durtles.wk.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smouldering_durtles.wk.ui.theme.BdTheme
import com.smouldering_durtles.wk.ui.theme.BlazingDurtlesTheme
import com.smouldering_durtles.wk.ui.theme.JetBrainsMono

/**
 * How much visual weight a call to action carries.
 *
 * The Connect screen needs to move the filled treatment from one button to the other once the
 * user has been sent to WaniKani, so emphasis is a parameter rather than a choice baked into two
 * separate composables. Only the fill moves — the buttons keep their positions, so nothing jumps
 * under the user's thumb between taps.
 */
enum class ButtonEmphasis { Filled, Tonal }

/** The single call-to-action button used across onboarding. */
@Composable
fun OnboardingButton(
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
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
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

/** Back-titled top app bar. [onBack] is null on screens that are a start destination. */
@Composable
fun OnboardingTopBar(title: String, onBack: (() -> Unit)? = null) {
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
                    contentDescription = OnboardingStrings.back,
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

/** A permission scope pill: green check plus the scope name in monospace. */
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
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
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
                OnboardingTopBar("Connect your account", onBack = {})
                OnboardingTopBar("No back button")
                OnboardingButton("Filled", {}, emphasis = ButtonEmphasis.Filled)
                OnboardingButton(
                    "Filled with icon", {},
                    trailingIcon = Icons.Rounded.OpenInNew,
                )
                OnboardingButton("Tonal", {}, emphasis = ButtonEmphasis.Tonal)
                OnboardingButton("Disabled", {}, enabled = false)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepBadge(1)
                    StepBadge(2)
                }
                PermissionChip("assignments:start")
            }
        }
    }
}
