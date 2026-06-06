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
import androidx.compose.ui.platform.LocalContext

@Composable
fun GSensorEffect(onGForceChanged: (gLateral: Float, gLongitudinal: Float) -> Unit) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        // Variabile pentru filtrul de netezire (Low-Pass Filter)
        var smoothedLateral = 0f
        var smoothedLongitudinal = 0f
        val alpha = 0.15f // Un filtru puțin mai puternic pentru a stabiliza punctul pe ecran

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type != Sensor.TYPE_LINEAR_ACCELERATION) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2] // Z iese din ecran, deci reprezintă fața/spatele telefonului

                val displayRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.display?.rotation ?: Surface.ROTATION_0
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.rotation
                }

                // Determinăm forțele în funcție de cum e rotit telefonul
                val rawLateral: Float
                val rawLongitudinal: Float

                // Presupunem că telefonul stă în suport, cu ecranul spre șofer
                when (displayRotation) {
                    Surface.ROTATION_0 -> { // Telefon vertical (Portrait)
                        rawLateral = x
                        rawLongitudinal = z
                    }
                    Surface.ROTATION_90 -> { // Telefon culcat spre stânga (Landscape)
                        rawLateral = -y
                        rawLongitudinal = z
                    }
                    Surface.ROTATION_270 -> { // Telefon culcat spre dreapta (Reverse Landscape)
                        rawLateral = y
                        rawLongitudinal = z
                    }
                    else -> {
                        rawLateral = x
                        rawLongitudinal = z
                    }
                }

                // Aplicăm filtrul de netezire
                smoothedLateral = alpha * rawLateral + (1 - alpha) * smoothedLateral
                smoothedLongitudinal = alpha * rawLongitudinal + (1 - alpha) * smoothedLongitudinal

                // Convertim din m/s^2 în Forță G (1G = 9.81) și inversăm semnele pentru vizualizare intuitivă
                // - Frânare = Acumularea punctului pe grafic "în față/sus"
                // - Viraj dreapta = Punctul se duce în stânga (pentru că mașina se duce în dreapta, tu ești împins în stânga)
                val gLateral = -(smoothedLateral / 9.81f)
                val gLongitudinal = (smoothedLongitudinal / 9.81f)

                onGForceChanged(gLateral, gLongitudinal)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, linearAccelSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }
}