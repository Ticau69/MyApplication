package com.example.firstapp.history

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.firstapp.AppState
import com.example.firstapp.data.Track
import com.example.firstapp.data.TrackRepository
import com.example.trackappv2.R
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.LatLngBounds
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import com.huawei.hms.maps.model.Polyline
import com.huawei.hms.maps.model.PolylineOptions

class SavedTracksState(
    private val view: View,
    private val onStateChange: (AppState) -> Unit,
    private var huaweiMap: HuaweiMap? = null
) {
    var isMapSet = false
        private set

    private val drawnPolylines = mutableListOf<Polyline>()
    private val drawnMarkers = mutableListOf<Marker>()
    private var selectedTrack: Track? = null

    fun setMap(map: HuaweiMap) {
        huaweiMap = map
        isMapSet = true
        // Dacă există un traseu selectat deja, îl desenăm
        selectedTrack?.let { drawTrack(it) }
    }

    fun setup() {
        view.findViewById<Button>(R.id.btnCloseSavedTracks)?.setOnClickListener {
            clearDrawnObjects()
            onStateChange(AppState.CRUISE)
        }

        // Apelăm funcția care populează ecranul
        loadTracksIntoUI()
    }

    private fun loadTracksIntoUI() {
        val context = view.context
        val repo = TrackRepository(context)
        val tracks = repo.getTracks()
        val container = view.findViewById<LinearLayout>(R.id.containerSavedTracks)

        container?.removeAllViews()
        val inflater = LayoutInflater.from(context)

        if (tracks.isEmpty()) {
            val emptyText = TextView(context).apply {
                text = context.getString(R.string.no_saved_tracks)
                setTextColor(Color.GRAY)
                setPadding(16, 16, 16, 16)
            }
            container?.addView(emptyText)
        } else {
            tracks.forEach { track ->
                val itemView = inflater.inflate(R.layout.item_saved_track, container, false)

                itemView.findViewById<TextView>(R.id.tvTrackName).text = track.name
                itemView.findViewById<TextView>(R.id.tvTrackDate).text = track.createdAt
                itemView.findViewById<TextView>(R.id.tvTrackCheckpoints).text =
                    context.getString(R.string.checkpoint_count, track.checkpoints.size)

                // Listener pentru butonul de ștergere
                itemView.findViewById<ImageButton>(R.id.btnDeleteTrack).setOnClickListener {
                    showDeleteConfirmation(track, repo)
                }

                // Click pe traseu → îl desenăm pe hartă și centrăm camera
                itemView.setOnClickListener {
                    selectedTrack = track
                    drawTrack(track)
                }

                container?.addView(itemView)
            }
        }
    }

    private fun showDeleteConfirmation(track: Track, repo: TrackRepository) {
        AlertDialog.Builder(view.context)
            .setTitle("Ștergere traseu")
            .setMessage("Ești sigur că vrei să ștergi cursa '${track.name}'?")
            .setPositiveButton("Șterge") { _, _ ->
                val success = repo.deleteTrack(track.id)
                if (success) {
                    // Dacă traseul șters era tocmai cel pe care ne uitam pe hartă, îl debifăm
                    if (selectedTrack?.id == track.id) {
                        clearDrawnObjects()
                        selectedTrack = null
                    }
                    // Reîmprospătăm interfața ca să dispară elementul șters
                    loadTracksIntoUI()
                }
            }
            .setNegativeButton("Anulează", null)
            .show()
    }

    private fun drawTrack(track: Track) {
        val map = huaweiMap ?: return
        val ctx = view.context

        // Curățăm ce era desenat anterior
        clearDrawnObjects()

        val points = if (track.routedPoints.isNotEmpty()) {
            track.routedPoints.map { it.toLatLng() }
        } else {
            buildList {
                add(track.start.toLatLng())
                addAll(track.checkpoints.map { it.toLatLng() })
                add(track.finish.toLatLng())
            }
        }

        // Polyline
        drawnPolylines.add(
            map.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .color(Color.parseColor("#1976D2"))
                    .width(8f)
            )
        )

        // Marker Start
        drawnMarkers.add(
            map.addMarker(
                MarkerOptions()
                    .position(track.start.toLatLng())
                    .icon(bitmapFromVector(ctx, R.drawable.ic_start_marker))
                    .anchorMarker(0.5f, 1f)
                    .title(track.name)
            )
        )

        // Marker Finish
        drawnMarkers.add(
            map.addMarker(
                MarkerOptions()
                    .position(track.finish.toLatLng())
                    .icon(bitmapFromVector(ctx, R.drawable.ic_finish_marker))
                    .anchorMarker(0.5f, 1f)
            )
        )

        // Markeri Checkpoint
        track.checkpoints.forEach { cp ->
            drawnMarkers.add(
                map.addMarker(
                    MarkerOptions()
                        .position(cp.toLatLng())
                        .icon(bitmapFromVector(ctx, R.drawable.ic_checkpoint_marker))
                        .anchorMarker(0.5f, 1f)
                )
            )
        }

        // Centrăm camera să cuprindă tot traseul
        val boundsBuilder = LatLngBounds.builder()
        points.forEach { boundsBuilder.include(it) }
        val bounds = boundsBuilder.build()
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(bounds, 100) // 100dp padding
        )
    }

    private fun clearDrawnObjects() {
        drawnPolylines.forEach { it.remove() }
        drawnMarkers.forEach { it.remove() }
        drawnPolylines.clear()
        drawnMarkers.clear()
    }

    private fun bitmapFromVector(context: android.content.Context, drawableRes: Int):
            com.huawei.hms.maps.model.BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, drawableRes)!!
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