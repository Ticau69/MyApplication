package com.example.firstapp.managers

import com.example.firstapp.data.Track
import com.huawei.hms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProximityManager(private val scope: CoroutineScope) {

    private val _nearbyTrack = MutableStateFlow<Track?>(null)
    val nearbyTrack = _nearbyTrack.asStateFlow()

    private val _distanceToNearbyTrack = MutableStateFlow<Int?>(null)
    val distanceToNearbyTrack = _distanceToNearbyTrack.asStateFlow()

    private var nearbyDismissJob: Job? = null
    private var lastDismissedTrackId: String? = null

    /**
     * Algoritmul central care caută cel mai apropiat traseu.
     * Rulează calculele matematice și gestionează logicile de "Auto-Dismiss".
     */
    fun checkProximity(currentPos: LatLng, tracks: List<Track>, detectionRadius: Int) {
        var closestTrack: Track? = null
        var minDistance = Float.MAX_VALUE
        val results = FloatArray(1)

        // 1. Căutăm cel mai apropiat punct de start
        for (track in tracks) {
            val startPos = track.start.toLatLng()
            android.location.Location.distanceBetween(
                currentPos.latitude, currentPos.longitude,
                startPos.latitude, startPos.longitude,
                results
            )
            val distance = results[0]

            if (distance < minDistance) {
                minDistance = distance
                closestTrack = track
            }
        }

        val radiusFloat = detectionRadius.toFloat()

        // 2. Logica de declanșare
        if (closestTrack != null && minDistance <= radiusFloat) {

            // Dacă l-am ascuns recent și încă ne învârtim în zona lui, ignorăm afișarea, dar îi updatăm distanța
            if (lastDismissedTrackId == closestTrack.id && minDistance < radiusFloat * 2) {
                _distanceToNearbyTrack.value = minDistance.toInt()
                return
            }

            // Dacă ne-am îndepărtat suficient de mult de un traseu ascuns, resetăm memoria ca să îl putem vedea din nou mai târziu
            if (lastDismissedTrackId == closestTrack.id && minDistance >= radiusFloat * 2) {
                lastDismissedTrackId = null
            }

            // Am găsit un traseu nou valid!
            if (_nearbyTrack.value?.id != closestTrack.id) {
                _nearbyTrack.value = closestTrack

                // Pornim timer-ul de 10 secunde pentru ascunderea automată a cardului de pe ecran
                nearbyDismissJob?.cancel()
                nearbyDismissJob = scope.launch {
                    delay(10000)
                    lastDismissedTrackId = _nearbyTrack.value?.id
                    forceClear()
                }
            }
            _distanceToNearbyTrack.value = minDistance.toInt()

        } else {
            // Nu e niciun traseu în rază
            if (_nearbyTrack.value != null) forceClear()

            // Reset general dacă ne-am depărtat foarte mult (> 2.5x raza)
            if (minDistance > radiusFloat * 2.5f) {
                lastDismissedTrackId = null
            }
        }
    }

    /**
     * Oprește forțat radarul și curăță interfața (util când ieșim din modul Cruise).
     */
    fun forceClear() {
        nearbyDismissJob?.cancel()
        _nearbyTrack.value = null
        _distanceToNearbyTrack.value = null
    }
}