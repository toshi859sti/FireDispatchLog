package com.firedispatch.log.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.firedispatch.log.data.entity.RoleMemberCount
import kotlinx.coroutines.flow.Flow

@Dao
interface RoleMemberCountDao {
    @Query("SELECT * FROM role_member_counts")
    fun getAllRoleMemberCounts(): Flow<List<RoleMemberCount>>

    @Query("SELECT * FROM role_member_counts WHERE roleType = :roleType")
    suspend fun getRoleMemberCount(roleType: String): RoleMemberCount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoleMemberCount(count: RoleMemberCount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoleMemberCounts(counts: List<RoleMemberCount>)

    @Query("DELETE FROM role_member_counts")
    suspend fun deleteAll()
}
