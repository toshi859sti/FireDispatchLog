package com.firedispatch.log.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.firedispatch.log.data.dao.AppSettingsDao
import com.firedispatch.log.data.dao.AttendanceDao
import com.firedispatch.log.data.dao.BackgroundColorDao
import com.firedispatch.log.data.dao.EventDao
import com.firedispatch.log.data.dao.MemberDao
import com.firedispatch.log.data.dao.RoleAssignmentDao
import com.firedispatch.log.data.dao.RoleMemberCountDao
import com.firedispatch.log.data.entity.AccountCategory
import com.firedispatch.log.data.entity.AccountSubCategory
import com.firedispatch.log.data.entity.AppSettings
import com.firedispatch.log.data.entity.Attendance
import com.firedispatch.log.data.entity.BackgroundColorPreset
import com.firedispatch.log.data.entity.Event
import com.firedispatch.log.data.entity.FiscalYear
import com.firedispatch.log.data.entity.Member
import com.firedispatch.log.data.entity.RoleAssignment
import com.firedispatch.log.data.entity.RoleMemberCount
import com.firedispatch.log.data.entity.ScreenBackgroundMapping
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
        Transaction::class,
        // 背景色設定エンティティ
        BackgroundColorPreset::class,
        ScreenBackgroundMapping::class
    ],
    version = 3,  // 2 → 3 に変更
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
    // 背景色設定DAO
    abstract fun backgroundColorDao(): BackgroundColorDao

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

        /**
         * マイグレーション 2→3: 背景色設定テーブルの追加
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // background_color_presets テーブル
                database.execSQL("""
                    CREATE TABLE background_color_presets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color1 TEXT NOT NULL,
                        color2 TEXT NOT NULL,
                        color3 TEXT NOT NULL
                    )
                """)

                // screen_background_mappings テーブル
                database.execSQL("""
                    CREATE TABLE screen_background_mappings (
                        screenName TEXT PRIMARY KEY NOT NULL,
                        presetId INTEGER NOT NULL
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)  // マイグレーション追加
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // デフォルト科目データを直接SQL で投入
                            insertDefaultCategories(db)
                        }
                    })
                    // .fallbackToDestructiveMigration() を削除（既存データ保護）
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * デフォルト科目データをSQLで直接投入
         */
        private fun insertDefaultCategories(db: SupportSQLiteDatabase) {
            // 収入科目（4件）
            db.execSQL("INSERT INTO account_categories (name, isIncome, isEditable, sortOrder, outputOrder) VALUES ('南島原市会計課', 1, 0, 1, 1)")
            db.execSQL("INSERT INTO account_categories (name, isIncome, isEditable, sortOrder, outputOrder) VALUES ('南島原市防災課', 1, 0, 2, 2)")
            db.execSQL("INSERT INTO account_categories (name, isIncome, isEditable, sortOrder, outputOrder) VALUES ('預金利息', 1, 0, 3, 3)")
            db.execSQL("INSERT INTO account_categories (name, isIncome, isEditable, sortOrder, outputOrder) VALUES ('寸志', 1, 0, 4, 4)")

            // 支出科目（8件）
            db.execSQL("INSERT INTO account_categories (name, isIncome, isEditable, sortOrder, outputOrder) VALUES ('消防団共済掛金', 0, 0, 1, 1)")
            db.execSQL("INSERT INTO account_categories (name, isIncome, isEditable, sortOrder, outputOrder) VALUES ('消防団負担金', 0, 0, 2, 2)")
            db.execSQL("INSERT INTO account_categories (name, isIncome, isEditable, sortOrder, outputOrder) VALUES ('年末警戒', 0, 0, 3, 3)")
            db.execSQL("INSERT INTO account_categories (name, isIncome, isEditable, sortOrder, outputOrder) VALUES ('旅行関連', 0, 0, 4, 4)")
            db.execSQL("INSERT INTO account_categories (name, isIncome, isEditable, sortOrder, outputOrder) VALUES ('飲食関連', 0, 0, 5, 5)")
            db.execSQL("INSERT INTO account_categories (name, isIncome, isEditable, sortOrder, outputOrder) VALUES ('環境整備費', 0, 0, 6, 6)")
            db.execSQL("INSERT INTO account_categories (name, isIncome, isEditable, sortOrder, outputOrder) VALUES ('出動手当金', 0, 0, 7, 7)")
            db.execSQL("INSERT INTO account_categories (name, isIncome, isEditable, sortOrder, outputOrder) VALUES ('免許補助金', 0, 1, 8, 8)")

            // 旅行関連（id=8）の補助科目
            db.execSQL("INSERT INTO account_sub_categories (parentId, name, sortOrder, outputOrder, isEditable) VALUES (8, '宿泊料', 1, 1, 1)")
            db.execSQL("INSERT INTO account_sub_categories (parentId, name, sortOrder, outputOrder, isEditable) VALUES (8, 'フェリー代', 2, 2, 1)")
            db.execSQL("INSERT INTO account_sub_categories (parentId, name, sortOrder, outputOrder, isEditable) VALUES (8, '車代', 3, 3, 1)")
            db.execSQL("INSERT INTO account_sub_categories (parentId, name, sortOrder, outputOrder, isEditable) VALUES (8, '食事代', 4, 4, 1)")
            db.execSQL("INSERT INTO account_sub_categories (parentId, name, sortOrder, outputOrder, isEditable) VALUES (8, '宴会料金', 5, 5, 1)")
            db.execSQL("INSERT INTO account_sub_categories (parentId, name, sortOrder, outputOrder, isEditable) VALUES (8, 'コンパニオン料金', 6, 6, 1)")
            db.execSQL("INSERT INTO account_sub_categories (parentId, name, sortOrder, outputOrder, isEditable) VALUES (8, '土産代', 7, 7, 1)")
            db.execSQL("INSERT INTO account_sub_categories (parentId, name, sortOrder, outputOrder, isEditable) VALUES (8, '駐車料金', 8, 8, 1)")
            db.execSQL("INSERT INTO account_sub_categories (parentId, name, sortOrder, outputOrder, isEditable) VALUES (8, '二次会料金', 9, 9, 1)")

            // 飲食関連（id=9）の補助科目
            db.execSQL("INSERT INTO account_sub_categories (parentId, name, sortOrder, outputOrder, isEditable) VALUES (9, '歓送迎会', 1, 1, 1)")
            db.execSQL("INSERT INTO account_sub_categories (parentId, name, sortOrder, outputOrder, isEditable) VALUES (9, '宴会', 2, 2, 1)")
            db.execSQL("INSERT INTO account_sub_categories (parentId, name, sortOrder, outputOrder, isEditable) VALUES (9, 'コンパニオン料金', 3, 3, 1)")
            db.execSQL("INSERT INTO account_sub_categories (parentId, name, sortOrder, outputOrder, isEditable) VALUES (9, 'お茶代', 4, 4, 1)")
        }
    }
}
