package com.firedispatch.log.data.repository

import com.firedispatch.log.data.dao.MemberDao
import com.firedispatch.log.data.entity.Member
import kotlinx.coroutines.flow.Flow

class MemberRepository(private val memberDao: MemberDao) {
    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()

    suspend fun getMemberById(id: Long): Member? {
        return memberDao.getMemberById(id)
    }

    suspend fun insertMember(member: Member): Long {
        return memberDao.insertMember(member)
    }

    suspend fun insertMembers(members: List<Member>) {
        memberDao.insertMembers(members)
    }

    suspend fun updateMember(member: Member) {
        memberDao.updateMember(member)
    }

    suspend fun deleteMember(member: Member) {
        memberDao.deleteMember(member)
    }

    suspend fun deleteAll() {
        memberDao.deleteAll()
    }

    suspend fun getMemberCount(): Int {
        return memberDao.getMemberCount()
    }
}
