package com.firedispatch.log.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.entity.Member
import com.firedispatch.log.data.entity.RoleAssignment
import com.firedispatch.log.data.entity.RoleMemberCount
import com.firedispatch.log.data.model.RoleType
import com.firedispatch.log.data.repository.MemberRepository
import com.firedispatch.log.data.repository.RoleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class RoleSlot(
    val roleType: RoleType,
    val position: Int,
    val assignedMember: Member?
)

class RoleAssignmentViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val memberRepository = MemberRepository(database.memberDao())
    private val roleRepository = RoleRepository(database.roleAssignmentDao(), database.roleMemberCountDao())

    private val _roleSlots = MutableStateFlow<List<RoleSlot>>(emptyList())
    val roleSlots: StateFlow<List<RoleSlot>> = _roleSlots.asStateFlow()

    private val _availableMembers = MutableStateFlow<List<Member>>(emptyList())
    val availableMembers: StateFlow<List<Member>> = _availableMembers.asStateFlow()

    private val _assignedMemberIds = MutableStateFlow<Set<Long>>(emptySet())

    init {
        loadRoleAssignments()
    }

    private fun loadRoleAssignments() {
        viewModelScope.launch {
            combine(
                memberRepository.allMembers,
                roleRepository.allRoleAssignments,
                roleRepository.allRoleMemberCounts
            ) { members, assignments, counts ->
                Triple(members, assignments, counts)
            }.collect { (members, assignments, counts) ->
                _availableMembers.value = members

                val assignmentMap = assignments.associateBy { "${it.roleType}_${it.position}" }
                val memberMap = members.associateBy { it.id }
                _assignedMemberIds.value = assignments.map { it.memberId }.toSet()

                val countMap = counts.associateBy { it.roleType }

                val slots = mutableListOf<RoleSlot>()

                RoleType.entries.forEach { roleType ->
                    if (roleType.isMultiple) {
                        val count = countMap[roleType.name]?.count ?: 1
                        repeat(count) { position ->
                            val key = "${roleType.name}_$position"
                            val assignment = assignmentMap[key]
                            val member = assignment?.let { memberMap[it.memberId] }
                            slots.add(RoleSlot(roleType, position, member))
                        }
                    } else {
                        val key = "${roleType.name}_0"
                        val assignment = assignmentMap[key]
                        val member = assignment?.let { memberMap[it.memberId] }
                        slots.add(RoleSlot(roleType, 0, member))
                    }
                }

                _roleSlots.value = slots
            }
        }
    }

    fun assignMember(roleType: RoleType, position: Int, memberId: Long) {
        viewModelScope.launch {
            val assignment = RoleAssignment(
                roleType = roleType.name,
                memberId = memberId,
                position = position
            )
            roleRepository.insertRoleAssignment(assignment)
        }
    }

    fun clearAssignment(roleType: RoleType, position: Int) {
        viewModelScope.launch {
            roleRepository.deleteRoleAssignment(roleType.name, position)
        }
    }

    fun isMemberAssigned(memberId: Long): Boolean {
        return _assignedMemberIds.value.contains(memberId)
    }

    fun canSave(): Boolean {
        val totalMembers = _availableMembers.value.size
        val assignedCount = _assignedMemberIds.value.size

        // 補助団員を除いた役職スロット数
        val requiredSlots = _roleSlots.value.count { it.roleType != RoleType.HOJODAN }

        return assignedCount == totalMembers && assignedCount >= requiredSlots
    }
}
