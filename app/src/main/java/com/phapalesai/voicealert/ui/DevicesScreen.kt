package com.phapalesai.voicealert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phapalesai.voicealert.data.DeviceProfile
import com.phapalesai.voicealert.data.DeviceType
import com.phapalesai.voicealert.ui.theme.*

@Composable
fun DevicesScreen(
    currentDevice: DeviceProfile?,
    onDeviceTypeSelected: (DeviceType) -> Unit
) {
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
                text = "Bluetooth Devices",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = TextPrimary
            )
            Text(
                text = "Classify connected devices to prevent accidental notification leaks",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        if (currentDevice == null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Bluetooth Device Connected",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connect headphones, a car system, or a speaker to classify it here.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            return@LazyColumn
        }

        val activeDevice = currentDevice

        // Active Device Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(ElectricCyanBright, EmeraldActive)),
                        RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceGlass)
                                    .border(1.dp, ElectricCyanBright, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (activeDevice.type) {
                                        DeviceType.PERSONAL -> Icons.Default.Headphones
                                        DeviceType.CAR -> Icons.Default.DirectionsCar
                                        DeviceType.SHARED -> Icons.Default.Speaker
                                        DeviceType.SILENT -> Icons.Default.VolumeOff
                                        DeviceType.OTHER -> Icons.Default.Headphones
                                    },
                                    contentDescription = null,
                                    tint = ElectricCyanBright,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = activeDevice.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Connected • MAC: ${activeDevice.address}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = EmeraldMint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (activeDevice.batteryLevel != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceGlass)
                                    .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = batteryIconFor(activeDevice.batteryLevel),
                                    contentDescription = null,
                                    tint = batteryColorFor(activeDevice.batteryLevel),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${activeDevice.batteryLevel}%",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                    color = batteryColorFor(activeDevice.batteryLevel)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Divider(color = SurfaceCardBorder)
                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Select Device Profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DeviceTypeChip(
                        title = "Personal Headphones / Earbuds",
                        description = "Private messages, bank alerts & calls spoken freely",
                        icon = Icons.Default.Headphones,
                        color = EmeraldActive,
                        selected = activeDevice.type == DeviceType.PERSONAL,
                        onSelect = { onDeviceTypeSelected(DeviceType.PERSONAL) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DeviceTypeChip(
                        title = "Car Audio System",
                        description = "Blocks private SMS/WhatsApp when family/friends are in car",
                        icon = Icons.Default.DirectionsCar,
                        color = SolarGold,
                        selected = activeDevice.type == DeviceType.CAR,
                        onSelect = { onDeviceTypeSelected(DeviceType.CAR) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DeviceTypeChip(
                        title = "Home / Public Speaker",
                        description = "Speaks quietly, sender name only — no loud blurted messages",
                        icon = Icons.Default.Speaker,
                        color = SolarGold,
                        selected = activeDevice.type == DeviceType.SHARED,
                        onSelect = { onDeviceTypeSelected(DeviceType.SHARED) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DeviceTypeChip(
                        title = "Silent / Off",
                        description = "Completely switches off voice announcements",
                        icon = Icons.Default.VolumeOff,
                        color = CrimsonAlert,
                        selected = activeDevice.type == DeviceType.SILENT,
                        onSelect = { onDeviceTypeSelected(DeviceType.SILENT) }
                    )
                }
            }
        }

        // Privacy Behavior Matrix Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Active Profile Rules Summary",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                        color = ElectricCyanBright
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val (isVoiceOn, isCallsOn, isPrivacyOn) = when (activeDevice.type) {
                        DeviceType.PERSONAL -> Triple("Voice ON (100%)", "Calls Allowed", "Full Details")
                        DeviceType.CAR -> Triple("Calls/Nav Only", "Calls Allowed", "Sender Only")
                        DeviceType.SHARED -> Triple("Voice ON (quiet)", "Calls Allowed", "Sender Only")
                        DeviceType.SILENT -> Triple("Voice OFF", "Silent", "Blocked")
                        DeviceType.OTHER -> Triple("Voice ON", "Allowed", "Standard")
                    }

                    RuleCheckRow(title = "Notification Voice Speech", status = isVoiceOn, isGood = activeDevice.type != DeviceType.SILENT)
                    RuleCheckRow(title = "Phone Call Announcements", status = isCallsOn, isGood = true)
                    RuleCheckRow(title = "Privacy Mode Protection", status = isPrivacyOn, isGood = true)
                }
            }
        }
    }
}

@Composable
fun RuleCheckRow(title: String, status: String, isGood: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isGood) EmeraldActive else TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = TextPrimary
            )
        }

        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = if (isGood) EmeraldMint else TextMuted
        )
    }
}

@Composable
fun DeviceTypeChip(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (selected) color else SurfaceCardBorder,
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) SurfaceGlass else SurfaceCard
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    color = if (selected) color else TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }
        }
    }
}

private fun batteryIconFor(level: Int): ImageVector = when {
    level <= 15 -> Icons.Default.BatteryAlert
    level <= 50 -> Icons.Default.Battery3Bar
    level <= 85 -> Icons.Default.Battery5Bar
    else -> Icons.Default.BatteryFull
}

private fun batteryColorFor(level: Int): Color = when {
    level <= 15 -> CrimsonAlert
    level <= 30 -> SolarGold
    else -> EmeraldMint
}
