package com.example.firstapp.managers

import android.content.Context
import com.example.firstapp.data.GhostFrame
import com.example.firstapp.data.GhostRepository
import com.example.firstapp.data.GhostRun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GhostManager(
    private val ghostRepository: GhostRepository,  // ← înlocuiește Context
    private val scope: CoroutineScope
) {
    // Starea pentru diferența de timp față de fantomă
    private val _ghostDeltaMs = MutableStateFlow<Long?>(null)
    val ghostDeltaMs = _ghostDeltaMs.asStateFlow()

    // Instanța fantomei pentru traseul curent
    private val _currentGhostRun = MutableStateFlow<GhostRun?>(null)
    val currentGhostRun = _currentGhostRun.asStateFlow()

    /**
     * Încarcă fantoma din baza de date pe un thread secundar.
     */
    fun loadGhostForTrack(trackId: String) {
        scope.launch {
            val ghost = withContext(Dispatchers.IO) {
                ghostRepository.getBestRun(trackId)
            }
            _currentGhostRun.value = ghost
            _ghostDeltaMs.value = null
        }
    }


    /**
     * Actualizează UI-ul cu avansul/întârzierea ta față de fantomă.
     */
    fun updateGhostDelta(deltaMs: Long) {
        _ghostDeltaMs.value = deltaMs
    }

    /**
     * Salvează o tură nouă ca fantomă, dacă este cel mai bun timp.
     */
    fun saveGhostRun(trackId: String, frames: List<GhostFrame>, totalTimeMs: Long) {
        scope.launch {
            withContext(Dispatchers.IO) {
                ghostRepository.saveBestRun(
                    GhostRun(
                        trackId     = trackId,
                        lapNumber   = 1,
                        totalTimeMs = totalTimeMs,
                        frames      = frames
                    )
                )
            }
        }
    }

    /**
     * Curățare memorie.
     */
    fun clearGhost() {
        _currentGhostRun.value = null
        _ghostDeltaMs.value = null
    }
}