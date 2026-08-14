package com.blazingdurtles.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blazingdurtles.ui.components.OnboardingTopBar
import com.blazingdurtles.ui.components.PermissionChip
import com.blazingdurtles.ui.components.PrimaryButton
import com.blazingdurtles.ui.components.StepBadge
import com.blazingdurtles.ui.components.TonalButton
import com.blazingdurtles.ui.theme.BdTheme

private val SCOPES = listOf(
    "assignments:start", "reviews:create",
    "study_materials:create", "study_materials:update"
)

/**
 * Screen 02 — Connect (v2 "Less").
 * Changed vs. v1: removed the "go to Settings → API Tokens" step (it contradicted the
 * "Open WaniKani settings" button, which now performs that navigation). Privacy note moved
 * up above the steps. Two steps remain: Generate → Copy.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onHaveToken: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            OnboardingTopBar("Connect your account", onBack)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 26.dp)
            ) {
                Text(
                    "Blazing Durtles syncs with your free WaniKani account using an API token. " +
                        "The button below opens the right page for you.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = BdTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Privacy — moved up
                PrivacyNote(Modifier.padding(bottom = 6.dp))

                StepRow(
                    number = 1,
                    title = "Generate a new token",
                    body = "On the page that opens, tap Generate a new token and switch on these four permissions:"
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.padding(top = 11.dp)
                    ) { SCOPES.forEach { PermissionChip(it) } }
                }

                StepRow(
                    number = 2,
                    title = "Copy it & come back",
                    body = "Paste the token on the next screen."
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                TonalButton("Open WaniKani settings", onOpenSettings, Icons.Rounded.OpenInNew)
                PrimaryButton("I've got my token", onHaveToken, trailingIcon = null)
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
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = BdTheme.colors.textPrimary)
            Text(body, style = MaterialTheme.typography.bodyMedium,
                color = BdTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 3.dp))
            extra?.invoke()
        }
    }
}

@Composable
private fun PrivacyNote(modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(18.dp), color = BdTheme.colors.surfaceCard,
        modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Icon(Icons.Rounded.Lock, contentDescription = null,
                tint = BdTheme.colors.emberInk, modifier = Modifier.size(19.dp))
            Spacer(Modifier.size(11.dp))
            Text(
                buildString {
                    append("We store only your API token, used solely to talk to WaniKani. ")
                },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = BdTheme.colors.textSecondary
            )
        }
    }
}
