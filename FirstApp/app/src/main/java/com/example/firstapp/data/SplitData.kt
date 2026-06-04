package com.example.firstapp.data

data class SplitData(
    val checkpointIndex: Int,
    val checkpointName: String,   // "Start", "CP 1", "CP 2", "Finish"
    val splitTimeMs: Long,         // Timpul absolut de la start până aici
    val deltaVsBestMs: Long?       // Diferența față de best run — null la primul run
)