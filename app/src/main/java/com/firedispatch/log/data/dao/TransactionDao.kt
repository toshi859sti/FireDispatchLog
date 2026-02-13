package com.firedispatch.log.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.firedispatch.log.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE fiscalYearId = :fiscalYearId ORDER BY date DESC, createdAt DESC")
    fun getTransactionsByFiscalYear(fiscalYearId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE fiscalYearId = :fiscalYearId AND isIncome = :isIncome ORDER BY date DESC, createdAt DESC")
    fun getTransactionsByFiscalYearAndType(fiscalYearId: Long, isIncome: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE fiscalYearId = :fiscalYearId AND date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    fun getTransactionsByFiscalYearAndDateRange(fiscalYearId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date < :endDate ORDER BY date DESC, createdAt DESC")
    fun getTransactionsForToday(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE fiscalYearId = :fiscalYearId")
    suspend fun deleteTransactionsByFiscalYear(fiscalYearId: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    // 集計用クエリ
    @Query("SELECT SUM(amount) FROM transactions WHERE fiscalYearId = :fiscalYearId AND isIncome = :isIncome")
    suspend fun getTotalAmountByType(fiscalYearId: Long, isIncome: Int): Int?

    @Query("SELECT SUM(amount) FROM transactions WHERE fiscalYearId = :fiscalYearId AND isIncome = :isIncome AND categoryId = :categoryId")
    suspend fun getTotalAmountByCategory(fiscalYearId: Long, isIncome: Int, categoryId: Long): Int?
}
