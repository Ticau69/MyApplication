package com.example.firstapp.managers

import android.content.Context
import com.example.firstapp.data.LapData
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.SplitData
import com.example.firstapp.racing.VoiceCopilot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TelemetryManager(
    context: Context,
    private val scope: CoroutineScope,
    private val settingsManager: SettingsManager // Cerem setările ca să știm de TTS
) {
    // Instanțiem copilotul direct aici, unde are loc acțiunea
    private val voiceCopilot = VoiceCopilot(context)

    // Modele de date mutate din AppViewModel pentru izolare
    data class RaceFinishData(
        val durationSeconds: Long,
        val maxSpeed: Int,
        val distanceKm: Double,
        val splits: List<SplitData> = emptyList(),
        val raceType: RaceType = RaceType.SPRINT
    )

    data class LapNotification(
        val lap: LapData,
        val isBestLap: Boolean
    )

    // --- STĂRI PENTRU INTERFAȚĂ (HUD) ---
    private val _currentLapTimeMs = MutableStateFlow(0L)
    val currentLapTimeMs = _currentLapTimeMs.asStateFlow()

    private val _sprintProgress = MutableStateFlow(0f)
    val sprintProgress = _sprintProgress.asStateFlow()

    private val _currentSplit = MutableStateFlow<SplitData?>(null)
    val currentSplit = _currentSplit.asStateFlow()

    private val _allLaps = MutableStateFlow<List<LapData>>(emptyList())
    val allLaps = _allLaps.asStateFlow()

    private val _lastLapNotification = MutableStateFlow<LapNotification?>(null)
    val lastLapNotification = _lastLapNotification.asStateFlow()

    private val _raceFinishData = MutableStateFlow<RaceFinishData?>(null)
    val raceFinishData = _raceFinishData.asStateFlow()

    // --- ACTUALIZĂRI RAPIDE (Fiecare 100ms) ---
    fun updateLapTime(ms: Long) {
        _currentLapTimeMs.value = ms
    }

    fun updateSprintProgress(progress: Float) {
        _sprintProgress.value = progress
    }

    // --- EVENIMENTE DE CURSĂ ---
    fun onSplitRecorded(split: SplitData) {
        _currentSplit.value = split

        // Citește setarea în timp real din SettingsManager
        if (settingsManager.isTtsEnabled.value) {
            voiceCopilot.speakCheckpoint(split.checkpointName, split.deltaVsBestMs)
        }

        // Ascunde UI-ul după 3 secunde
        scope.launch {
            delay(3000)
            _currentSplit.value = null
        }
    }

    fun onLapCompleted(lap: LapData, laps: List<LapData>) {
        val isBest = laps.minByOrNull { it.lapTimeMs }?.lapNumber == lap.lapNumber
        _lastLapNotification.value = LapNotification(lap, isBest)
        _allLaps.value = laps.toList()

        if (settingsManager.isTtsEnabled.value) {
            voiceCopilot.speakLap(lap.lapNumber, lap.lapTimeMs)
        }

        // Ascunde popup-ul după 4 secunde
        scope.launch {
            delay(4000)
            _lastLapNotification.value = null
        }
    }

    fun onRaceFinished(data: RaceFinishData) {
        _raceFinishData.value = data
    }

    fun dismissRaceFinish() {
        _raceFinishData.value = null
    }

    fun resetTelemetry() {
        _currentLapTimeMs.value = 0L
        _sprintProgress.value = 0f
        _currentSplit.value = null
        _allLaps.value = emptyList()
        _lastLapNotification.value = null
    }

    fun destroy() {
        voiceCopilot.destroy() // Eliberăm motorul audio la final
    }
}