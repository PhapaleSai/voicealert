package com.phapalesai.voicealert.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.phapalesai.voicealert.VoiceAlertApp
import com.phapalesai.voicealert.data.SpeakMode
import com.phapalesai.voicealert.notification.VoiceNotificationListenerService
import com.phapalesai.voicealert.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

private enum class ModeFilter { ALL, SPEAK_OVER, SPEAK, DISABLED }

/**
 * Loading every installed app's icon via PackageManager is expensive (~150-250ms for 150+ apps).
 * Cached at the process level so returning to this tab after the first load is instant.
 */
private object InstalledAppsCache {
    var apps: List<InstalledApp>? = null
}

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
                icon = try {
                    packageManager.getApplicationIcon(appInfo.packageName)
                        .toBitmap(width = 96, height = 96)
                        .asImageBitmap()
                } catch (e: Exception) {
                    null
                }
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
    var activeFilter by remember { mutableStateOf(ModeFilter.ALL) }

    var installedApps by remember { mutableStateOf(InstalledAppsCache.apps ?: emptyList()) }
    var isLoading by remember { mutableStateOf(InstalledAppsCache.apps == null) }

    LaunchedEffect(Unit) {
        if (InstalledAppsCache.apps == null) {
            val loaded = withContext(Dispatchers.Default) {
                loadInstalledApps(context.packageManager)
            }
            InstalledAppsCache.apps = loaded
            installedApps = loaded
            isLoading = false
        }
    }

    val savedRules by repository.appRulesFlow.collectAsState(initial = emptyMap())
    val recentEvents by VoiceNotificationListenerService.recentEventsFlow.collectAsState(initial = emptyList())

    fun modeFor(packageName: String): SpeakMode {
        val saved = savedRules[packageName] ?: return SpeakMode.SPEAK
        return try { SpeakMode.valueOf(saved) } catch (e: IllegalArgumentException) { SpeakMode.SPEAK }
    }

    val recentPackageNames = remember(recentEvents) {
        recentEvents.map { it.packageName }.distinct()
    }
    val recentApps = remember(recentPackageNames, installedApps) {
        recentPackageNames.mapNotNull { pkg -> installedApps.find { it.packageName == pkg } }
    }

    val filteredApps = remember(searchQuery, installedApps, activeFilter, savedRules) {
        installedApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() || app.label.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (activeFilter) {
                ModeFilter.ALL -> true
                ModeFilter.SPEAK_OVER -> modeFor(app.packageName) == SpeakMode.SPEAK_OVER
                ModeFilter.SPEAK -> modeFor(app.packageName) == SpeakMode.SPEAK
                ModeFilter.DISABLED -> modeFor(app.packageName) == SpeakMode.DISABLED
            }
            matchesSearch && matchesFilter
        }
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
                color = CrimsonAlert,
                selected = activeFilter == ModeFilter.SPEAK_OVER,
                onClick = { activeFilter = if (activeFilter == ModeFilter.SPEAK_OVER) ModeFilter.ALL else ModeFilter.SPEAK_OVER }
            )
            SummaryChip(
                modifier = Modifier.weight(1f),
                label = "${installedApps.size - speakOverCount - disabledCount} Speak",
                color = EmeraldActive,
                selected = activeFilter == ModeFilter.SPEAK,
                onClick = { activeFilter = if (activeFilter == ModeFilter.SPEAK) ModeFilter.ALL else ModeFilter.SPEAK }
            )
            SummaryChip(
                modifier = Modifier.weight(1f),
                label = "$disabledCount Disabled",
                color = TextMuted,
                selected = activeFilter == ModeFilter.DISABLED,
                onClick = { activeFilter = if (activeFilter == ModeFilter.DISABLED) ModeFilter.ALL else ModeFilter.DISABLED }
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

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricCyanBright)
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (recentApps.isNotEmpty() && searchQuery.isBlank() && activeFilter == ModeFilter.ALL) {
                        item(key = "recent_header") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = ElectricCyanBright, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Recently Notified",
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    color = ElectricCyanBright
                                )
                            }
                        }
                        items(recentApps, key = { "recent_${it.packageName}" }) { app ->
                            AppRuleCard(
                                app = app,
                                mode = modeFor(app.packageName),
                                onSelect = { newMode ->
                                    scope.launch { repository.setAppMode(app.packageName, newMode.name) }
                                }
                            )
                        }
                        item(key = "recent_divider") {
                            HorizontalDivider(color = SurfaceCardBorder, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }

                    items(filteredApps, key = { it.packageName }) { app ->
                        AppRuleCard(
                            app = app,
                            mode = modeFor(app.packageName),
                            onSelect = { newMode ->
                                scope.launch { repository.setAppMode(app.packageName, newMode.name) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRuleCard(
    app: InstalledApp,
    mode: SpeakMode,
    onSelect: (SpeakMode) -> Unit
) {
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
                            bitmap = app.icon,
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

            SpeakModeSelector(selected = mode, onSelect = onSelect)
        }
    }
}

@Composable
private fun SummaryChip(
    modifier: Modifier = Modifier,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = onClick ?: {},
        modifier = modifier.border(
            if (selected) 1.5.dp else 1.dp,
            if (selected) color else color.copy(alpha = 0.5f),
            RoundedCornerShape(14.dp)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) color.copy(alpha = 0.18f) else SurfaceCard
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = color,
                maxLines = 1
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                ),
                color = if (active) color else TextSecondary,
                maxLines = 1
            )
        }
    }
}
