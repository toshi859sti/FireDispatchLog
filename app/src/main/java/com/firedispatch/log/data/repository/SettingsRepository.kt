package com.firedispatch.log.data.repository

import com.firedispatch.log.data.dao.AppSettingsDao
import com.firedispatch.log.data.entity.AppSettings
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val appSettingsDao: AppSettingsDao) {
    fun getSetting(key: String): Flow<AppSettings?> {
        return appSettingsDao.getSetting(key)
    }

    suspend fun getSettingValue(key: String): String? {
        return appSettingsDao.getSettingValue(key)
    }

    suspend fun insertSetting(key: String, value: String) {
        appSettingsDao.insertSetting(AppSettings(key, value))
    }

    suspend fun deleteAll() {
        appSettingsDao.deleteAll()
    }

    companion object {
        const val KEY_FISCAL_YEAR = "fiscal_year"
        const val KEY_ORGANIZATION_NAME = "organization_name"
    }
}
