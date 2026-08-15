package com.smouldering_durtles.wk.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smouldering_durtles.wk.ui.components.BdButton
import com.smouldering_durtles.wk.ui.components.BdTopBar
import com.smouldering_durtles.wk.ui.theme.BdTheme
import com.smouldering_durtles.wk.ui.theme.BlazingDurtlesTheme

/**
 * The token input.
 *
 * [token] is held exactly as typed — normalising per keystroke fights the IME's composing region
 * and would let the field disagree with what is eventually stored. The caller normalises once,
 * on submit. [canContinue] carries the format check, so an unusable token simply leaves the
 * button disabled rather than needing an error state of its own.
 */
@Composable
fun EnterTokenScreen(
    token: String,
    canContinue: Boolean,
    onTokenChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            BdTopBar(OnboardingStrings.enterTokenTitle, onBack)
            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 26.dp)) {
                Text(
                    OnboardingStrings.enterTokenIntro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BdTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 10.dp, bottom = 26.dp),
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = onTokenChange,
                    label = { Text(OnboardingStrings.tokenFieldLabel) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboard.getText()?.text?.let(onTokenChange)
                            }
                        ) {
                            Icon(
                                Icons.Rounded.ContentPaste,
                                contentDescription = OnboardingStrings.pasteFromClipboard,
                                tint = BdTheme.colors.textSecondary,
                            )
                        }
                    },
                    // 16sp rather than the 14sp body size: this is the one string the user has
                    // to check character by character before committing to it.
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BdTheme.colors.divider,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    OnboardingStrings.tokenFieldHelper,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BdTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = 14.dp, top = 8.dp),
                )
            }
            BdButton(
                text = OnboardingStrings.verifyAndContinue,
                onClick = onContinue,
                enabled = canContinue,
                modifier = Modifier
                    .padding(horizontal = 26.dp, vertical = 16.dp)
                    .imePadding(),
            )
        }
    }
}

@Preview(name = "Enter token — light, empty", showBackground = true)
@Composable
private fun EnterTokenPreviewEmpty() = BlazingDurtlesTheme(darkTheme = false) {
    EnterTokenScreen("", false, {}, {}, {})
}

@Preview(name = "Enter token — light, valid", showBackground = true)
@Composable
private fun EnterTokenPreviewValid() = BlazingDurtlesTheme(darkTheme = false) {
    EnterTokenScreen("00000000-1111-2222-3333-444444444444", true, {}, {}, {})
}

@Preview(name = "Enter token — dark, empty", showBackground = true)
@Composable
private fun EnterTokenPreviewDarkEmpty() = BlazingDurtlesTheme(darkTheme = true) {
    EnterTokenScreen("", false, {}, {}, {})
}

@Preview(name = "Enter token — dark, valid", showBackground = true)
@Composable
private fun EnterTokenPreviewDarkValid() = BlazingDurtlesTheme(darkTheme = true) {
    EnterTokenScreen("00000000-1111-2222-3333-444444444444", true, {}, {}, {})
}
