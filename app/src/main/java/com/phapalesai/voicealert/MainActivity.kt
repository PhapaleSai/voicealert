package com.phapalesai.voicealert

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phapalesai.voicealert.bluetooth.BluetoothDeviceManager
import com.phapalesai.voicealert.data.DeviceProfile
import com.phapalesai.voicealert.notification.VoiceNotificationListenerService
import com.phapalesai.voicealert.ui.*
import com.phapalesai.voicealert.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Results aren't required immediately; flows re-read state (e.g. call detection) lazily. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bluetoothManager = VoiceAlertApp.instance.bluetoothManager

        requestRuntimePermissions()

        setContent {
            VoiceAlertTheme {
                MainAppHost(bluetoothManager = bluetoothManager)
            }
        }
    }

    /**
     * The manifest declares POST_NOTIFICATIONS, BLUETOOTH_CONNECT and READ_PHONE_STATE, but
     * declaring a dangerous permission alone doesn't grant it — without this request, features
     * like device-type detection and stopping speech when a call is answered silently no-op.
     */
    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            needed += Manifest.permission.BLUETOOTH_CONNECT
        }
        needed += Manifest.permission.READ_PHONE_STATE

        val toRequest = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }
}

@Composable
fun MainAppHost(bluetoothManager: BluetoothDeviceManager) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = VoiceAlertApp.instance.preferencesRepository
    val scope = rememberCoroutineScope()

    val travelMode by repository.travelModeFlow.collectAsState(initial = true)
    val preferredLanguage by repository.preferredLanguageFlow.collectAsState(initial = "auto")
    val privacyMode by repository.privacyModeFlow.collectAsState(initial = false)
    val otpProtection by repository.otpProtectionFlow.collectAsState(initial = true)
    val bankMasking by repository.bankMaskingFlow.collectAsState(initial = true)
    val quietHoursEnabled by repository.quietHoursEnabledFlow.collectAsState(initial = false)
    val quietHoursStart by repository.quietHoursStartFlow.collectAsState(initial = 22)
    val quietHoursEnd by repository.quietHoursEndFlow.collectAsState(initial = 7)
    val respectDnd by repository.respectDndFlow.collectAsState(initial = true)

    val currentDevice by bluetoothManager.connectedDeviceFlow.collectAsState(initial = null)
    val recentEvents by VoiceNotificationListenerService.recentEventsFlow.collectAsState(initial = emptyList())

    val hasNotificationPermission = remember(context) {
        checkNotificationPermission(context)
    }

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                contentColor = TextPrimary
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.home_tab)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldActive,
                        selectedTextColor = EmeraldActive,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = SurfaceCardBorder
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                    label = { Text(stringResource(R.string.apps_tab)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldActive,
                        selectedTextColor = EmeraldActive,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = SurfaceCardBorder
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Devices, contentDescription = null) },
                    label = { Text(stringResource(R.string.devices_tab)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldActive,
                        selectedTextColor = EmeraldActive,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = SurfaceCardBorder
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.SettingsIcon, contentDescription = null) },
                    label = { Text(stringResource(R.string.settings_tab)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldActive,
                        selectedTextColor = EmeraldActive,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = SurfaceCardBorder
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> HomeScreen(
                    travelMode = travelMode,
                    onTravelModeChange = { enabled ->
                        scope.launch { repository.setTravelMode(enabled) }
                    },
                    hasNotificationPermission = hasNotificationPermission,
                    preferredLanguage = preferredLanguage,
                    recentEvents = recentEvents,
                    currentDevice = currentDevice
                )
                1 -> AppSelectionScreen()
                2 -> DevicesScreen(
                    currentDevice = currentDevice,
                    onDeviceTypeSelected = { type ->
                        bluetoothManager.updateDeviceType(type)
                    }
                )
                3 -> SettingsScreen(
                    preferredLanguage = preferredLanguage,
                    onLanguageChange = { lang ->
                        scope.launch { repository.setPreferredLanguage(lang) }
                    },
                    privacyMode = privacyMode,
                    onPrivacyModeChange = { enabled ->
                        scope.launch { repository.setPrivacyMode(enabled) }
                    },
                    otpProtection = otpProtection,
                    onOtpProtectionChange = { enabled ->
                        scope.launch { repository.setOtpProtection(enabled) }
                    },
                    bankMasking = bankMasking,
                    onBankMaskingChange = { enabled ->
                        scope.launch { repository.setBankMasking(enabled) }
                    },
                    quietHoursEnabled = quietHoursEnabled,
                    onQuietHoursEnabledChange = { enabled ->
                        scope.launch { repository.setQuietHoursEnabled(enabled) }
                    },
                    quietHoursStart = quietHoursStart,
                    quietHoursEnd = quietHoursEnd,
                    onQuietHoursRangeChange = { start, end ->
                        scope.launch { repository.setQuietHoursRange(start, end) }
                    },
                    respectDnd = respectDnd,
                    onRespectDndChange = { enabled ->
                        scope.launch { repository.setRespectDnd(enabled) }
                    }
                )
            }
        }
    }
}

fun checkNotificationPermission(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    val packageName = context.packageName
    return enabledListeners != null && enabledListeners.contains(packageName)
}
