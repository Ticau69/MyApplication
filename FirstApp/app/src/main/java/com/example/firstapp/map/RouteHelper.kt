package com.example.firstapp.map

import com.huawei.hms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object RouteHelper {

    /**
     * Returnează punctele de pe stradă între origin și destination folosind OSRM (gratuit, fără cheie).
     * Fallback la linie dreaptă dacă API-ul eșuează.
     */
    suspend fun getRoutedPoints(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng> = emptyList()
    ): List<LatLng> = withContext(Dispatchers.IO) {
        try {
            val allPoints = buildList {
                add(origin)
                addAll(waypoints)
                add(destination)
            }

            val coords = allPoints.joinToString(";") { "${it.longitude},${it.latitude}" }

            // radiuses=50 — caută strada în raza de 50m față de fiecare punct
            val radiuses = allPoints.joinToString(";") { "50" }

            val url = "https://routing.openstreetmap.de/routed-car/route/v1/driving/$coords" +
                    "?overview=full&geometries=geojson&steps=true&continue_straight=false&radiuses=$radiuses"

            android.util.Log.d("RouteHelper", "OSRM request: $url")

            val connection = (URL(url).openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; RaceTrackerApp)")
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val responseCode = connection.responseCode
            val response = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                throw Exception("OSRM HTTP $responseCode")
            }
            connection.disconnect()

            val points = parseOsrmPoints(response)

            // Dacă ruta e prea scurtă față de numărul de puncte originale, fallback
            if (points.size < 2) {
                android.util.Log.w("RouteHelper", "Ruta prea scurtă, fallback")
                return@withContext buildList {
                    add(origin)
                    addAll(waypoints)
                    add(destination)
                }
            }

            points

        } catch (e: Exception) {
            android.util.Log.e("RouteHelper", "Eroare OSRM: ${e.message}", e)
            buildList {
                add(origin)
                addAll(waypoints)
                add(destination)
            }
        }
    }

    /**
     * Snap to road folosind OSRM match service.
     */
    suspend fun snapPointsToRoad(
        points: List<LatLng>,
        interpolate: Boolean = true
    ): List<LatLng> = withContext(Dispatchers.IO) {
        if (points.isEmpty()) return@withContext emptyList()
        try {
            val coords = points.joinToString(";") { "${it.longitude},${it.latitude}" }
            val url = "https://router.project-osrm.org/match/v1/driving/$coords?overview=full&geometries=geojson"

            val connection = (URL(url).openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; RaceTrackerApp)")
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val responseCode = connection.responseCode
            val response = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else throw Exception("OSRM match HTTP $responseCode")
            connection.disconnect()

            parseOsrmMatchPoints(response)

        } catch (e: Exception) {
            android.util.Log.e("RouteHelper", "Eroare snap OSRM: ${e.message}", e)
            points
        }
    }

    private fun parseOsrmPoints(json: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        try {
            val root = JSONObject(json)
            if (root.optString("code") != "Ok") {
                android.util.Log.e("RouteHelper", "OSRM code: ${root.optString("code")}")
                return points
            }
            val routes = root.getJSONArray("routes")
            if (routes.length() == 0) return points

            val geometry = routes.getJSONObject(0).getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")

            for (i in 0 until coordinates.length()) {
                val coord = coordinates.getJSONArray(i)
                // GeoJSON e [lng, lat]
                points.add(LatLng(coord.getDouble(1), coord.getDouble(0)))
            }
        } catch (e: Exception) {
            android.util.Log.e("RouteHelper", "Parse OSRM error: ${e.message}")
        }
        return points
    }

    private fun parseOsrmMatchPoints(json: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        try {
            val root = JSONObject(json)
            if (root.optString("code") != "Ok") return points

            val matchings = root.getJSONArray("matchings")
            if (matchings.length() == 0) return points

            val geometry = matchings.getJSONObject(0).getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")

            for (i in 0 until coordinates.length()) {
                val coord = coordinates.getJSONArray(i)
                points.add(LatLng(coord.getDouble(1), coord.getDouble(0)))
            }
        } catch (e: Exception) {
            android.util.Log.e("RouteHelper", "Parse OSRM match error: ${e.message}")
        }
        return points
    }

    /**
     * Găsește cel mai apropiat punct valid de pe o stradă folosind serviciul OSRM Nearest.
     */
    suspend fun getNearestRoadPoint(point: LatLng): LatLng? = withContext(Dispatchers.IO) {
        try {
            // OSRM folosește formatul {longitude},{latitude}
            val url = "https://router.project-osrm.org/nearest/v1/driving/${point.longitude},${point.latitude}"

            val connection = (URL(url).openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; RaceTrackerApp)")
                connectTimeout = 3000 // Timeout scurt, vrem un răspuns rapid la click
                readTimeout = 3000
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().readText()
                val root = JSONObject(response)

                if (root.optString("code") == "Ok") {
                    val waypoints = root.getJSONArray("waypoints")
                    if (waypoints.length() > 0) {
                        val location = waypoints.getJSONObject(0).getJSONArray("location")
                        // Returnăm coordonata de pe stradă (OSRM returnează [lng, lat])
                        return@withContext LatLng(location.getDouble(1), location.getDouble(0))
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            android.util.Log.e("RouteHelper", "Eroare Nearest OSRM: ${e.message}")
        }
        return@withContext null
    }

}

