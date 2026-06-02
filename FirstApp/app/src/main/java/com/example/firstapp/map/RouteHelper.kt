package com.example.firstapp.map

import com.huawei.hms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object RouteHelper {

    private const val API_KEY = "DgEDALyLFJ7AkTCA4gUPaACghvk9l1swQsMgsXS63+Pba7SykshRpVQuc8FnJa2wQq2bGVwKOt9aAKn7X5Bmgr95DCHns1LP7fCupw=="

    /**
     * Returnează lista de puncte care urmează strada între origin și destination.
     * Dacă API-ul eșuează, returnează lista originală (linie dreaptă fallback).
     */
    suspend fun getRoutedPoints(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<LatLng> = emptyList()
    ): List<LatLng> = withContext(Dispatchers.IO) {
        try {
            val url = "https://mapapi.cloud.huawei.com/mapApi/v1/routeService/driving?key=$API_KEY"

            val waypointsJson = if (waypoints.isNotEmpty()) {
                val arr = waypoints.joinToString(",") { wp ->
                    """{"lat":${wp.latitude},"lng":${wp.longitude}}"""
                }
                """"waypoints":[$arr],"""
            } else ""

            val body = """
                {
                    "origin":{"lat":${origin.latitude},"lng":${origin.longitude}},
                    "destination":{"lat":${destination.latitude},"lng":${destination.longitude}},
                    $waypointsJson
                    "optimizeWaypoints": false
                }
            """.trimIndent()

            val connection = URL(url).openConnection() as HttpsURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                outputStream.write(body.toByteArray())
            }

            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            parseRoutePoints(response)
        } catch (e: Exception) {
            android.util.Log.e("RouteHelper", "Eroare la fetch rută: ${e.message}")
            // Fallback — linie dreaptă
            buildList {
                add(origin)
                addAll(waypoints)
                add(destination)
            }
        }
    }

    private fun parseRoutePoints(json: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        try {
            val root = JSONObject(json)
            val routes = root.getJSONArray("routes")
            if (routes.length() == 0) return points

            val route = routes.getJSONObject(0)
            val paths = route.getJSONArray("paths")
            if (paths.length() == 0) return points

            val path = paths.getJSONObject(0)
            val steps = path.getJSONArray("steps")

            for (i in 0 until steps.length()) {
                val step = steps.getJSONObject(i)
                val polyline = step.getJSONArray("polyline")

                for (j in 0 until polyline.length()) {
                    val point = polyline.getJSONObject(j)
                    points.add(
                        LatLng(
                            point.getDouble("lat"),
                            point.getDouble("lng")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RouteHelper", "Eroare la parse: ${e.message}")
        }
        return points
    }
}