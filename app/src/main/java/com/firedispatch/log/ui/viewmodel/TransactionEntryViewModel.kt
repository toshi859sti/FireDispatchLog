package com.firedispatch.log.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.entity.AccountCategory
import com.firedispatch.log.data.entity.AccountSubCategory
import com.firedispatch.log.data.entity.FiscalYear
import com.firedispatch.log.data.entity.Transaction
import com.firedispatch.log.data.repository.AccountingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TransactionEntryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AccountingRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AccountingRepository(
            db.fiscalYearDao(),
            db.accountCategoryDao(),
            db.transactionDao()
        )
    }

    // アクティブな年度
    val activeFiscalYear: Flow<FiscalYear?> = repository.activeFiscalYear

    // 収入科目
    val incomeCategories: Flow<List<AccountCategory>> = repository.getCategoriesByType(1)

    // 支出科目
    val expenseCategories: Flow<List<AccountCategory>> = repository.getCategoriesByType(0)

    // エラーメッセージ
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * 指定科目の補助科目を取得
     */
    fun getSubCategories(categoryId: Long): Flow<List<AccountSubCategory>> {
        return repository.getSubCategoriesByParent(categoryId)
    }

    /**
     * 指定年度の取引一覧を取得
     */
    fun getTransactionsByFiscalYear(fiscalYearId: Long): Flow<List<Transaction>> {
        return repository.getTransactionsByFiscalYear(fiscalYearId)
    }

    /**
     * 取引を追加
     */
    fun addTransaction(
        fiscalYearId: Long,
        isIncome: Boolean,
        date: Long,
        categoryId: Long,
        subCategoryId: Long?,
        amount: Int,
        memo: String
    ) {
        viewModelScope.launch {
            try {
                if (amount <= 0) {
                    _errorMessage.value = "金額は1円以上を入力してください"
                    return@launch
                }

                val transaction = Transaction(
                    fiscalYearId = fiscalYearId,
                    isIncome = if (isIncome) 1 else 0,
                    date = date,
                    categoryId = categoryId,
                    subCategoryId = subCategoryId,
                    amount = amount,
                    memo = memo
                )
                repository.insertTransaction(transaction)
            } catch (e: Exception) {
                _errorMessage.value = "取引の追加に失敗しました: ${e.message}"
            }
        }
    }

    /**
     * 取引を更新
     */
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                if (transaction.amount <= 0) {
                    _errorMessage.value = "金額は1円以上を入力してください"
                    return@launch
                }
                repository.updateTransaction(transaction)
            } catch (e: Exception) {
                _errorMessage.value = "取引の更新に失敗しました: ${e.message}"
            }
        }
    }

    /**
     * 取引を削除
     */
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction)
            } catch (e: Exception) {
                _errorMessage.value = "取引の削除に失敗しました: ${e.message}"
            }
        }
    }

    /**
     * エラーメッセージをクリア
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * 旅行開始日を設定
     */
    fun setTravelStartDate(fiscalYearId: Long, date: Long?) {
        viewModelScope.launch {
            try {
                val fiscalYear = repository.getFiscalYearById(fiscalYearId)
                fiscalYear?.let {
                    repository.updateFiscalYear(it.copy(travelStartDate = date))
                }
            } catch (e: Exception) {
                _errorMessage.value = "旅行期間の設定に失敗しました: ${e.message}"
            }
        }
    }
}
