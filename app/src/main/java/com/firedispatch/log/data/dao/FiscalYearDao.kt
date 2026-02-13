package com.firedispatch.log.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.firedispatch.log.data.entity.FiscalYear
import kotlinx.coroutines.flow.Flow

@Dao
interface FiscalYearDao {
    @Query("SELECT * FROM fiscal_years ORDER BY year DESC")
    fun getAllFiscalYears(): Flow<List<FiscalYear>>

    @Query("SELECT * FROM fiscal_years WHERE isActive = 1 LIMIT 1")
    fun getActiveFiscalYear(): Flow<FiscalYear?>

    @Query("SELECT * FROM fiscal_years WHERE id = :id")
    suspend fun getFiscalYearById(id: Long): FiscalYear?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiscalYear(fiscalYear: FiscalYear): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiscalYears(fiscalYears: List<FiscalYear>)

    @Update
    suspend fun updateFiscalYear(fiscalYear: FiscalYear)

    @Delete
    suspend fun deleteFiscalYear(fiscalYear: FiscalYear)

    @Query("UPDATE fiscal_years SET isActive = 0")
    suspend fun deactivateAllFiscalYears()

    @Query("UPDATE fiscal_years SET isActive = 1 WHERE id = :id")
    suspend fun setActiveFiscalYear(id: Long)

    @Query("DELETE FROM fiscal_years")
    suspend fun deleteAll()
}
