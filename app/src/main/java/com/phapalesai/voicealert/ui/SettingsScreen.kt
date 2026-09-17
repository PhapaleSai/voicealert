package com.phapalesai.voicealert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phapalesai.voicealert.R
import com.phapalesai.voicealert.ui.theme.*

@Composable
fun SettingsScreen(
    preferredLanguage: String,
    onLanguageChange: (String) -> Unit,
    privacyMode: Boolean,
    onPrivacyModeChange: (Boolean) -> Unit,
    otpProtection: Boolean,
    onOtpProtectionChange: (Boolean) -> Unit,
    bankMasking: Boolean,
    onBankMaskingChange: (Boolean) -> Unit,
    quietHoursEnabled: Boolean,
    onQuietHoursEnabledChange: (Boolean) -> Unit,
    quietHoursStart: Int,
    quietHoursEnd: Int,
    onQuietHoursRangeChange: (start: Int, end: Int) -> Unit,
    respectDnd: Boolean,
    onRespectDndChange: (Boolean) -> Unit
) {
    var speechSpeed by remember { mutableFloatStateOf(1.0f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DeepSlateBg, DeepSlateBgSecondary, DeepSlateBg)
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_tab),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = TextPrimary
            )
            Text(
                text = "Language routing, audio speeds, and privacy protection settings",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Language Router Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = ElectricCyanBright,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.language_selection),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            color = ElectricCyanBright
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val languages = listOf(
                        Triple("auto", stringResource(R.string.auto_detect), "Detects English, Hindi & Marathi automatically"),
                        Triple("en", stringResource(R.string.english), "Speaks alerts in Indian Accent English (en_IN)"),
                        Triple("hi", stringResource(R.string.hindi), "Speaks alerts in Hindi (hi_IN)"),
                        Triple("mr", stringResource(R.string.marathi), "Speaks alerts in Marathi (mr_IN)")
                    )

                    languages.forEach { (code, label, desc) ->
                        Card(
                            onClick = { onLanguageChange(code) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(
                                    1.dp,
                                    if (preferredLanguage == code) EmeraldActive else SurfaceCardBorder,
                                    RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (preferredLanguage == code) SurfaceGlass else SurfaceCard
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = preferredLanguage == code,
                                    onClick = { onLanguageChange(code) },
                                    colors = RadioButtonDefaults.colors(selectedColor = EmeraldActive)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = if (preferredLanguage == code) EmeraldMint else TextPrimary
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Privacy Protection Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = AuroraPurpleGlow,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Privacy Shield",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            color = AuroraPurpleGlow
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingToggleRow(
                        title = stringResource(R.string.privacy_mode),
                        subtitle = "Sender-only mode (Hides message contents)",
                        checked = privacyMode,
                        onCheckedChange = onPrivacyModeChange
                    )

                    Divider(color = SurfaceCardBorder, modifier = Modifier.padding(vertical = 10.dp))

                    SettingToggleRow(
                        title = stringResource(R.string.otp_privacy),
                        subtitle = "Masks numeric verification codes in SMS",
                        checked = otpProtection,
                        onCheckedChange = onOtpProtectionChange
                    )

                    Divider(color = SurfaceCardBorder, modifier = Modifier.padding(vertical = 10.dp))

                    SettingToggleRow(
                        title = stringResource(R.string.bank_masking),
                        subtitle = "Hides account numbers & cleans transaction text",
                        checked = bankMasking,
                        onCheckedChange = onBankMaskingChange
                    )
                }
            }
        }

        // Quiet Hours & Do Not Disturb Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = null,
                            tint = ElectricCyanBright,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Quiet Hours & Do Not Disturb",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            color = ElectricCyanBright
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingToggleRow(
                        title = "Quiet Hours",
                        subtitle = "Stay silent overnight or during a set window every day",
                        checked = quietHoursEnabled,
                        onCheckedChange = onQuietHoursEnabledChange
                    )

                    if (quietHoursEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HourStepper(
                                modifier = Modifier.weight(1f),
                                label = "From",
                                hour = quietHoursStart,
                                onHourChange = { newStart -> onQuietHoursRangeChange(newStart, quietHoursEnd) }
                            )
                            HourStepper(
                                modifier = Modifier.weight(1f),
                                label = "Until",
                                hour = quietHoursEnd,
                                onHourChange = { newEnd -> onQuietHoursRangeChange(quietHoursStart, newEnd) }
                            )
                        }
                    }

                    Divider(color = SurfaceCardBorder, modifier = Modifier.padding(vertical = 10.dp))

                    SettingToggleRow(
                        title = "Respect System Do Not Disturb",
                        subtitle = "Stay silent whenever your phone's own DND is on",
                        checked = respectDnd,
                        onCheckedChange = onRespectDndChange
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Apps set to \"Speak Over\" always break through both of these.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextMuted
                    )
                }
            }
        }

        // Audio Speed Slider Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = SolarGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Speech Rate Speed",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "${"%.1f".format(speechSpeed)}x",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = SolarGold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = speechSpeed,
                        onValueChange = { speechSpeed = it },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = SolarGold,
                            activeTrackColor = SolarGold,
                            inactiveTrackColor = SurfaceCardBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0.5x (Slow)", fontSize = 11.sp, color = TextMuted)
                        Text("1.0x (Normal)", fontSize = 11.sp, color = TextMuted)
                        Text("2.0x (Fast)", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

private fun formatHour(hour: Int): String {
    val normalized = ((hour % 24) + 24) % 24
    val period = if (normalized < 12) "AM" else "PM"
    val display = when {
        normalized == 0 -> 12
        normalized > 12 -> normalized - 12
        else -> normalized
    }
    return "$display:00 $period"
}

@Composable
private fun HourStepper(
    modifier: Modifier = Modifier,
    label: String,
    hour: Int,
    onHourChange: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HourStepButton(
                icon = Icons.Default.Remove,
                contentDescription = "Earlier",
                onClick = { onHourChange(((hour - 1) % 24 + 24) % 24) }
            )
            Text(
                text = formatHour(hour),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = ElectricCyanBright,
                maxLines = 1,
                softWrap = false
            )
            HourStepButton(
                icon = Icons.Default.Add,
                contentDescription = "Later",
                onClick = { onHourChange((hour + 1) % 24) }
            )
        }
    }
}

@Composable
private fun HourStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(DeepSlateBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = TextSecondary, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmeraldActive,
                checkedTrackColor = EmeraldGlow
            )
        )
    }
}
