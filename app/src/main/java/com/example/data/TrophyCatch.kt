package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trophy_catches")
data class TrophyCatch(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userName: String,
    val rodId: Int,
    val rodName: String,
    val strikeType: String, // "NORMAL" veya "BOSA_DUSTU"
    val durationSeconds: Long,
    val durationFormatted: String, // e.g. "02:45"
    val timestampFormatted: String, // e.g. "02.10.2026 04:52"
    val certificateText: String,
    val createdAt: Long = System.currentTimeMillis()
)
