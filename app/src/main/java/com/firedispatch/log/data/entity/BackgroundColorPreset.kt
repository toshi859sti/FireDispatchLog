package com.firedispatch.log.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "background_color_presets")
data class BackgroundColorPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color1: String, // Tailwindカラー名（例："blue-500"）
    val color2: String,
    val color3: String
)
