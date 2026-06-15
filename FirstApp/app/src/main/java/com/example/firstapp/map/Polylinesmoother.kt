package com.example.firstapp.map

import com.huawei.hms.maps.model.LatLng

object PolineSmoother {

    // ── Douglas-Peucker ───────────────────────────────────────────
    // Elimină punctele redundante care sunt aproape pe aceeași linie
    // epsilon = toleranța în grade (0.00001 ≈ ~1m, 0.0001 ≈ ~10m)
    fun douglasPeucker(points: List<LatLng>, epsilon: Double = 0.00005): List<LatLng> {
        if (points.size < 3) return points

        var maxDistance = 0.0
        var maxIndex = 0

        val first = points.first()
        val last = points.last()

        for (i in 1 until points.size - 1) {
            val distance = perpendicularDistance(points[i], first, last)
            if (distance > maxDistance) {
                maxDistance = distance
                maxIndex = i
            }
        }

        return if (maxDistance > epsilon) {
            // Recursiv pe cele două jumătăți
            val left  = douglasPeucker(points.subList(0, maxIndex + 1), epsilon)
            val right = douglasPeucker(points.subList(maxIndex, points.size), epsilon)
            // Combinăm fără a duplica punctul de mijloc
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    // ── Chaikin ───────────────────────────────────────────────────
    // Taie colțurile de mai multe ori → curbe moi
    // iterations: 2-3 sunt suficiente, mai mult = pierdere fidelitate
    fun chaikin(points: List<LatLng>, iterations: Int = 3): List<LatLng> {
        if (points.size < 3) return points

        var result = points
        repeat(iterations) {
            result = chaikinPass(result)
        }
        return result
    }

    private fun chaikinPass(points: List<LatLng>): List<LatLng> {
        val smoothed = mutableListOf<LatLng>()

        // Păstrăm primul punct neschimbat
        smoothed.add(points.first())

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]

            // Q = 75% din p0 + 25% din p1
            val q = LatLng(
                0.75 * p0.latitude  + 0.25 * p1.latitude,
                0.75 * p0.longitude + 0.25 * p1.longitude
            )
            // R = 25% din p0 + 75% din p1
            val r = LatLng(
                0.25 * p0.latitude  + 0.75 * p1.latitude,
                0.25 * p0.longitude + 0.75 * p1.longitude
            )

            smoothed.add(q)
            smoothed.add(r)
        }

        // Păstrăm ultimul punct neschimbat
        smoothed.add(points.last())
        return smoothed
    }

    // ── Pipeline complet ──────────────────────────────────────────
    // Apelează asta din CreationState și TrackRacingState
    fun smooth(
        points: List<LatLng>,
        dpEpsilon: Double = 0.00005,    // Toleranță Douglas-Peucker
        chaikinIterations: Int = 3      // Iterații Chaikin
    ): List<LatLng> {
        if (points.size < 3) return points

        // Pasul 1: eliminăm punctele redundante
        val simplified = douglasPeucker(points, dpEpsilon)

        // Pasul 2: netezim curbele
        return chaikin(simplified, chaikinIterations)
    }

    // ── Helper geometric ──────────────────────────────────────────
    // Distanța perpendiculară de la un punct la o linie definită de două puncte
    private fun perpendicularDistance(
        point: LatLng,
        lineStart: LatLng,
        lineEnd: LatLng
    ): Double {
        val dx = lineEnd.longitude - lineStart.longitude
        val dy = lineEnd.latitude  - lineStart.latitude

        // Linie degenerată (start == end)
        if (dx == 0.0 && dy == 0.0) {
            return Math.hypot(
                point.longitude - lineStart.longitude,
                point.latitude  - lineStart.latitude
            )
        }

        val t = ((point.longitude - lineStart.longitude) * dx +
                (point.latitude  - lineStart.latitude)  * dy) /
                (dx * dx + dy * dy)

        val nearestLng = lineStart.longitude + t * dx
        val nearestLat = lineStart.latitude  + t * dy

        return Math.hypot(
            point.longitude - nearestLng,
            point.latitude  - nearestLat
        )
    }
}