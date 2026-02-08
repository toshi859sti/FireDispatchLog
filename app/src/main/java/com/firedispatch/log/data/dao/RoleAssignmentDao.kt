package com.firedispatch.log.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.firedispatch.log.data.entity.RoleAssignment
import kotlinx.coroutines.flow.Flow

@Dao
interface RoleAssignmentDao {
    @Query("SELECT * FROM role_assignments ORDER BY roleType, position")
    fun getAllRoleAssignments(): Flow<List<RoleAssignment>>

    @Query("SELECT * FROM role_assignments WHERE roleType = :roleType ORDER BY position")
    fun getRoleAssignmentsByRole(roleType: String): Flow<List<RoleAssignment>>

    @Query("SELECT * FROM role_assignments WHERE memberId = :memberId")
    suspend fun getRoleAssignmentByMemberId(memberId: Long): RoleAssignment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoleAssignment(assignment: RoleAssignment)

    @Query("DELETE FROM role_assignments WHERE roleType = :roleType")
    suspend fun deleteRoleAssignmentsByRole(roleType: String)

    @Query("DELETE FROM role_assignments WHERE roleType = :roleType AND position = :position")
    suspend fun deleteRoleAssignment(roleType: String, position: Int)

    @Query("DELETE FROM role_assignments")
    suspend fun deleteAll()

    @Transaction
    suspend fun updateRoleAssignments(assignments: List<RoleAssignment>) {
        deleteAll()
        assignments.forEach { insertRoleAssignment(it) }
    }
}
