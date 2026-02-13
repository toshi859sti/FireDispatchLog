package com.firedispatch.log.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.firedispatch.log.data.entity.AccountCategory
import com.firedispatch.log.data.entity.AccountSubCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountCategoryDao {
    // 科目（AccountCategory）関連
    @Query("SELECT * FROM account_categories ORDER BY sortOrder")
    fun getAllCategories(): Flow<List<AccountCategory>>

    @Query("SELECT * FROM account_categories WHERE isIncome = :isIncome ORDER BY sortOrder")
    fun getCategoriesByType(isIncome: Int): Flow<List<AccountCategory>>

    @Query("SELECT * FROM account_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): AccountCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: AccountCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<AccountCategory>)

    @Update
    suspend fun updateCategory(category: AccountCategory)

    @Delete
    suspend fun deleteCategory(category: AccountCategory)

    @Query("DELETE FROM account_categories")
    suspend fun deleteAllCategories()

    @Query("SELECT COUNT(*) FROM account_categories")
    suspend fun getCategoryCount(): Int

    // 補助科目（AccountSubCategory）関連
    @Query("SELECT * FROM account_sub_categories WHERE parentId = :parentId ORDER BY sortOrder")
    fun getSubCategoriesByParent(parentId: Long): Flow<List<AccountSubCategory>>

    @Query("SELECT * FROM account_sub_categories WHERE id = :id")
    suspend fun getSubCategoryById(id: Long): AccountSubCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubCategory(subCategory: AccountSubCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubCategories(subCategories: List<AccountSubCategory>)

    @Update
    suspend fun updateSubCategory(subCategory: AccountSubCategory)

    @Delete
    suspend fun deleteSubCategory(subCategory: AccountSubCategory)

    @Query("DELETE FROM account_sub_categories WHERE parentId = :parentId")
    suspend fun deleteSubCategoriesByParent(parentId: Long)

    @Query("DELETE FROM account_sub_categories")
    suspend fun deleteAllSubCategories()
}
