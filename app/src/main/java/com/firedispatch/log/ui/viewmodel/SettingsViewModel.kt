package com.firedispatch.log.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firedispatch.log.data.backup.BackupManager
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.entity.FiscalYear
import com.firedispatch.log.data.repository.AccountingRepository
import com.firedispatch.log.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val settingsRepository = SettingsRepository(database.appSettingsDao())
    private val backupManager = BackupManager(application)
    private val accountingRepository = AccountingRepository(
        database.fiscalYearDao(),
        database.accountCategoryDao(),
        database.transactionDao()
    )

    private val _fiscalYear = MutableStateFlow(getCurrentFiscalYear())
    val fiscalYear: StateFlow<Int> = _fiscalYear.asStateFlow()

    private val _organizationName = MutableStateFlow("")
    val organizationName: StateFlow<String> = _organizationName.asStateFlow()

    private val _allowPhoneCall = MutableStateFlow(false)
    val allowPhoneCall: StateFlow<Boolean> = _allowPhoneCall.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getSetting(SettingsRepository.KEY_FISCAL_YEAR).collect { setting ->
                if (setting != null) {
                    _fiscalYear.value = setting.value.toIntOrNull() ?: getCurrentFiscalYear()
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.getSetting(SettingsRepository.KEY_ORGANIZATION_NAME).collect { setting ->
                if (setting != null) {
                    _organizationName.value = setting.value
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.getSetting(SettingsRepository.KEY_ALLOW_PHONE_CALL).collect { setting ->
                _allowPhoneCall.value = setting?.value == "true"
            }
        }
    }

    private fun getCurrentFiscalYear(): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        return if (currentMonth >= 4) currentYear else currentYear - 1
    }

    fun setFiscalYear(year: Int) {
        _fiscalYear.value = year
    }

    fun setOrganizationName(name: String) {
        _organizationName.value = name
    }

    fun setAllowPhoneCall(allow: Boolean) {
        _allowPhoneCall.value = allow
    }

    fun saveSettings(onComplete: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.insertSetting(
                SettingsRepository.KEY_FISCAL_YEAR,
                _fiscalYear.value.toString()
            )
            settingsRepository.insertSetting(
                SettingsRepository.KEY_ORGANIZATION_NAME,
                _organizationName.value
            )
            settingsRepository.insertSetting(
                SettingsRepository.KEY_ALLOW_PHONE_CALL,
                _allowPhoneCall.value.toString()
            )
            onComplete()
        }
    }

    fun resetAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            database.clearAllTables()
            onComplete()
        }
    }

    fun resetEventData(onComplete: () -> Unit) {
        viewModelScope.launch {
            // 行事データと出席データを削除
            database.eventDao().deleteAll()
            database.attendanceDao().deleteAll()
            onComplete()
        }
    }

    fun exportBackup(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.exportBackup(uri, getApplication())
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "エクスポートに失敗しました")
            }
        }
    }

    fun importBackup(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.importBackup(uri, getApplication())
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "インポートに失敗しました")
            }
        }
    }

    /**
     * 取引データを初期化
     */
    fun resetTransactionData(onComplete: () -> Unit) {
        viewModelScope.launch {
            database.transactionDao().deleteAll()
            onComplete()
        }
    }

    /**
     * 次年度に移行
     * @param deletePreviousYearData 前年度のデータを削除するかどうか
     */
    fun moveToNextFiscalYear(
        deletePreviousYearData: Boolean = true,
        onSuccess: (newYearId: Long) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // 現在のアクティブな年度を取得
                var activeFiscalYear: FiscalYear? = null
                accountingRepository.activeFiscalYear.collect { year ->
                    activeFiscalYear = year
                    if (activeFiscalYear != null) {
                        // 最初の値を取得したら処理を進める
                        return@collect
                    }
                }

                if (activeFiscalYear == null) {
                    onError("アクティブな年度が設定されていません")
                    return@launch
                }

                val currentYear = activeFiscalYear!!

                // 差引残高を計算
                val balance = accountingRepository.calculateBalance(currentYear)

                // 次年度を作成
                val nextYear = currentYear.year + 1
                val calendar = Calendar.getInstance()

                // 開始日: nextYear年4月1日 00:00:00
                calendar.set(nextYear, Calendar.APRIL, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis

                // 終了日: nextYear+1年3月31日 23:59:59
                calendar.set(nextYear + 1, Calendar.MARCH, 31, 23, 59, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endDate = calendar.timeInMillis

                val newFiscalYear = FiscalYear(
                    year = nextYear,
                    startDate = startDate,
                    endDate = endDate,
                    carryOver = balance,
                    isActive = 0
                )

                // 新年度を挿入
                val newYearId = accountingRepository.insertFiscalYear(newFiscalYear)

                // 新年度をアクティブに設定
                accountingRepository.setActiveFiscalYear(newYearId)

                // 前年度のデータを削除（オプション）
                if (deletePreviousYearData) {
                    // 出動表データを削除
                    database.attendanceDao().deleteAll()
                    database.eventDao().deleteAll()

                    // 取引データを削除
                    accountingRepository.deleteTransactionsByFiscalYear(currentYear.id)
                }

                onSuccess(newYearId)
            } catch (e: Exception) {
                onError("次年度への移行に失敗しました: ${e.message}")
            }
        }
    }
}
