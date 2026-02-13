package com.firedispatch.log.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screen_background_mappings")
data class ScreenBackgroundMapping(
    @PrimaryKey val screenName: String, // "menu", "accounting", "transaction_entry" など
    val presetId: Long // BackgroundColorPresetのID
)
