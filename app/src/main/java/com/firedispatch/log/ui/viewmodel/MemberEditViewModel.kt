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

    private val _focusedRowIndex = MutableStateFlow<Int?>(null)
    val focusedRowIndex: StateFlow<Int?> = _focusedRowIndex.asStateFlow()

    init {
        loadMembers()
    }

    private fun loadMembers() {
        viewModelScope.launch {
            memberRepository.allMembers.collect { members ->
                // sortKeyで並び替え（保存時の順序を保持）
                val sortedMembers = members.sortedBy { it.sortKey }

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

    // 五十音順にソート
    fun sortByName() {
        val currentRows = _editRows.value.toMutableList()
        val collator = Collator.getInstance(Locale.JAPANESE)
        val nonEmptyRows = currentRows.filter { it.name.isNotBlank() }
            .sortedWith(compareBy(collator) { it.name })
        val emptyRows = currentRows.filter { it.name.isBlank() }
        _editRows.value = nonEmptyRows + emptyRows
    }

    // フォーカスを設定
    fun setFocusedRow(index: Int?) {
        _focusedRowIndex.value = index
    }

    // フォーカスされている行を上に移動
    fun moveFocusedRowUp() {
        val index = _focusedRowIndex.value ?: return
        if (index > 0) {
            val currentRows = _editRows.value.toMutableList()
            val temp = currentRows[index]
            currentRows[index] = currentRows[index - 1]
            currentRows[index - 1] = temp
            _editRows.value = currentRows
            _focusedRowIndex.value = index - 1
        }
    }

    // フォーカスされている行を下に移動
    fun moveFocusedRowDown() {
        val index = _focusedRowIndex.value ?: return
        val currentRows = _editRows.value
        if (index < currentRows.size - 1) {
            val mutableRows = currentRows.toMutableList()
            val temp = mutableRows[index]
            mutableRows[index] = mutableRows[index + 1]
            mutableRows[index + 1] = temp
            _editRows.value = mutableRows
            _focusedRowIndex.value = index + 1
        }
    }

    fun saveMembers(onComplete: () -> Unit) {
        viewModelScope.launch {
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

            // 現在の順序で保存（ソートしない）
            validRows.forEachIndexed { index, row ->
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
