package com.example.firstapp.data.local

import androidx.room.TypeConverter
import com.example.firstapp.data.GhostFrame
import com.example.firstapp.data.LapData
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.SerializableLatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    // ── SerializableLatLng ────────────────────────────────────────
    @TypeConverter
    fun fromSerializableLatLng(value: SerializableLatLng?): String? =
        if (value == null) null else "${value.latitude},${value.longitude}"

    @TypeConverter
    fun toSerializableLatLng(value: String?): SerializableLatLng? {
        if (value == null) return null
        val parts = value.split(",")
        if (parts.size < 2) return null
        return SerializableLatLng(parts[0].toDouble(), parts[1].toDouble())
    }

    // ── List<SerializableLatLng> ──────────────────────────────────
    @TypeConverter
    fun fromLatLngList(list: List<SerializableLatLng>?): String? =
        gson.toJson(list)

    @TypeConverter
    fun toLatLngList(value: String?): List<SerializableLatLng> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<SerializableLatLng>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    // ── RaceType ──────────────────────────────────────────────────
    @TypeConverter
    fun fromRaceType(value: RaceType?): String? = value?.name

    @TypeConverter
    fun toRaceType(value: String?): RaceType? =
        if (value == null) null else RaceType.valueOf(value)

    // ── List<LapData> ─────────────────────────────────────────────
    @TypeConverter
    fun fromLapDataList(list: List<LapData>?): String? =
        gson.toJson(list)

    @TypeConverter
    fun toLapDataList(value: String?): List<LapData> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<LapData>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    // ── List<GhostFrame> ─────────────────────────────────────────
    @TypeConverter
    fun fromGhostFrameList(list: List<GhostFrame>?): String? =
        gson.toJson(list)

    @TypeConverter
    fun toGhostFrameList(value: String?): List<GhostFrame> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<GhostFrame>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}