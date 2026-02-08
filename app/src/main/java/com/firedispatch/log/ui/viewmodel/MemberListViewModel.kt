package com.firedispatch.log.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.entity.Member
import com.firedispatch.log.data.entity.RoleAssignment
import com.firedispatch.log.data.model.RoleType
import com.firedispatch.log.data.repository.MemberRepository
import com.firedispatch.log.data.repository.RoleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class MemberWithRole(
    val member: Member,
    val roleType: RoleType?
)

class MemberListViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val memberRepository = MemberRepository(database.memberDao())
    private val roleRepository = RoleRepository(database.roleAssignmentDao(), database.roleMemberCountDao())

    private val _membersWithRoles = MutableStateFlow<List<MemberWithRole>>(emptyList())
    val membersWithRoles: StateFlow<List<MemberWithRole>> = _membersWithRoles.asStateFlow()

    init {
        loadMembersWithRoles()
    }

    private fun loadMembersWithRoles() {
        viewModelScope.launch {
            combine(
                memberRepository.allMembers,
                roleRepository.allRoleAssignments
            ) { members, assignments ->
                val assignmentMap = assignments.associateBy { it.memberId }

                // 役職順にソート
                members.map { member ->
                    val assignment = assignmentMap[member.id]
                    val roleType = assignment?.let { RoleType.valueOf(it.roleType) }
                    MemberWithRole(member, roleType)
                }.sortedBy { memberWithRole ->
                    memberWithRole.roleType?.order ?: Int.MAX_VALUE
                }
            }.collect { sortedList ->
                _membersWithRoles.value = sortedList
            }
        }
    }
}
