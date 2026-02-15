package com.firedispatch.log.data.repository

import com.firedispatch.log.data.dao.AccountCategoryDao
import com.firedispatch.log.data.dao.FiscalYearDao
import com.firedispatch.log.data.dao.TransactionDao
import com.firedispatch.log.data.entity.AccountCategory
import com.firedispatch.log.data.entity.AccountSubCategory
import com.firedispatch.log.data.entity.FiscalYear
import com.firedispatch.log.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

class AccountingRepository(
    private val fiscalYearDao: FiscalYearDao,
    private val accountCategoryDao: AccountCategoryDao,
    private val transactionDao: TransactionDao
) {
    // 年度関連
    val allFiscalYears: Flow<List<FiscalYear>> = fiscalYearDao.getAllFiscalYears()
    val activeFiscalYear: Flow<FiscalYear?> = fiscalYearDao.getActiveFiscalYear()

    suspend fun getFiscalYearById(id: Long): FiscalYear? = fiscalYearDao.getFiscalYearById(id)
    suspend fun insertFiscalYear(fiscalYear: FiscalYear): Long = fiscalYearDao.insertFiscalYear(fiscalYear)
    suspend fun insertFiscalYears(fiscalYears: List<FiscalYear>) = fiscalYearDao.insertFiscalYears(fiscalYears)
    suspend fun updateFiscalYear(fiscalYear: FiscalYear) = fiscalYearDao.updateFiscalYear(fiscalYear)
    suspend fun deleteFiscalYear(fiscalYear: FiscalYear) = fiscalYearDao.deleteFiscalYear(fiscalYear)

    suspend fun setActiveFiscalYear(id: Long) {
        fiscalYearDao.deactivateAllFiscalYears()
        fiscalYearDao.setActiveFiscalYear(id)
    }

    // 科目関連
    val allCategories: Flow<List<AccountCategory>> = accountCategoryDao.getAllCategories()

    fun getCategoriesByType(isIncome: Int): Flow<List<AccountCategory>> =
        accountCategoryDao.getCategoriesByType(isIncome)

    suspend fun getCategoryById(id: Long): AccountCategory? = accountCategoryDao.getCategoryById(id)
    suspend fun insertCategory(category: AccountCategory): Long = accountCategoryDao.insertCategory(category)
    suspend fun insertCategories(categories: List<AccountCategory>) = accountCategoryDao.insertCategories(categories)
    suspend fun updateCategory(category: AccountCategory) = accountCategoryDao.updateCategory(category)
    suspend fun deleteCategory(category: AccountCategory) = accountCategoryDao.deleteCategory(category)
    suspend fun getCategoryCount(): Int = accountCategoryDao.getCategoryCount()

    // 補助科目関連
    fun getSubCategoriesByParent(parentId: Long): Flow<List<AccountSubCategory>> =
        accountCategoryDao.getSubCategoriesByParent(parentId)

    suspend fun getSubCategoryById(id: Long): AccountSubCategory? = accountCategoryDao.getSubCategoryById(id)
    suspend fun insertSubCategory(subCategory: AccountSubCategory): Long = accountCategoryDao.insertSubCategory(subCategory)
    suspend fun insertSubCategories(subCategories: List<AccountSubCategory>) = accountCategoryDao.insertSubCategories(subCategories)
    suspend fun updateSubCategory(subCategory: AccountSubCategory) = accountCategoryDao.updateSubCategory(subCategory)
    suspend fun deleteSubCategory(subCategory: AccountSubCategory) = accountCategoryDao.deleteSubCategory(subCategory)
    suspend fun deleteSubCategoriesByParent(parentId: Long) = accountCategoryDao.deleteSubCategoriesByParent(parentId)

    // 取引関連
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByFiscalYear(fiscalYearId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByFiscalYear(fiscalYearId)

    fun getTransactionsByFiscalYearAndType(fiscalYearId: Long, isIncome: Int): Flow<List<Transaction>> =
        transactionDao.getTransactionsByFiscalYearAndType(fiscalYearId, isIncome)

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    fun getTransactionsByFiscalYearAndDateRange(fiscalYearId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByFiscalYearAndDateRange(fiscalYearId, startDate, endDate)

    fun getTransactionsForToday(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsForToday(startDate, endDate)

    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)
    suspend fun insertTransaction(transaction: Transaction): Long = transactionDao.insertTransaction(transaction)
    suspend fun insertTransactions(transactions: List<Transaction>) = transactionDao.insertTransactions(transactions)
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)
    suspend fun deleteTransactionsByFiscalYear(fiscalYearId: Long) = transactionDao.deleteTransactionsByFiscalYear(fiscalYearId)

    // 集計関連
    suspend fun getTotalAmountByType(fiscalYearId: Long, isIncome: Int): Int =
        transactionDao.getTotalAmountByType(fiscalYearId, isIncome) ?: 0

    suspend fun getTotalAmountByCategory(fiscalYearId: Long, isIncome: Int, categoryId: Long): Int =
        transactionDao.getTotalAmountByCategory(fiscalYearId, isIncome, categoryId) ?: 0

    /**
     * 差引残高を計算（繰越金 + 収入 - 支出）
     */
    suspend fun calculateBalance(fiscalYear: FiscalYear): Int {
        val incomeTotal = getTotalAmountByType(fiscalYear.id, 1)
        val expenseTotal = getTotalAmountByType(fiscalYear.id, 0)
        return fiscalYear.carryOver + incomeTotal - expenseTotal
    }
}
