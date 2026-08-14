package com.blazingdurtles.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blazingdurtles.ui.theme.BdTheme
import com.blazingdurtles.ui.theme.JetBrainsMono

/** Simple back-titled top app bar matching the design (56dp, no elevation). */
@Composable
fun OnboardingTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back",
                tint = BdTheme.colors.textPrimary)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = BdTheme.colors.textPrimary)
    }
}

/** Filled pill primary CTA, 56–58dp tall, optional trailing icon. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Rounded.ArrowForward,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = BdTheme.colors.placeholderButton,
            disabledContentColor = BdTheme.colors.textDisabled,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
        if (trailingIcon != null && enabled) {
            Icon(trailingIcon, contentDescription = null,
                modifier = Modifier.padding(start = 8.dp).size(20.dp))
        }
    }
}

/** Tonal secondary button (warm chip fill). */
@Composable
fun TonalButton(text: String, onClick: () -> Unit, trailingIcon: ImageVector? = null,
                modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(27.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = BdTheme.colors.emberInk,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.5f.sp))
        if (trailingIcon != null) {
            Icon(trailingIcon, contentDescription = null,
                modifier = Modifier.padding(start = 8.dp).size(18.dp))
        }
    }
}

/** Numbered step badge used on the Connect screen. */
@Composable
fun StepBadge(number: Int) {
    Surface(shape = CircleShape, color = BdTheme.colors.container,
        modifier = Modifier.size(34.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text("$number", color = BdTheme.colors.emberInk,
                fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
    }
}

/** A permission scope pill: green check + monospace scope name. */
@Composable
fun PermissionChip(scope: String) {
    Surface(shape = RoundedCornerShape(11.dp), color = BdTheme.colors.surfaceChip) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null,
                tint = BdTheme.colors.success, modifier = Modifier.size(15.dp))
            Text(scope, fontFamily = JetBrainsMono, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, color = BdTheme.colors.emberInk)
        }
    }
}

/** Ghost/text button in ember. */
@Composable
fun EmberTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth().height(50.dp)) {
        Text(text, color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
    }
}

/** Standard outlined button used for less-prominent actions. */
@Composable
fun WarmOutlinedButton(text: String, onClick: () -> Unit, leadingIcon: ImageVector? = null) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.5.dp, BdTheme.colors.divider),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null,
                modifier = Modifier.padding(end = 8.dp).size(19.dp),
                tint = BdTheme.colors.emberInk)
        }
        Text(text, color = BdTheme.colors.emberInk,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.5f.sp))
    }
}
