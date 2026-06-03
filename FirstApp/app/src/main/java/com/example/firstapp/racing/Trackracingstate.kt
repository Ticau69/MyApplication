package com.example.firstapp.racing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.firstapp.AppState
import com.example.firstapp.data.Track
import com.example.trackappv2.R
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import com.huawei.hms.maps.model.Polyline
import com.huawei.hms.maps.model.PolylineOptions
import com.huawei.hms.maps.model.JointType
import com.huawei.hms.maps.model.RoundCap

class TrackRacingState(
    private val view: View,
    private val onStateChange: (AppState) -> Unit,
    private val track: Track,
    private val huaweiMap: HuaweiMap
) {
    private val session = QuickRaceSession()
    private var isInitialized = false
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    // Polilinii pe hartă
    private var trackPolyline: Polyline? = null      // traseul salvat (albastru)
    private var gpsTrailPolyline: Polyline? = null   // urma GPS în timp real (verde)
    private val gpsTrailPoints = mutableListOf<LatLng>()

    // Markeri traseu
    private val drawnMarkers = mutableListOf<Marker>()

    // Checkpoint tracking
    private val checkpoints: List<LatLng>
    private var nextCheckpointIndex = 0
    private val CHECKPOINT_RADIUS_M = 30f

    init {
        // Construim lista de checkpoints în ordine: start → cp1..cpN → finish
        checkpoints = buildList {
            add(track.start.toLatLng())
            addAll(track.checkpoints.map { it.toLatLng() })
            add(track.finish.toLatLng())
        }

        setupUI()
        drawSavedTrack()
    }

    private fun setupUI() {
        view.findViewById<TextView>(R.id.tvTrackName)?.text = track.name

        view.findViewById<Button>(R.id.btnStopRacing)?.setOnClickListener {
            stopTimerTicker()
            saveRaceRecord()
            cleanup()
            onStateChange(AppState.CRUISE)
        }
    }

    private fun drawSavedTrack() {
        val points = if (track.routedPoints.isNotEmpty()) {
            track.routedPoints.map { it.toLatLng() }
        } else {
            buildList {
                add(track.start.toLatLng())
                addAll(track.checkpoints.map { it.toLatLng() })
                add(track.finish.toLatLng())
            }
        }

        // Poliline traseu salvat — albastru semi-transparent
        trackPolyline = huaweiMap.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(Color.parseColor("#991976D2"))
                .width(10f)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )

        // Poliline urmă GPS — se completează în timp real, portocaliu
        gpsTrailPolyline = huaweiMap.addPolyline(
            PolylineOptions()
                .color(Color.parseColor("#FFFF6B35"))
                .width(6f)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )

        // Marker Start
        drawnMarkers.add(
            huaweiMap.addMarker(
                MarkerOptions()
                    .position(track.start.toLatLng())
                    .icon(bitmapFromVector(R.drawable.ic_start_marker))
                    .anchorMarker(0.5f, 1f)
                    .title(track.name)
            )
        )

        // Marker Finish
        drawnMarkers.add(
            huaweiMap.addMarker(
                MarkerOptions()
                    .position(track.finish.toLatLng())
                    .icon(bitmapFromVector(R.drawable.ic_finish_marker))
                    .anchorMarker(0.5f, 1f)
            )
        )

        // Markeri Checkpoint
        track.checkpoints.forEach { cp ->
            drawnMarkers.add(
                huaweiMap.addMarker(
                    MarkerOptions()
                        .position(cp.toLatLng())
                        .icon(bitmapFromVector(R.drawable.ic_checkpoint_marker))
                        .anchorMarker(0.5f, 1f)
                )
            )
        }
    }

    fun update(speed: Int, latLng: LatLng?) {
        if (!isInitialized) {
            session.start()
            isInitialized = true
            startTimerTicker()
            // Sărim primul checkpoint (startul) dacă suntem deja pe el
            nextCheckpointIndex = 1
        }

        latLng?.let { pos ->
            session.update(speed, pos)

            // Adăugăm punctul curent la urma GPS
            gpsTrailPoints.add(pos)
            gpsTrailPolyline?.points = gpsTrailPoints.toList()

            // Verificăm dacă am ajuns la următorul checkpoint
            checkCheckpointProximity(pos)

            // Actualizăm indicatorul checkpoint
            updateCheckpointIndicator(pos)
        }

        view.findViewById<TextView>(R.id.tvSpeed)?.text = "$speed km/h"
        view.findViewById<TextView>(R.id.tvMaxSpeed)?.text = "Max: ${session.maxSpeed} km/h"
        view.findViewById<TextView>(R.id.tvDistance)?.text =
            String.format("Dist: %.2f km", session.getDistanceKm())

        updateTimerUI()
    }

    private fun checkCheckpointProximity(pos: LatLng) {
        if (nextCheckpointIndex >= checkpoints.size) return

        val nextCp = checkpoints[nextCheckpointIndex]
        val dist = distanceBetween(pos, nextCp)

        if (dist <= CHECKPOINT_RADIUS_M) {
            nextCheckpointIndex++

            // Dacă am trecut de finish
            if (nextCheckpointIndex >= checkpoints.size) {
                onFinishReached()
            }
        }
    }

    private fun updateCheckpointIndicator(pos: LatLng) {
        val layoutCp = view.findViewById<LinearLayout>(R.id.layoutCheckpoint) ?: return
        val tvNext = view.findViewById<TextView>(R.id.tvNextCheckpoint) ?: return
        val tvDist = view.findViewById<TextView>(R.id.tvCheckpointDist) ?: return

        if (nextCheckpointIndex >= checkpoints.size) {
            layoutCp.visibility = View.GONE
            return
        }

        layoutCp.visibility = View.VISIBLE
        val nextCp = checkpoints[nextCheckpointIndex]
        val dist = distanceBetween(pos, nextCp)

        val isFinish = nextCheckpointIndex == checkpoints.size - 1
        val cpNumber = nextCheckpointIndex // 1-based față de start

        tvNext.text = when {
            isFinish -> "FINISH"
            nextCheckpointIndex == 1 && track.checkpoints.isEmpty() -> "FINISH"
            else -> "Checkpoint $cpNumber"
        }

        tvDist.text = if (dist >= 1000) {
            String.format("%.1f km", dist / 1000f)
        } else {
            "${dist.toInt()} m"
        }
    }

    private fun onFinishReached() {
        stopTimerTicker()
        saveRaceRecord()

        android.app.AlertDialog.Builder(view.context)
            .setTitle("🏁 Finish!")
            .setMessage(
                "Timp: ${formatTime(session.getDurationSeconds())}\n" +
                        "Viteză max: ${session.maxSpeed} km/h\n" +
                        "Distanță: ${String.format("%.2f", session.getDistanceKm())} km"
            )
            .setPositiveButton("OK") { _, _ ->
                cleanup()
                onStateChange(AppState.CRUISE)
            }
            .setCancelable(false)
            .show()
    }

    private fun startTimerTicker() {
        stopTimerTicker()
        timerRunnable = object : Runnable {
            override fun run() {
                updateTimerUI()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable!!)
    }

    fun stopTimerTicker() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
    }

    private fun updateTimerUI() {
        val tvTimer = view.findViewById<TextView>(R.id.tvTimer) ?: return
        tvTimer.text = formatTime(session.getDurationSeconds())
    }

    private fun formatTime(totalSecs: Long): String {
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }

    private fun saveRaceRecord() {
        val context = view.context
        val manager = HistoryManager(context)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val record = RaceRecord(
            id = java.util.UUID.randomUUID().toString(),
            date = sdf.format(java.util.Date()),
            maxSpeed = session.maxSpeed,
            distanceKm = session.getDistanceKm(),
            durationSeconds = session.getDurationSeconds()
        )
        manager.saveRace(record)
    }

    fun cleanup() {
        trackPolyline?.remove()
        gpsTrailPolyline?.remove()
        drawnMarkers.forEach { it.remove() }
        drawnMarkers.clear()
        gpsTrailPoints.clear()
        trackPolyline = null
        gpsTrailPolyline = null
    }

    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            a.latitude, a.longitude,
            b.latitude, b.longitude,
            results
        )
        return results[0]
    }

    private fun bitmapFromVector(drawableRes: Int): com.huawei.hms.maps.model.BitmapDescriptor {
        val ctx = view.context
        val drawable = ContextCompat.getDrawable(ctx, drawableRes)!!
        val bitmap = createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}