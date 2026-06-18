package com.example.firstapp.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.firstapp.AppState

@Composable
fun BearingSensorEffect(
    appState: AppState,
    currentSpeed: Int, // Parametru nou adăugat
    onBearingChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    var lastUpdateTime by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) { // Am scos sensorDelay-ul din key pentru a evita re-crearea excesivă
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                // 1. MAGIC FIX: Când ne mișcăm (> 5 km/h), GPS-ul preia direcția!
                // Ignorăm complet busola hardware ca să evităm bug-urile de landscape din mașină.
                if (currentSpeed >= 5) return

                // 2. Limităm actualizările la maxim 10 ori pe secundă (100ms)
                // Acest filtru oprește înghețarea completă a hărții
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime < 100) return
                lastUpdateTime = currentTime

                if (event?.sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return

                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val displayRotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    context.display?.rotation ?: Surface.ROTATION_0
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.rotation
                }

                val remappedMatrix = FloatArray(9)
                when (displayRotation) {
                    Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, remappedMatrix)
                    Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, remappedMatrix)
                    Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                        rotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, remappedMatrix)
                    else -> SensorManager.remapCoordinateSystem(
                        rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, remappedMatrix)
                }

                val orientation = FloatArray(3)
                SensorManager.getOrientation(remappedMatrix, orientation)
                val bearing = ((Math.toDegrees(orientation[0].toDouble()).toFloat()) + 360) % 360

                onBearingChanged(bearing)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Folosim SENSOR_DELAY_NORMAL (mai blând cu resursele)
        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sensorManager.unregisterListener(listener) }
    }
}