package com.firedispatch.log.data.repository

import com.firedispatch.log.data.dao.BackgroundColorDao
import com.firedispatch.log.data.entity.BackgroundColorPreset
import com.firedispatch.log.data.entity.ScreenBackgroundMapping
import kotlinx.coroutines.flow.Flow

class BackgroundColorRepository(private val dao: BackgroundColorDao) {

    // プリセット管理
    fun getAllPresets(): Flow<List<BackgroundColorPreset>> = dao.getAllPresets()

    suspend fun getPresetById(id: Long): BackgroundColorPreset? = dao.getPresetById(id)

    suspend fun insertPreset(preset: BackgroundColorPreset): Long = dao.insertPreset(preset)

    suspend fun updatePreset(preset: BackgroundColorPreset) = dao.updatePreset(preset)

    suspend fun deletePreset(preset: BackgroundColorPreset) {
        // プリセット削除時に、このプリセットを使用している画面マッピングも削除
        dao.deleteMappingsByPreset(preset.id)
        dao.deletePreset(preset)
    }

    suspend fun getPresetCount(): Int = dao.getPresetCount()

    // 画面マッピング管理
    fun getAllMappings(): Flow<List<ScreenBackgroundMapping>> = dao.getAllMappings()

    suspend fun getMappingByScreen(screenName: String): ScreenBackgroundMapping? =
        dao.getMappingByScreen(screenName)

    suspend fun setScreenMapping(screenName: String, presetId: Long) {
        dao.insertMapping(ScreenBackgroundMapping(screenName, presetId))
    }

    suspend fun removeScreenMapping(screenName: String) {
        val mapping = dao.getMappingByScreen(screenName)
        mapping?.let { dao.deleteMapping(it) }
    }

    /**
     * 画面名に対応するプリセットを取得
     */
    suspend fun getPresetForScreen(screenName: String): BackgroundColorPreset? {
        val mapping = dao.getMappingByScreen(screenName)
        return mapping?.let { dao.getPresetById(it.presetId) }
    }
}
