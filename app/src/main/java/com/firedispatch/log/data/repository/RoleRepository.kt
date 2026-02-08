package com.firedispatch.log.data.repository

import com.firedispatch.log.data.dao.RoleAssignmentDao
import com.firedispatch.log.data.dao.RoleMemberCountDao
import com.firedispatch.log.data.entity.RoleAssignment
import com.firedispatch.log.data.entity.RoleMemberCount
import kotlinx.coroutines.flow.Flow

class RoleRepository(
    private val roleAssignmentDao: RoleAssignmentDao,
    private val roleMemberCountDao: RoleMemberCountDao
) {
    val allRoleAssignments: Flow<List<RoleAssignment>> = roleAssignmentDao.getAllRoleAssignments()
    val allRoleMemberCounts: Flow<List<RoleMemberCount>> = roleMemberCountDao.getAllRoleMemberCounts()

    fun getRoleAssignmentsByRole(roleType: String): Flow<List<RoleAssignment>> {
        return roleAssignmentDao.getRoleAssignmentsByRole(roleType)
    }

    suspend fun getRoleAssignmentByMemberId(memberId: Long): RoleAssignment? {
        return roleAssignmentDao.getRoleAssignmentByMemberId(memberId)
    }

    suspend fun insertRoleAssignment(assignment: RoleAssignment) {
        roleAssignmentDao.insertRoleAssignment(assignment)
    }

    suspend fun deleteRoleAssignmentsByRole(roleType: String) {
        roleAssignmentDao.deleteRoleAssignmentsByRole(roleType)
    }

    suspend fun deleteRoleAssignment(roleType: String, position: Int) {
        roleAssignmentDao.deleteRoleAssignment(roleType, position)
    }

    suspend fun updateRoleAssignments(assignments: List<RoleAssignment>) {
        roleAssignmentDao.updateRoleAssignments(assignments)
    }

    suspend fun getRoleMemberCount(roleType: String): RoleMemberCount? {
        return roleMemberCountDao.getRoleMemberCount(roleType)
    }

    suspend fun insertRoleMemberCount(count: RoleMemberCount) {
        roleMemberCountDao.insertRoleMemberCount(count)
    }

    suspend fun insertRoleMemberCounts(counts: List<RoleMemberCount>) {
        roleMemberCountDao.insertRoleMemberCounts(counts)
    }

    suspend fun deleteAllRoleAssignments() {
        roleAssignmentDao.deleteAll()
    }

    suspend fun deleteAllRoleMemberCounts() {
        roleMemberCountDao.deleteAll()
    }
}
