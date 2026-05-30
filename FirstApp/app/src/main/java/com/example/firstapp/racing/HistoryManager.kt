package com.example.firstapp.racing

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("race_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveRace(record: RaceRecord) {
        val history = getHistory().toMutableList()
        history.add(0, record)
        
        // Keep only last 20
        val limitedHistory = if (history.size > 20) history.take(20) else history
        
        prefs.edit().putString("history_list", gson.toJson(limitedHistory)).apply()
    }

    fun getHistory(): List<RaceRecord> {
        val json = prefs.getString("history_list", null) ?: return emptyList()
        val type = object : TypeToken<List<RaceRecord>>() {}.type
        return gson.fromJson(json, type)
    }
}
