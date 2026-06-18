package com.example.firstapp.managers

import android.location.Location
import com.example.firstapp.data.SpeedCamera
import com.example.firstapp.data.SpeedRecord
import com.huawei.hms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SpeedTrapManager {

    // Emite un eveniment când iei radarul (pentru a face ecranul să facă "Flash" alb)
    private val _cameraFlashEvent = MutableSharedFlow<SpeedRecord>(extraBufferCapacity = 1)
    val cameraFlashEvent = _cameraFlashEvent.asSharedFlow()

    // Starea internă a senzorului
    private var activeCameraInZone: SpeedCamera? = null
    private var maxSpeedInCurrentZone: Int = 0

    /**
     * Verifică poziția ta la fiecare secundă raportat la toate radarele încărcate pe hartă.
     */
    fun checkLocation(currentPos: LatLng, currentSpeedKmh: Int, allCameras: List<SpeedCamera>) {
        val radiusMeters = 15f // Raza radarului

        // Dacă suntem deja într-o zonă de radar, continuăm să înregistrăm viteza maximă
        if (activeCameraInZone != null) {
            val camera = activeCameraInZone!!
            val dist = distanceBetween(currentPos, LatLng(camera.lat, camera.lng))

            if (dist <= radiusMeters) {
                // Încă suntem în zonă, updatăm viteza dacă e mai mare
                if (currentSpeedKmh > maxSpeedInCurrentZone) {
                    maxSpeedInCurrentZone = currentSpeedKmh
                }
            } else {
                // AM IEȘIT DIN ZONĂ -> FLASH! (Se declanșează radarul)
                if (maxSpeedInCurrentZone > 0) {
                    val record = SpeedRecord(
                        cameraId = camera.id,
                        topSpeedKmh = maxSpeedInCurrentZone
                    )
                    _cameraFlashEvent.tryEmit(record)

                    // TODO: Aici vom apela Supabase / Room DB pentru a salva recordul
                    println("📸 FLASH! Radarul ${camera.name} te-a prins cu $maxSpeedInCurrentZone km/h!")
                }

                // Resetăm starea
                activeCameraInZone = null
                maxSpeedInCurrentZone = 0
            }
            return // Ieșim, nu căutăm alte camere cât timp suntem într-una
        }

        // Dacă NU suntem într-o zonă, verificăm dacă am intrat în vreuna
        for (camera in allCameras) {
            val dist = distanceBetween(currentPos, LatLng(camera.lat, camera.lng))
            if (dist <= radiusMeters) {
                // Am intrat în raza radarului! Armăm sistemul.
                activeCameraInZone = camera
                maxSpeedInCurrentZone = currentSpeedKmh
                break
            }
        }
    }

    private fun distanceBetween(p1: LatLng, p2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
        return results[0]
    }
}