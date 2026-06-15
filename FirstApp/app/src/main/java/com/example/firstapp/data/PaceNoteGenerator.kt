package com.example.firstapp.data

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object PaceNoteGenerator {

    // Fereastra de puncte pentru calculul unghiului
    // Mai mare = mai smooth, mai mic = mai sensibil
    private const val WINDOW_SIZE = 3
    private const val MIN_SEGMENT_POINTS = 2

    /**
     * Generează Pace Notes din lista de puncte rutate (OSRM sau înregistrate live).
     * Funcționează identic pentru ambele moduri — hibrid prin design.
     */
    fun generate(
        trackId: String,
        points: List<SerializableLatLng>
    ): List<PaceNote> {
        if (points.size < WINDOW_SIZE * 2 + 1) return emptyList()

        val paceNotes = mutableListOf<PaceNote>()
        val smoothedBearings = calculateSmoothedBearings(points)

        var segmentStart = 0
        var segmentIndex = 0
        var currentType = PaceNoteType.fromAngle(0f)

        for (i in 1 until smoothedBearings.size) {
            val delta = angleDelta(smoothedBearings[i - 1], smoothedBearings[i])
            val type = PaceNoteType.fromAngle(abs(delta))
            val direction = when {
                abs(delta) < 10f -> TurnDirection.STRAIGHT
                delta > 0        -> TurnDirection.RIGHT
                else             -> TurnDirection.LEFT
            }

            // Dacă tipul s-a schimbat, închidem segmentul curent
            if (type != currentType && i - segmentStart >= MIN_SEGMENT_POINTS) {
                val segmentPoints = points.subList(segmentStart, i + 1)
                val avgAngle = calculateAverageAngle(smoothedBearings, segmentStart, i)
                val avgDirection = dominantDirection(smoothedBearings, segmentStart, i)

                paceNotes.add(
                    PaceNote(
                        trackId        = trackId,
                        segmentIndex   = segmentIndex,
                        startPoint     = points[segmentStart],
                        endPoint       = points[i],
                        points         = segmentPoints,
                        type           = currentType,
                        direction      = avgDirection,
                        angleDeviation = avgAngle
                    )
                )
                segmentStart = i
                segmentIndex++
                currentType = type
            }
        }

        // Ultimul segment
        if (segmentStart < points.size - 1) {
            val segmentPoints = points.subList(segmentStart, points.size)
            paceNotes.add(
                PaceNote(
                    trackId        = trackId,
                    segmentIndex   = segmentIndex,
                    startPoint     = points[segmentStart],
                    endPoint       = points.last(),
                    points         = segmentPoints,
                    type           = currentType,
                    direction      = TurnDirection.STRAIGHT,
                    angleDeviation = 0f
                )
            )
        }

        return paceNotes
    }

    // ── Calcule geometrice ────────────────────────────────────────

    /**
     * Calculează bearing-ul între două puncte GPS în grade (0-360).
     */
    private fun bearing(from: SerializableLatLng, to: SerializableLatLng): Float {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)

        val x = sin(dLng) * cos(lat2)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)

        return ((Math.toDegrees(atan2(x, y)).toFloat() + 360f) % 360f)
    }

    /**
     * Delta unghiular între două bearing-uri (-180 la +180).
     * Pozitiv = dreapta, Negativ = stânga.
     */
    private fun angleDelta(b1: Float, b2: Float): Float {
        var delta = b2 - b1
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }

    /**
     * Smoothing pe fereastră glisantă — elimină zgomotul GPS.
     */
    private fun calculateSmoothedBearings(points: List<SerializableLatLng>): List<Float> {
        val rawBearings = (0 until points.size - 1).map { i ->
            bearing(points[i], points[i + 1])
        }

        return rawBearings.mapIndexed { i, _ ->
            val start = maxOf(0, i - WINDOW_SIZE)
            val end   = minOf(rawBearings.size - 1, i + WINDOW_SIZE)
            averageBearing(rawBearings.subList(start, end + 1))
        }
    }

    /**
     * Media bearing-urilor (corectă pentru unghiuri circulare).
     */
    private fun averageBearing(bearings: List<Float>): Float {
        val sinSum = bearings.sumOf { sin(Math.toRadians(it.toDouble())) }
        val cosSum = bearings.sumOf { cos(Math.toRadians(it.toDouble())) }
        return ((Math.toDegrees(atan2(sinSum, cosSum)).toFloat() + 360f) % 360f)
    }

    private fun calculateAverageAngle(
        bearings: List<Float>,
        start: Int,
        end: Int
    ): Float {
        if (start >= end) return 0f
        val deltas = (start until end).map { i ->
            abs(angleDelta(bearings[i], bearings[i + 1]))
        }
        return deltas.average().toFloat()
    }

    private fun dominantDirection(
        bearings: List<Float>,
        start: Int,
        end: Int
    ): TurnDirection {
        if (start >= end) return TurnDirection.STRAIGHT
        val deltas = (start until end).map { i ->
            angleDelta(bearings[i], bearings[i + 1])
        }
        val avg = deltas.average()
        return when {
            abs(avg) < 10.0 -> TurnDirection.STRAIGHT
            avg > 0         -> TurnDirection.RIGHT
            else            -> TurnDirection.LEFT
        }
    }
}