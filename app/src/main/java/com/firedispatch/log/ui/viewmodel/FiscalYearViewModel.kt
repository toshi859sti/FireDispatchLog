package com.firedispatch.log.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.entity.FiscalYear
import com.firedispatch.log.data.repository.AccountingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

class FiscalYearViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AccountingRepository(
        database.fiscalYearDao(),
        database.accountCategoryDao(),
        database.transactionDao()
    )

    /**
     * 既存年度が存在するかチェック
     */
    suspend fun hasExistingFiscalYears(): Boolean {
        return repository.allFiscalYears.firstOrNull()?.isNotEmpty() ?: false
    }

    val allFiscalYears = repository.allFiscalYears
    val activeFiscalYear = repository.activeFiscalYear

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun showAddDialog() {
        _showAddDialog.value = true
    }

    fun hideAddDialog() {
        _showAddDialog.value = false
    }

    fun addFiscalYear(year: Int, startDate: Long, endDate: Long, carryOver: Int, clearExistingData: Boolean = false) {
        viewModelScope.launch {
            try {
                // 既存データをクリアする場合
                if (clearExistingData) {
                    // 出動表データを削除
                    database.attendanceDao().deleteAll()
                    database.eventDao().deleteAll()

                    // 全取引データを削除
                    database.transactionDao().deleteAll()
                }

                val fiscalYear = FiscalYear(
                    year = year,
                    startDate = startDate,
                    endDate = endDate,
                    carryOver = carryOver,
                    isActive = 0
                )
                val newYearId = repository.insertFiscalYear(fiscalYear)

                // 新しい年度を自動的にアクティブに設定
                repository.setActiveFiscalYear(newYearId)

                hideAddDialog()
            } catch (e: Exception) {
                _errorMessage.value = "年度の追加に失敗しました: ${e.message}"
            }
        }
    }

    fun updateCarryOver(fiscalYear: FiscalYear, newCarryOver: Int) {
        viewModelScope.launch {
            try {
                repository.updateFiscalYear(fiscalYear.copy(carryOver = newCarryOver))
            } catch (e: Exception) {
                _errorMessage.value = "繰越金の更新に失敗しました: ${e.message}"
            }
        }
    }

    fun setActiveFiscalYear(fiscalYearId: Long) {
        viewModelScope.launch {
            try {
                repository.setActiveFiscalYear(fiscalYearId)
            } catch (e: Exception) {
                _errorMessage.value = "年度の切替に失敗しました: ${e.message}"
            }
        }
    }

    fun deleteFiscalYear(fiscalYear: FiscalYear) {
        viewModelScope.launch {
            try {
                repository.deleteFiscalYear(fiscalYear)
            } catch (e: Exception) {
                _errorMessage.value = "年度の削除に失敗しました: ${e.message}"
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * デフォルトの年度情報を生成（4/1〜3/31）
     */
    fun createDefaultFiscalYearDates(year: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // 開始日: year年4月1日 00:00:00
        calendar.set(year, Calendar.APRIL, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        // 終了日: year+1年3月31日 23:59:59
        calendar.set(year + 1, Calendar.MARCH, 31, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }
}
