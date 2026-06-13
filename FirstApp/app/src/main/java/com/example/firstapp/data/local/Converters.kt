package com.example.firstapp.data.local

import androidx.room.TypeConverter
import com.huawei.hms.maps.model.LatLng
import java.util.Locale

class Converters {
    @TypeConverter
    fun fromLatLng(latLng: LatLng?): String? {
        if (latLng == null) return null
        return String.format(Locale.US, "%f,%f", latLng.latitude, latLng.longitude)
    }

    @TypeConverter
    fun toLatLng(value: String?): LatLng? {
        if (value == null) return null
        val pieces = value.split(",")
        if (pieces.size < 2) return null
        return LatLng(pieces[0].toDouble(), pieces[1].toDouble())
    }
}