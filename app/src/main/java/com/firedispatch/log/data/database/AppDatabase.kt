package com.firedispatch.log.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.firedispatch.log.data.dao.AppSettingsDao
import com.firedispatch.log.data.dao.AttendanceDao
import com.firedispatch.log.data.dao.EventDao
import com.firedispatch.log.data.dao.MemberDao
import com.firedispatch.log.data.dao.RoleAssignmentDao
import com.firedispatch.log.data.dao.RoleMemberCountDao
import com.firedispatch.log.data.entity.AppSettings
import com.firedispatch.log.data.entity.Attendance
import com.firedispatch.log.data.entity.Event
import com.firedispatch.log.data.entity.Member
import com.firedispatch.log.data.entity.RoleAssignment
import com.firedispatch.log.data.entity.RoleMemberCount

@Database(
    entities = [
        Member::class,
        RoleAssignment::class,
        RoleMemberCount::class,
        Event::class,
        Attendance::class,
        AppSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun roleAssignmentDao(): RoleAssignmentDao
    abstract fun roleMemberCountDao(): RoleMemberCountDao
    abstract fun eventDao(): EventDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fire_dispatch_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
