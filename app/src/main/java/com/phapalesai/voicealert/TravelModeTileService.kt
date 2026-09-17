package com.phapalesai.voicealert

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Lets the user flip Travel Mode on/off from the notification shade without opening the app. */
class TravelModeTileService : TileService() {

    private val scope: CoroutineScope = MainScope()

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val repository = VoiceAlertApp.instance.preferencesRepository
        scope.launch {
            val current = repository.travelModeFlow.first()
            repository.setTravelMode(!current)
            refreshTile()
        }
    }

    private fun refreshTile() {
        val repository = VoiceAlertApp.instance.preferencesRepository
        scope.launch(Dispatchers.Main) {
            val active = repository.travelModeFlow.first()
            qsTile?.apply {
                state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = "VoiceAlert"
                subtitle = if (active) "Travel Mode ON" else "Travel Mode OFF"
                updateTile()
            }
        }
    }
}
