package com.phapalesai.voicealert.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phapalesai.voicealert.data.AppRule
import com.phapalesai.voicealert.data.Priority
import com.phapalesai.voicealert.ui.theme.*

@Composable
fun AppSelectionScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val defaultApps = remember {
        mutableStateListOf(
            AppRule("com.whatsapp", "WhatsApp", "Messaging", true, Priority.NORMAL),
            AppRule("com.google.android.dialer", "Phone & Calls", "Calls", true, Priority.CRITICAL),
            AppRule("com.google.android.apps.messaging", "SMS Messages", "Messaging", true, Priority.HIGH),
            AppRule("com.google.android.apps.maps", "Google Maps", "Navigation", true, Priority.CRITICAL),
            AppRule("com.sbi.upi", "YONO SBI Bank", "Banking", true, Priority.HIGH),
            AppRule("com.phonepe.app", "PhonePe Payments", "Banking", true, Priority.HIGH),
            AppRule("com.google.android.apps.nfcpayment", "Google Pay", "Banking", true, Priority.HIGH),
            AppRule("com.instagram.android", "Instagram", "Social", false, Priority.LOW),
            AppRule("com.google.android.youtube", "YouTube", "Social", false, Priority.LOW)
        )
    }

    val categories = listOf("All", "Messaging", "Banking", "Calls", "Navigation", "Social")

    val filteredApps = defaultApps.filter { app ->
        val matchesSearch = app.displayName.contains(searchQuery, ignoreCase = true) || app.category.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || app.category.equals(selectedCategory, ignoreCase = true)
        matchesSearch && matchesCategory
    }

    val activeCount = defaultApps.count { it.enabled }
    val silencedCount = defaultApps.count { !it.enabled }

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
            text = "Control which applications VoiceAlert is allowed to speak",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Active vs Silenced Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, EmeraldActive.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EmeraldActive)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$activeCount Apps Enabled",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = EmeraldMint
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(TextMuted)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$silencedCount Apps Silenced",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search messaging, banking, maps...", color = TextMuted) },
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

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(category, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan,
                        selectedLabelColor = DeepSlateBg,
                        containerColor = SurfaceCard,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) ElectricCyanBright else SurfaceCardBorder,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredApps) { app ->
                val icon = when (app.category) {
                    "Messaging" -> Icons.Default.ChatBubble
                    "Calls" -> Icons.Default.Call
                    "Banking" -> Icons.Default.AccountBalance
                    "Navigation" -> Icons.Default.Navigation
                    else -> Icons.Default.Apps
                }

                val accentColor = when (app.priority) {
                    Priority.CRITICAL -> CrimsonAlert
                    Priority.HIGH -> SolarGold
                    Priority.NORMAL -> EmeraldActive
                    Priority.LOW -> TextMuted
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (app.enabled) SurfaceCardBorder else SurfaceCardBorder.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceGlass)
                                .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = app.displayName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                                    color = if (app.enabled) TextPrimary else TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = app.category,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = TextSecondary
                                )
                                Text(text = " • ", color = TextMuted, fontSize = 12.sp)
                                Text(
                                    text = app.priority.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor
                                )
                            }
                        }

                        Switch(
                            checked = app.enabled,
                            onCheckedChange = { isChecked ->
                                val index = defaultApps.indexOf(app)
                                if (index != -1) {
                                    defaultApps[index] = app.copy(enabled = isChecked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepSlateBg,
                                checkedTrackColor = EmeraldActive,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceCardBorder
                            )
                        )
                    }
                }
            }
        }
    }
}
