package com.phapalesai.voicealert.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.phapalesai.voicealert.VoiceAlertApp
import com.phapalesai.voicealert.data.SpeakMode
import com.phapalesai.voicealert.ui.theme.*
import kotlinx.coroutines.launch

private data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

private fun loadInstalledApps(packageManager: PackageManager): List<InstalledApp> {
    val launcherApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { appInfo ->
            // Only apps that can actually post notifications a user would care about:
            // apps with a launcher entry (user-facing), including system apps like Phone/Messages.
            packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
        }
    return launcherApps
        .map { appInfo: ApplicationInfo ->
            InstalledApp(
                packageName = appInfo.packageName,
                label = packageManager.getApplicationLabel(appInfo).toString(),
                icon = try { packageManager.getApplicationIcon(appInfo.packageName) } catch (e: Exception) { null }
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

@Composable
fun AppSelectionScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = VoiceAlertApp.instance.preferencesRepository

    var searchQuery by remember { mutableStateOf("") }

    val installedApps = remember {
        loadInstalledApps(context.packageManager)
    }

    val savedRules by repository.appRulesFlow.collectAsState(initial = emptyMap())

    fun modeFor(packageName: String): SpeakMode {
        val saved = savedRules[packageName] ?: return SpeakMode.SPEAK
        return try { SpeakMode.valueOf(saved) } catch (e: IllegalArgumentException) { SpeakMode.SPEAK }
    }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    val speakOverCount = installedApps.count { modeFor(it.packageName) == SpeakMode.SPEAK_OVER }
    val disabledCount = installedApps.count { modeFor(it.packageName) == SpeakMode.DISABLED }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DeepSlateBg, DeepSlateBgSecondary, DeepSlateBg)
                )
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Notification Sources",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = TextPrimary
        )
        Text(
            text = "Choose how each app on your phone is spoken aloud",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryChip(
                modifier = Modifier.weight(1f),
                label = "$speakOverCount Speak Over",
                color = CrimsonAlert
            )
            SummaryChip(
                modifier = Modifier.weight(1f),
                label = "${installedApps.size - speakOverCount - disabledCount} Speak",
                color = EmeraldActive
            )
            SummaryChip(
                modifier = Modifier.weight(1f),
                label = "$disabledCount Disabled",
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed apps...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                focusedBorderColor = ElectricCyanBright,
                unfocusedBorderColor = SurfaceCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                val mode = modeFor(app.packageName)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceGlass),
                                contentAlignment = Alignment.Center
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon.toBitmap(width = 96, height = 96).asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                } else {
                                    Icon(Icons.Default.Apps, contentDescription = null, tint = TextSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
                                color = if (mode == SpeakMode.DISABLED) TextMuted else TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        SpeakModeSelector(
                            selected = mode,
                            onSelect = { newMode ->
                                scope.launch {
                                    repository.setAppMode(app.packageName, newMode.name)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(modifier: Modifier = Modifier, label: String, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = modifier.border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = color
            )
        }
    }
}

@Composable
private fun SpeakModeSelector(selected: SpeakMode, onSelect: (SpeakMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModeOption(
            modifier = Modifier.weight(1f),
            label = "Speak Over",
            active = selected == SpeakMode.SPEAK_OVER,
            color = CrimsonAlert,
            onClick = { onSelect(SpeakMode.SPEAK_OVER) }
        )
        ModeOption(
            modifier = Modifier.weight(1f),
            label = "Speak",
            active = selected == SpeakMode.SPEAK,
            color = EmeraldActive,
            onClick = { onSelect(SpeakMode.SPEAK) }
        )
        ModeOption(
            modifier = Modifier.weight(1f),
            label = "Disabled",
            active = selected == SpeakMode.DISABLED,
            color = TextMuted,
            onClick = { onSelect(SpeakMode.DISABLED) }
        )
    }
}

@Composable
private fun ModeOption(
    modifier: Modifier = Modifier,
    label: String,
    active: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.border(
            1.dp,
            if (active) color else SurfaceCardBorder,
            RoundedCornerShape(10.dp)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (active) color.copy(alpha = 0.18f) else DeepSlateBg
        )
    ) {
        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                ),
                color = if (active) color else TextSecondary
            )
        }
    }
}
