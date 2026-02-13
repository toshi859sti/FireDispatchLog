package com.firedispatch.log.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.entity.AccountCategory
import com.firedispatch.log.data.entity.AccountSubCategory
import com.firedispatch.log.data.repository.AccountingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountCategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AccountingRepository(
        database.fiscalYearDao(),
        database.accountCategoryDao(),
        database.transactionDao()
    )

    // 収入科目（isIncome=1）
    val incomeCategories = repository.getCategoriesByType(1)

    // 支出科目（isIncome=0）
    val expenseCategories = repository.getCategoriesByType(0)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun getSubCategories(parentId: Long) = repository.getSubCategoriesByParent(parentId)

    fun addCategory(name: String, isIncome: Int) {
        viewModelScope.launch {
            try {
                val category = AccountCategory(
                    name = name,
                    isIncome = isIncome,
                    isEditable = 1,
                    sortOrder = 999 // 末尾に追加
                )
                repository.insertCategory(category)
            } catch (e: Exception) {
                _errorMessage.value = "科目の追加に失敗しました: ${e.message}"
            }
        }
    }

    fun updateCategory(category: AccountCategory, newName: String) {
        viewModelScope.launch {
            try {
                repository.updateCategory(category.copy(name = newName))
            } catch (e: Exception) {
                _errorMessage.value = "科目の更新に失敗しました: ${e.message}"
            }
        }
    }

    fun deleteCategory(category: AccountCategory) {
        viewModelScope.launch {
            try {
                // 補助科目も削除
                repository.deleteSubCategoriesByParent(category.id)
                repository.deleteCategory(category)
            } catch (e: Exception) {
                _errorMessage.value = "科目の削除に失敗しました: ${e.message}"
            }
        }
    }

    fun addSubCategory(parentId: Long, name: String) {
        viewModelScope.launch {
            try {
                val subCategory = AccountSubCategory(
                    parentId = parentId,
                    name = name,
                    sortOrder = 999 // 末尾に追加
                )
                repository.insertSubCategory(subCategory)
            } catch (e: Exception) {
                _errorMessage.value = "補助科目の追加に失敗しました: ${e.message}"
            }
        }
    }

    fun updateSubCategory(subCategory: AccountSubCategory, newName: String) {
        viewModelScope.launch {
            try {
                repository.updateSubCategory(subCategory.copy(name = newName))
            } catch (e: Exception) {
                _errorMessage.value = "補助科目の更新に失敗しました: ${e.message}"
            }
        }
    }

    fun deleteSubCategory(subCategory: AccountSubCategory) {
        viewModelScope.launch {
            try {
                repository.deleteSubCategory(subCategory)
            } catch (e: Exception) {
                _errorMessage.value = "補助科目の削除に失敗しました: ${e.message}"
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
