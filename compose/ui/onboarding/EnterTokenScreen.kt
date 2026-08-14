package com.blazingdurtles.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blazingdurtles.ui.components.OnboardingTopBar
import com.blazingdurtles.ui.components.PrimaryButton
import com.blazingdurtles.ui.theme.BdTheme
import com.blazingdurtles.ui.theme.JetBrainsMono

/**
 * Screen 03 — Enter token (v2 "Less").
 * Changed vs. v1: dropped "we'll check it straight away" copy and the separate
 * "Paste from clipboard" button (a trailing paste icon inside the field covers it).
 * "Verify & continue" enables only once something is entered.
 */
@Composable
fun EnterTokenScreen(
    onBack: () -> Unit,
    onVerify: (String) -> Unit,
) {
    var token by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            OnboardingTopBar("Enter your token", onBack)
            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 26.dp)) {
                Text(
                    "Paste the V2 API token you just generated.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = BdTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 10.dp, bottom = 26.dp)
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it.trim() },
                    label = { Text("V2 API token") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Rounded.VpnKey, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            token = clipboard.getText()?.text?.trim() ?: token
                        }) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste from clipboard",
                                tint = BdTheme.colors.textSecondary)
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = JetBrainsMono, fontSize = 16.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done
                    ),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BdTheme.colors.divider,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Looks like a1b2c3d4-… — find it on your WaniKani API Tokens page.",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                    ),
                    color = BdTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = 14.dp, top = 8.dp)
                )
            }
            PrimaryButton(
                text = "Verify & continue",
                onClick = { onVerify(token) },
                enabled = token.isNotBlank(),
                trailingIcon = null,
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 16.dp)
            )
        }
    }
}
