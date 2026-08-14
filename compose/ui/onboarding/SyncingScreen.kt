package com.blazingdurtles.ui.onboarding

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blazingdurtles.ui.theme.BdTheme
import com.blazingdurtles.ui.theme.EmberBright
import com.blazingdurtles.ui.theme.EmberDeep

/** State of a single sync task in the checklist. */
enum class SyncStatus { DONE, RUNNING, WAITING }

data class SyncTask(val label: String, val detail: String, val status: SyncStatus)

data class SyncUiState(
    val userName: String,
    val level: Int,
    val subtitle: String,
    val itemsSynced: Int,
    val itemsTotal: Int,
    val tasks: List<SyncTask>,
) {
    val progress: Float get() = if (itemsTotal == 0) 0f else itemsSynced.toFloat() / itemsTotal
}

/**
 * Screen 04 — Identified + Syncing (v2 "Less"; replaces v1's Verified + All-set).
 * Once the token identifies the user, the input is gone: we show the account and pull data.
 * When [state] completes, the host navigates to the dashboard.
 */
@Composable
fun SyncingScreen(state: SyncUiState) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).padding(horizontal = 26.dp, vertical = 14.dp)
        ) {
            IdentifiedCard(state.userName, state.level, state.subtitle)

            Column(
                Modifier.padding(top = 30.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Setting things up…", style = MaterialTheme.typography.headlineSmall,
                    color = BdTheme.colors.textPrimary)
                Text(
                    "Pulling your progress from WaniKani. This only happens once.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = BdTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 20.dp, end = 20.dp)
                )
            }

            LinearProgressIndicator(
                progress = { state.progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = BdTheme.colors.track,
                modifier = Modifier.fillMaxWidth().height(8.dp)
                    .padding(top = 22.dp).clip(RoundedCornerShape(5.dp))
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Syncing", style = MaterialTheme.typography.labelMedium,
                    color = BdTheme.colors.textTertiary)
                Text("${state.itemsSynced} / ${state.itemsTotal} items",
                    style = MaterialTheme.typography.labelMedium, color = BdTheme.colors.textTertiary)
            }

            Surface(
                shape = RoundedCornerShape(24.dp), color = BdTheme.colors.surfaceCard,
                modifier = Modifier.padding(top = 22.dp).fillMaxWidth()
            ) {
                Column {
                    state.tasks.forEachIndexed { i, task ->
                        SyncRow(task)
                        if (i != state.tasks.lastIndex) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(BdTheme.colors.divider))
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Text(
                "You'll land on your dashboard automatically.",
                style = MaterialTheme.typography.labelMedium,
                color = BdTheme.colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun IdentifiedCard(name: String, level: Int, subtitle: String) {
    Surface(shape = RoundedCornerShape(24.dp), color = BdTheme.colors.surfaceCard,
        modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(54.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(EmberBright, EmberDeep))),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium,
                    color = BdTheme.colors.textPrimary)
                Row(
                    Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = CircleShape, color = BdTheme.colors.container) {
                        Text("Level $level", color = BdTheme.colors.emberInk,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp))
                    }
                    Text(subtitle, style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Normal), color = BdTheme.colors.textSecondary)
                }
            }
            Icon(Icons.Rounded.Verified, contentDescription = null,
                tint = BdTheme.colors.success, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun SyncRow(task: SyncTask) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (task.status) {
            SyncStatus.DONE -> Icon(Icons.Rounded.CheckCircle, null,
                tint = BdTheme.colors.success, modifier = Modifier.size(22.dp))
            SyncStatus.RUNNING -> SpinningSync()
            SyncStatus.WAITING -> Icon(Icons.Rounded.RadioButtonUnchecked, null,
                tint = BdTheme.colors.textDisabled, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.size(13.dp))
        Text(
            task.label,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.5f.sp),
            color = if (task.status == SyncStatus.WAITING) BdTheme.colors.textMuted
            else BdTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            task.detail,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (task.status == SyncStatus.RUNNING) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = when (task.status) {
                SyncStatus.RUNNING -> MaterialTheme.colorScheme.primary
                SyncStatus.WAITING -> BdTheme.colors.textMuted
                SyncStatus.DONE -> BdTheme.colors.textTertiary
            }
        )
    }
}

@Composable
private fun SpinningSync() {
    val transition = rememberInfiniteTransition(label = "sync")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    Icon(Icons.Rounded.Sync, contentDescription = "Syncing",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = angle })
}
