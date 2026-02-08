package com.firedispatch.log.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.entity.RoleMemberCount
import com.firedispatch.log.data.model.RoleType
import com.firedispatch.log.data.repository.RoleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoleCountItem(
    val roleType: RoleType,
    var count: Int
)

class RoleMemberCountViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val roleRepository = RoleRepository(database.roleAssignmentDao(), database.roleMemberCountDao())

    private val _roleCounts = MutableStateFlow<List<RoleCountItem>>(emptyList())
    val roleCounts: StateFlow<List<RoleCountItem>> = _roleCounts.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _isValid = MutableStateFlow(false)
    val isValid: StateFlow<Boolean> = _isValid.asStateFlow()

    init {
        loadRoleCounts()
    }

    private fun loadRoleCounts() {
        viewModelScope.launch {
            roleRepository.allRoleMemberCounts.collect { counts ->
                val countMap = counts.associateBy { it.roleType }

                val regularRoles = RoleType.getRegularMemberRoles()
                val items = regularRoles.map { roleType ->
                    RoleCountItem(
                        roleType = roleType,
                        count = countMap[roleType.name]?.count ?: getDefaultCount(roleType)
                    )
                }

                _roleCounts.value = items
                updateTotalAndValidation()
            }
        }
    }

    private fun getDefaultCount(roleType: RoleType): Int {
        return when (roleType) {
            RoleType.HOJODAN -> 0
            else -> 1
        }
    }

    fun updateCount(roleType: RoleType, newCount: Int) {
        val minCount = if (roleType == RoleType.HOJODAN) 0 else 1
        val clampedCount = newCount.coerceAtLeast(minCount)

        val currentCounts = _roleCounts.value.toMutableList()
        val index = currentCounts.indexOfFirst { it.roleType == roleType }
        if (index != -1) {
            currentCounts[index] = currentCounts[index].copy(count = clampedCount)
            _roleCounts.value = currentCounts
            updateTotalAndValidation()
        }
    }

    private fun updateTotalAndValidation() {
        val total = _roleCounts.value.sumOf { it.count }
        _totalCount.value = total
        _isValid.value = (total == 11)
    }

    fun saveCounts(onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            if (_isValid.value) {
                // 既存の団員数設定をクリア
                roleRepository.deleteAllRoleMemberCounts()

                // 新しい設定を保存
                val counts = _roleCounts.value.map { item ->
                    RoleMemberCount(
                        roleType = item.roleType.name,
                        count = item.count
                    )
                }
                roleRepository.insertRoleMemberCounts(counts)

                // 役職割り当てをクリア（団員行が変わったため）
                val regularRoles = RoleType.getRegularMemberRoles()
                regularRoles.forEach { roleType ->
                    roleRepository.deleteRoleAssignmentsByRole(roleType.name)
                }

                onSuccess()
            } else {
                onError()
            }
        }
    }
}
