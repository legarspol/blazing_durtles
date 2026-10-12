package com.smouldering_durtles.wk.ui.sync

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.ui.theme.BdTheme
import com.smouldering_durtles.wk.ui.theme.BlazingDurtlesTheme

/**
 * What the dashboard shows while the very first sync runs.
 *
 * Only on the first run. Every later sync keeps the one-line counter in `SyncProgressView`, which
 * is fine when the dashboard around it already has data to show; this exists because on a first
 * run it does not, and a new user would otherwise sit in front of an empty screen wondering
 * whether anything is happening.
 *
 * There is no Scaffold and no fixed height: this sits inside `activity_main.xml` under the
 * toolbar, so it takes the space it needs and lets the (empty) dashboard below absorb the rest.
 */
@Composable
fun FirstSyncScreen(state: SyncUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 26.dp, vertical = 20.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                SyncStrings.title,
                style = MaterialTheme.typography.headlineSmall,
                color = BdTheme.colors.textPrimary,
            )
            Text(
                SyncStrings.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = BdTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 20.dp, end = 20.dp),
            )
        }

        LinearProgressIndicator(
            progress = { state.progress },
            color = MaterialTheme.colorScheme.primary,
            trackColor = BdTheme.colors.track,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(5.dp)),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                SyncStrings.syncing,
                style = MaterialTheme.typography.labelMedium,
                color = BdTheme.colors.textTertiary,
            )
            if (state.showItemCount) {
                Text(
                    SyncStrings.items(state.itemsSynced, state.itemsTotal),
                    style = MaterialTheme.typography.labelMedium,
                    color = BdTheme.colors.textTertiary,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = BdTheme.colors.surfaceCard,
            modifier = Modifier.padding(top = 22.dp).fillMaxWidth(),
        ) {
            Column {
                state.rows.forEachIndexed { index, row ->
                    ChecklistRow(row)
                    if (index != state.rows.lastIndex) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(BdTheme.colors.divider)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(row: SyncRow) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (row.status) {
            SyncRowStatus.Done -> Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = BdTheme.colors.success,
                modifier = Modifier.size(22.dp),
            )
            SyncRowStatus.Running -> SpinningSync()
            SyncRowStatus.Waiting -> Icon(
                Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = BdTheme.colors.textDisabled,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(13.dp))
        Text(
            row.label,
            style = MaterialTheme.typography.titleSmall,
            color = if (row.status == SyncRowStatus.Waiting) {
                BdTheme.colors.textMuted
            } else {
                BdTheme.colors.textPrimary
            },
            modifier = Modifier.weight(1f),
        )
        Text(
            row.detail,
            style = MaterialTheme.typography.labelMedium,
            color = when (row.status) {
                SyncRowStatus.Running -> MaterialTheme.colorScheme.primary
                SyncRowStatus.Waiting -> BdTheme.colors.textMuted
                SyncRowStatus.Done -> BdTheme.colors.textTertiary
            },
        )
    }
}

@Composable
private fun SpinningSync() {
    val transition = rememberInfiniteTransition(label = "sync")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    Icon(
        Icons.Rounded.Sync,
        contentDescription = SyncStrings.runningDescription,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = angle },
    )
}

private val previewState = SyncUiState(
    visible = true,
    progress = 0.41f,
    itemsSynced = 3214,
    itemsTotal = 5020,
    rows = listOf(
        SyncRow(SyncStrings.rowProfile, SyncRowStatus.Done, SyncStrings.done),
        SyncRow(SyncStrings.rowSubjects, SyncRowStatus.Done, "2,431"),
        SyncRow(SyncStrings.rowAssignments, SyncRowStatus.Running, "783 / 2,589"),
        SyncRow(SyncStrings.rowForecast, SyncRowStatus.Waiting, SyncStrings.waiting),
    ),
)

@Preview(name = "First sync — light", showBackground = true)
@Composable
private fun FirstSyncPreviewLight() = BlazingDurtlesTheme(darkTheme = false) {
    FirstSyncScreen(previewState)
}

@Preview(name = "First sync — dark", showBackground = true)
@Composable
private fun FirstSyncPreviewDark() = BlazingDurtlesTheme(darkTheme = true) {
    FirstSyncScreen(previewState)
}

@Preview(name = "First sync — nothing reported yet", showBackground = true)
@Composable
private fun FirstSyncPreviewEarly() = BlazingDurtlesTheme(darkTheme = false) {
    FirstSyncScreen(
        SyncUiState(
            visible = true,
            progress = 0f,
            rows = listOf(
                SyncRow(SyncStrings.rowProfile, SyncRowStatus.Running, ""),
                SyncRow(SyncStrings.rowSubjects, SyncRowStatus.Waiting, SyncStrings.waiting),
                SyncRow(SyncStrings.rowAssignments, SyncRowStatus.Waiting, SyncStrings.waiting),
                SyncRow(SyncStrings.rowForecast, SyncRowStatus.Waiting, SyncStrings.waiting),
            ),
        )
    )
}
