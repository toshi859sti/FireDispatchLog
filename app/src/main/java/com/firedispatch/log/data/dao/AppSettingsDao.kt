package com.firedispatch.log.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.firedispatch.log.data.entity.AppSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE key = :key")
    fun getSetting(key: String): Flow<AppSettings?>

    @Query("SELECT value FROM app_settings WHERE key = :key")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSettings)

    @Query("DELETE FROM app_settings")
    suspend fun deleteAll()
}
