package com.smouldering_durtles.wk.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.ui.theme.BdTheme
import com.smouldering_durtles.wk.ui.theme.BlazingDurtlesTheme

/**
 * How to get a WaniKani API token.
 *
 * This is the start destination for a returning user whose token has expired, so it has no back
 * button — there would be nothing beneath it to go back to.
 *
 * [settingsTapped] moves the filled emphasis from "Open WaniKani settings" to "I've got my
 * token" once the user has actually been sent to WaniKani. Before that tap the next step is to
 * go and generate a token; after it, the next step is to come back and paste one. The buttons
 * keep their positions either way.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectScreen(
    settingsTapped: Boolean,
    onOpenSettings: () -> Unit,
    onHaveToken: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            OnboardingTopBar(OnboardingStrings.connectTitle)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 26.dp)
            ) {
                Text(
                    OnboardingStrings.connectIntro,
                    style = MaterialTheme.typography.bodyLarge,
                    color = BdTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                PrivacyNote(Modifier.padding(bottom = 6.dp))

                StepRow(
                    number = 1,
                    title = OnboardingStrings.stepGenerateTitle,
                    body = OnboardingStrings.stepGenerateBody,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.padding(top = 11.dp),
                    ) {
                        OnboardingStrings.requiredScopes.forEach { PermissionChip(it) }
                    }
                }

                StepRow(
                    number = 2,
                    title = OnboardingStrings.stepCopyTitle,
                    body = OnboardingStrings.stepCopyBody,
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                OnboardingButton(
                    text = OnboardingStrings.openWaniKaniSettings,
                    onClick = onOpenSettings,
                    emphasis = if (settingsTapped) ButtonEmphasis.Tonal else ButtonEmphasis.Filled,
                    trailingIcon = painterResource(R.drawable.ic_open_in_new),
                )
                OnboardingButton(
                    text = OnboardingStrings.haveMyToken,
                    onClick = onHaveToken,
                    emphasis = if (settingsTapped) ButtonEmphasis.Filled else ButtonEmphasis.Tonal,
                )
            }
        }
    }
}

@Composable
private fun StepRow(
    number: Int,
    title: String,
    body: String,
    extra: @Composable (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        StepBadge(number)
        Spacer(Modifier.size(15.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = BdTheme.colors.textPrimary,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = BdTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 3.dp),
            )
            extra?.invoke()
        }
    }
}

@Composable
private fun PrivacyNote(modifier: Modifier = Modifier) {
    val text = buildAnnotatedString {
        append(OnboardingStrings.privacyNote)
        withLink(
            LinkAnnotation.Url(
                OnboardingStrings.privacyPolicyUrl,
                TextLinkStyles(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                    )
                ),
            )
        ) {
            append(OnboardingStrings.privacyLinkLabel)
        }
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = BdTheme.colors.surfaceCard,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Icon(
                painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = BdTheme.colors.emberInk,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.size(11.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = BdTheme.colors.textSecondary,
            )
        }
    }
}

@Preview(name = "Connect — light, before tap", showBackground = true)
@Composable
private fun ConnectPreviewLight() = BlazingDurtlesTheme(darkTheme = false) {
    ConnectScreen(settingsTapped = false, onOpenSettings = {}, onHaveToken = {})
}

@Preview(name = "Connect — light, after tap", showBackground = true)
@Composable
private fun ConnectPreviewLightTapped() = BlazingDurtlesTheme(darkTheme = false) {
    ConnectScreen(settingsTapped = true, onOpenSettings = {}, onHaveToken = {})
}

@Preview(name = "Connect — dark, before tap", showBackground = true)
@Composable
private fun ConnectPreviewDark() = BlazingDurtlesTheme(darkTheme = true) {
    ConnectScreen(settingsTapped = false, onOpenSettings = {}, onHaveToken = {})
}

@Preview(name = "Connect — dark, after tap", showBackground = true)
@Composable
private fun ConnectPreviewDarkTapped() = BlazingDurtlesTheme(darkTheme = true) {
    ConnectScreen(settingsTapped = true, onOpenSettings = {}, onHaveToken = {})
}
