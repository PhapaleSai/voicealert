package com.phapalesai.voicealert.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phapalesai.voicealert.R
import com.phapalesai.voicealert.data.NotificationEvent
import com.phapalesai.voicealert.notification.VoiceNotificationListenerService
import com.phapalesai.voicealert.ui.theme.*

@Composable
fun HomeScreen(
    travelMode: Boolean,
    onTravelModeChange: (Boolean) -> Unit,
    hasNotificationPermission: Boolean,
    preferredLanguage: String,
    recentEvents: List<NotificationEvent>
) {
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val auraPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraScale"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

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
        // App Header with Glowing Branding
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(EmeraldActive, ElectricCyan)
                                    )
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = DeepSlateBg
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricCyanBright
                    )
                }

                // Security permission badge button
                IconButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceGlass)
                        .border(1.dp, SurfaceGlassBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = if (hasNotificationPermission) Icons.Default.Shield else Icons.Default.Warning,
                        contentDescription = "Permissions",
                        tint = if (hasNotificationPermission) EmeraldActive else CrimsonAlert
                    )
                }
            }
        }

        // Warning Permission Banner if not granted
        if (!hasNotificationPermission) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, CrimsonAlert, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CrimsonAlert,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.permission_banner_title),
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                                color = TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.permission_banner_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.grant_permission), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Hero Master Portal Card (Travel Mode)
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Ambient glowing background aura
                if (travelMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(180.dp)
                            .scale(auraPulseScale)
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        EmeraldGlow.copy(alpha = auraAlpha),
                                        ElectricCyan.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.5.dp,
                            if (travelMode) Brush.horizontalGradient(
                                listOf(EmeraldActive, ElectricCyanBright)
                            ) else Brush.horizontalGradient(listOf(SurfaceCardBorder, SurfaceCardBorder)),
                            RoundedCornerShape(28.dp)
                        )
                        .shadow(16.dp, RoundedCornerShape(28.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (travelMode) EmeraldActive else TextMuted)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.travel_mode),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextPrimary
                                )
                            }

                            Switch(
                                checked = travelMode,
                                onCheckedChange = onTravelModeChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DeepSlateBg,
                                    checkedTrackColor = EmeraldActive,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SurfaceCardBorder
                                ),
                                modifier = Modifier.scale(1.2f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (travelMode) stringResource(R.string.travel_mode_active) else stringResource(R.string.travel_mode_inactive),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = if (travelMode) EmeraldMint else TextSecondary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Audio Equalizer Spectrum Visualizer (12 Frequency Bars)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(48.dp)
                        ) {
                            val barsCount = 12
                            repeat(barsCount) { index ->
                                val targetHeight = if (travelMode) (16 + (index % 5) * 8 + (if (index % 2 == 0) 12 else 4)).toFloat() else 6f
                                val barHeight by infiniteTransition.animateFloat(
                                    initialValue = 6f,
                                    targetValue = targetHeight,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(
                                            durationMillis = 350 + (index * 60),
                                            easing = FastOutSlowInEasing
                                        ),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "barHeight_$index"
                                )

                                Box(
                                    modifier = Modifier
                                        .width(7.dp)
                                        .height(if (travelMode) barHeight.dp else 6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (travelMode) {
                                                Brush.verticalGradient(
                                                    listOf(ElectricCyanBright, EmeraldActive, AuroraViolet)
                                                )
                                            } else {
                                                Brush.verticalGradient(listOf(TextMuted, TextMuted))
                                            }
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Connected Bluetooth Device Status Badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceGlass)
                                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = ElectricCyanBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OnePlus Buds Pro • Personal Earbuds",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Live Stats Grid (Alerts Spoken, Privacy Protected, Engine Mode)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Spoken Today",
                    value = "${recentEvents.size}",
                    icon = Icons.Default.RecordVoiceOver,
                    color = EmeraldActive
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Privacy Shield",
                    value = "Active",
                    icon = Icons.Default.Security,
                    color = AuroraViolet
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Language",
                    value = preferredLanguage.uppercase(),
                    icon = Icons.Default.Translate,
                    color = ElectricCyan
                )
            }
        }

        // Interactive Voice Alert Simulator / Soundboard (EN / HI / MR)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Voice Alert Simulator",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap any alert scenario to test voice output in headphones",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = TextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = ElectricCyanBright
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Horizontal Soundboard Simulator Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            SimulationChip(
                                appName = "WhatsApp",
                                message = "Rahul: Bro, where are you right now?",
                                langCode = "en",
                                badgeText = "EN",
                                color = EmeraldActive
                            )
                        }
                        item {
                            SimulationChip(
                                appName = "WhatsApp",
                                message = "राहुल कडून संदेश: मित्रा, तू कधी पोहोचशील?",
                                langCode = "mr",
                                badgeText = "मराठी",
                                color = ElectricCyan
                            )
                        }
                        item {
                            SimulationChip(
                                appName = "WhatsApp",
                                message = "राहुल से संदेश: भाई, तुम कब तक पहुंचोगे?",
                                langCode = "hi",
                                badgeText = "हिन्दी",
                                color = AuroraViolet
                            )
                        }
                        item {
                            SimulationChip(
                                appName = "Banking Alert",
                                message = "HDFC Bank: Rs 4,500 credited to your account.",
                                langCode = "en",
                                badgeText = "Bank SMS",
                                color = SolarGold
                            )
                        }
                        item {
                            SimulationChip(
                                appName = "Security OTP",
                                message = "Your Google verification code is 849201.",
                                langCode = "en",
                                badgeText = "OTP Safe",
                                color = CrimsonAlert
                            )
                        }
                    }
                }
            }
        }

        // Spoken Alerts Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.recent_alerts),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = TextPrimary
                )

                if (recentEvents.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { VoiceNotificationListenerService.activeService?.repeatLastAlert() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = null,
                                tint = ElectricCyanBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Repeat",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                color = ElectricCyanBright
                            )
                        }
                        Text(
                            text = "${recentEvents.size} items",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = TextMuted
                        )
                    }
                }
            }
        }

        if (recentEvents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Headset,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.no_alerts_yet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            items(recentEvents) { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceGlass)
                                .border(1.dp, EmeraldActive.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = EmeraldActive,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = event.appName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                                    color = ElectricCyanBright
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AuroraViolet.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = event.language.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = AuroraPurpleGlow
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = event.spokenText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.border(1.dp, SurfaceCardBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = TextPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = TextSecondary
            )
        }
    }
}

@Composable
fun SimulationChip(
    appName: String,
    message: String,
    langCode: String,
    badgeText: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable {
                VoiceNotificationListenerService.activeService?.speakTestMessage(message, langCode)
            },
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 13.sp),
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = color
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Tap to speak",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = color
                )
            }
        }
    }
}
