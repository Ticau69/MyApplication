package com.example.firstapp.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.firstapp.AppState

@Composable
fun BearingSensorEffect(
    appState: AppState,
    onBearingChanged: (Float) -> Unit
) {
    val context = LocalContext.current

    // Recalculăm delay-ul când se schimbă starea
    val sensorDelay = remember(appState) {
        when (appState) {
            AppState.CRUISE, AppState.RACING, AppState.TRACK_RACING ->
                SensorManager.SENSOR_DELAY_UI      // ~60Hz
            else ->
                SensorManager.SENSOR_DELAY_NORMAL  // ~5Hz în meniuri
        }
    }

    DisposableEffect(sensorDelay) { // ← depinde de delay, se recreează când se schimbă
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return

                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val displayRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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

        sensorManager.registerListener(listener, rotationSensor, sensorDelay)
        onDispose { sensorManager.unregisterListener(listener) }
    }
}