package com.firedispatch.log.data.dao

import androidx.room.*
import com.firedispatch.log.data.entity.BackgroundColorPreset
import com.firedispatch.log.data.entity.ScreenBackgroundMapping
import kotlinx.coroutines.flow.Flow

@Dao
interface BackgroundColorDao {
    // BackgroundColorPreset
    @Query("SELECT * FROM background_color_presets ORDER BY id ASC")
    fun getAllPresets(): Flow<List<BackgroundColorPreset>>

    @Query("SELECT * FROM background_color_presets WHERE id = :id")
    suspend fun getPresetById(id: Long): BackgroundColorPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: BackgroundColorPreset): Long

    @Update
    suspend fun updatePreset(preset: BackgroundColorPreset)

    @Delete
    suspend fun deletePreset(preset: BackgroundColorPreset)

    @Query("SELECT COUNT(*) FROM background_color_presets")
    suspend fun getPresetCount(): Int

    // ScreenBackgroundMapping
    @Query("SELECT * FROM screen_background_mappings")
    fun getAllMappings(): Flow<List<ScreenBackgroundMapping>>

    @Query("SELECT * FROM screen_background_mappings WHERE screenName = :screenName")
    suspend fun getMappingByScreen(screenName: String): ScreenBackgroundMapping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: ScreenBackgroundMapping)

    @Delete
    suspend fun deleteMapping(mapping: ScreenBackgroundMapping)

    @Query("DELETE FROM screen_background_mappings WHERE presetId = :presetId")
    suspend fun deleteMappingsByPreset(presetId: Long)
}
