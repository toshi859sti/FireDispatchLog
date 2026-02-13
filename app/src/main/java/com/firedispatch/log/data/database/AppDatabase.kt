package com.firedispatch.log.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.firedispatch.log.data.dao.AppSettingsDao
import com.firedispatch.log.data.dao.AttendanceDao
import com.firedispatch.log.data.dao.EventDao
import com.firedispatch.log.data.dao.MemberDao
import com.firedispatch.log.data.dao.RoleAssignmentDao
import com.firedispatch.log.data.dao.RoleMemberCountDao
import com.firedispatch.log.data.entity.AccountCategory
import com.firedispatch.log.data.entity.AccountSubCategory
import com.firedispatch.log.data.entity.AppSettings
import com.firedispatch.log.data.entity.Attendance
import com.firedispatch.log.data.entity.Event
import com.firedispatch.log.data.entity.FiscalYear
import com.firedispatch.log.data.entity.Member
import com.firedispatch.log.data.entity.RoleAssignment
import com.firedispatch.log.data.entity.RoleMemberCount
import com.firedispatch.log.data.entity.Transaction

@Database(
    entities = [
        Member::class,
        RoleAssignment::class,
        RoleMemberCount::class,
        Event::class,
        Attendance::class,
        AppSettings::class,
        // 会計機能エンティティ
        FiscalYear::class,
        AccountCategory::class,
        AccountSubCategory::class,
        Transaction::class
    ],
    version = 2,  // 1 → 2 に変更
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun roleAssignmentDao(): RoleAssignmentDao
    abstract fun roleMemberCountDao(): RoleMemberCountDao
    abstract fun eventDao(): EventDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun appSettingsDao(): AppSettingsDao
    // 会計機能DAO
    abstract fun fiscalYearDao(): com.firedispatch.log.data.dao.FiscalYearDao
    abstract fun accountCategoryDao(): com.firedispatch.log.data.dao.AccountCategoryDao
    abstract fun transactionDao(): com.firedispatch.log.data.dao.TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * マイグレーション 1→2: 会計機能テーブルの追加
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // fiscal_years テーブル
                database.execSQL("""
                    CREATE TABLE fiscal_years (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        year INTEGER NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        carryOver INTEGER NOT NULL DEFAULT 0,
                        isActive INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // account_categories テーブル
                database.execSQL("""
                    CREATE TABLE account_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        name TEXT NOT NULL,
                        isIncome INTEGER NOT NULL,
                        isEditable INTEGER NOT NULL DEFAULT 1,
                        outputOrder INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // account_sub_categories テーブル
                database.execSQL("""
                    CREATE TABLE account_sub_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        parentId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        name TEXT NOT NULL,
                        isEditable INTEGER NOT NULL DEFAULT 1,
                        outputOrder INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // transactions テーブル
                database.execSQL("""
                    CREATE TABLE transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fiscalYearId INTEGER NOT NULL,
                        isIncome INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        categoryId INTEGER NOT NULL,
                        subCategoryId INTEGER,
                        amount INTEGER NOT NULL,
                        memo TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fire_dispatch_database"
                )
                    .addMigrations(MIGRATION_1_2)  // マイグレーション追加
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // 初期データ投入（バックグラウンドで実行）
                            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                                populateDefaultCategories(context)
                            }
                        }
                    })
                    // .fallbackToDestructiveMigration() を削除（既存データ保護）
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * デフォルト科目データの投入
         */
        private suspend fun populateDefaultCategories(context: Context) {
            val database = getDatabase(context)
            val categoryDao = database.accountCategoryDao()

            // 科目テーブルが空の場合のみ実行
            if (categoryDao.getCategoryCount() > 0) {
                return
            }

            // 収入科目（固定）
            val incomeCategories = listOf(
                AccountCategory(name = "南島原市会計課", isIncome = 1, isEditable = 0, sortOrder = 1, outputOrder = 1),
                AccountCategory(name = "南島原市防災課", isIncome = 1, isEditable = 0, sortOrder = 2, outputOrder = 2),
                AccountCategory(name = "預金利息", isIncome = 1, isEditable = 0, sortOrder = 3, outputOrder = 3),
                AccountCategory(name = "寸志", isIncome = 1, isEditable = 0, sortOrder = 4, outputOrder = 4)
            )

            // 支出科目
            val expenseCategories = listOf(
                AccountCategory(name = "消防団共済掛金", isIncome = 0, isEditable = 0, sortOrder = 1, outputOrder = 1),
                AccountCategory(name = "消防団負担金", isIncome = 0, isEditable = 0, sortOrder = 2, outputOrder = 2),
                AccountCategory(name = "年末警戒", isIncome = 0, isEditable = 0, sortOrder = 3, outputOrder = 3),
                AccountCategory(name = "旅行関連", isIncome = 0, isEditable = 0, sortOrder = 4, outputOrder = 4),
                AccountCategory(name = "飲食関連", isIncome = 0, isEditable = 0, sortOrder = 5, outputOrder = 5),
                AccountCategory(name = "環境整備費", isIncome = 0, isEditable = 0, sortOrder = 6, outputOrder = 6),
                AccountCategory(name = "出動手当金", isIncome = 0, isEditable = 0, sortOrder = 7, outputOrder = 7),
                AccountCategory(name = "免許補助金", isIncome = 0, isEditable = 1, sortOrder = 8, outputOrder = 8)
            )

            // 収入科目を投入
            incomeCategories.forEach { categoryDao.insertCategory(it) }

            // 支出科目を投入し、IDを保存
            val expenseCategoryIds = mutableListOf<Long>()
            expenseCategories.forEach { category ->
                val id = categoryDao.insertCategory(category)
                expenseCategoryIds.add(id)
            }

            // 旅行関連の補助科目（expenseCategories[3]）
            val travelId = expenseCategoryIds[3]
            val travelSubCategories = listOf(
                AccountSubCategory(parentId = travelId, name = "宿泊料", sortOrder = 1, outputOrder = 1),
                AccountSubCategory(parentId = travelId, name = "フェリー代", sortOrder = 2, outputOrder = 2),
                AccountSubCategory(parentId = travelId, name = "車代", sortOrder = 3, outputOrder = 3),
                AccountSubCategory(parentId = travelId, name = "食事代", sortOrder = 4, outputOrder = 4),
                AccountSubCategory(parentId = travelId, name = "宴会料金", sortOrder = 5, outputOrder = 5),
                AccountSubCategory(parentId = travelId, name = "コンパニオン料金", sortOrder = 6, outputOrder = 6),
                AccountSubCategory(parentId = travelId, name = "土産代", sortOrder = 7, outputOrder = 7),
                AccountSubCategory(parentId = travelId, name = "駐車料金", sortOrder = 8, outputOrder = 8),
                AccountSubCategory(parentId = travelId, name = "二次会料金", sortOrder = 9, outputOrder = 9)
            )

            // 飲食関連の補助科目（expenseCategories[4]）
            val diningId = expenseCategoryIds[4]
            val diningSubCategories = listOf(
                AccountSubCategory(parentId = diningId, name = "歓送迎会", sortOrder = 1, outputOrder = 1),
                AccountSubCategory(parentId = diningId, name = "宴会", sortOrder = 2, outputOrder = 2),
                AccountSubCategory(parentId = diningId, name = "コンパニオン料金", sortOrder = 3, outputOrder = 3),
                AccountSubCategory(parentId = diningId, name = "お茶代", sortOrder = 4, outputOrder = 4)
            )

            categoryDao.insertSubCategories(travelSubCategories + diningSubCategories)
        }
    }
}
