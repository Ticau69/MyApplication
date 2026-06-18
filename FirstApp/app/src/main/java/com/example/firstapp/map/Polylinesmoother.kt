package com.example.firstapp.map

import com.huawei.hms.maps.model.LatLng

object PolineSmoother {

    // ── Douglas-Peucker ───────────────────────────────────────────
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
            val left  = douglasPeucker(points.subList(0, maxIndex + 1), epsilon)
            val right = douglasPeucker(points.subList(maxIndex, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    // ── Catmull-Rom Spline (NOU) ──────────────────────────────────
    // Forțează linia să treacă EXACT prin punctele GPS, prevenind scurtăturile
    private fun catmullRom(points: List<LatLng>, pointsPerSegment: Int = 5): List<LatLng> {
        if (points.size < 3) return points

        val result = mutableListOf<LatLng>()

        // Dublăm capetele pentru a avea mereu 4 puncte de control pentru calcul
        val p = mutableListOf<LatLng>()
        p.add(points.first())
        p.addAll(points)
        p.add(points.last())

        for (i in 1 until p.size - 2) {
            val p0 = p[i - 1]
            val p1 = p[i]
            val p2 = p[i + 1]
            val p3 = p[i + 2]

            // Interpolăm puncte pe segmentul dintre p1 și p2
            for (tSteps in 0 until pointsPerSegment) {
                val t = tSteps.toDouble() / pointsPerSegment
                val t2 = t * t
                val t3 = t2 * t

                val lat = 0.5 * (
                        (2.0 * p1.latitude) +
                                (-p0.latitude + p2.latitude) * t +
                                (2.0 * p0.latitude - 5.0 * p1.latitude + 4.0 * p2.latitude - p3.latitude) * t2 +
                                (-p0.latitude + 3.0 * p1.latitude - 3.0 * p2.latitude + p3.latitude) * t3
                        )

                val lng = 0.5 * (
                        (2.0 * p1.longitude) +
                                (-p0.longitude + p2.longitude) * t +
                                (2.0 * p0.longitude - 5.0 * p1.longitude + 4.0 * p2.longitude - p3.longitude) * t2 +
                                (-p0.longitude + 3.0 * p1.longitude - 3.0 * p2.longitude + p3.longitude) * t3
                        )

                result.add(LatLng(lat, lng))
            }
        }

        // Asigurăm ultimul punct exact
        result.add(points.last())
        return result
    }

    // ── Pipeline complet ──────────────────────────────────────────
    fun smooth(
        points: List<LatLng>,
        dpEpsilon: Double = 0.00003,    // Menținut la o toleranță foarte strictă
        pointsPerSegment: Int = 5       // Numărul de puncte generate per curbă
    ): List<LatLng> {
        if (points.size < 3) return points

        // Pasul 1: Reducem zgomotul
        val simplified = douglasPeucker(points, dpEpsilon)

        // Pasul 2: Generăm curba organică
        return catmullRom(simplified, pointsPerSegment)
    }

    // ── Helper geometric ──────────────────────────────────────────
    private fun perpendicularDistance(
        point: LatLng,
        lineStart: LatLng,
        lineEnd: LatLng
    ): Double {
        val dx = lineEnd.longitude - lineStart.longitude
        val dy = lineEnd.latitude  - lineStart.latitude

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