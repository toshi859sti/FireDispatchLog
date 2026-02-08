package com.firedispatch.log.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firedispatch.log.data.database.AppDatabase
import com.firedispatch.log.data.entity.Member
import com.firedispatch.log.data.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

data class MemberEditRow(
    val id: Long = 0,
    var name: String = "",
    var phoneNumber: String = ""
)

class MemberEditViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val memberRepository = MemberRepository(database.memberDao())

    private val _editRows = MutableStateFlow<List<MemberEditRow>>(List(20) { MemberEditRow() })
    val editRows: StateFlow<List<MemberEditRow>> = _editRows.asStateFlow()

    init {
        loadMembers()
    }

    private fun loadMembers() {
        viewModelScope.launch {
            memberRepository.allMembers.collect { members ->
                val collator = Collator.getInstance(Locale.JAPANESE)
                val sortedMembers = members.sortedWith(compareBy(collator) { it.name })

                val rows = mutableListOf<MemberEditRow>()
                sortedMembers.forEach { member ->
                    rows.add(
                        MemberEditRow(
                            id = member.id,
                            name = member.name,
                            phoneNumber = member.phoneNumber
                        )
                    )
                }

                // 残りを空行で埋める（最大20行）
                while (rows.size < 20) {
                    rows.add(MemberEditRow())
                }

                _editRows.value = rows
            }
        }
    }

    fun updateRow(index: Int, name: String, phoneNumber: String) {
        val currentRows = _editRows.value.toMutableList()
        if (index in currentRows.indices) {
            val row = currentRows[index]
            currentRows[index] = row.copy(name = name, phoneNumber = phoneNumber)
            _editRows.value = currentRows
        }
    }

    fun saveMembers(onComplete: () -> Unit) {
        viewModelScope.launch {
            val collator = Collator.getInstance(Locale.JAPANESE)

            // 空でない行のみ抽出
            val validRows = _editRows.value.filter { it.name.isNotBlank() }

            // 電話番号のみの行をチェック
            val invalidRows = validRows.filter { it.name.isBlank() && it.phoneNumber.isNotBlank() }
            if (invalidRows.isNotEmpty()) {
                // エラーハンドリングは画面側で行う
                return@launch
            }

            // 既存のメンバーを削除
            memberRepository.deleteAll()

            // 五十音順にソートして保存
            val sortedRows = validRows.sortedWith(compareBy(collator) { it.name })

            sortedRows.forEachIndexed { index, row ->
                val member = Member(
                    name = row.name,
                    phoneNumber = row.phoneNumber,
                    sortKey = String.format("%05d", index)
                )
                memberRepository.insertMember(member)
            }

            onComplete()
        }
    }

    fun hasInvalidRows(): Boolean {
        val rows = _editRows.value
        return rows.any { it.name.isBlank() && it.phoneNumber.isNotBlank() }
    }
}
