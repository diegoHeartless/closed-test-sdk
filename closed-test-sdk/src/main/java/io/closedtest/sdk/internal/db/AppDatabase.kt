package io.closedtest.sdk.internal.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [QueuedEventEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun queuedEventDao(): QueuedEventDao

    companion object {
        private const val NAME = "closed_test_sdk.db"

        private val sqlitePragmasCallback =
            object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    // Avoid mmap + WAL-heavy paths that trigger denied F2FS ioctls on some OEM SELinux
                    // (e.g. audit ioctlcmd=0xf522 on app_data_file) while keeping a single-file journal.
                    db.execSQL("PRAGMA mmap_size = 0")
                }
            }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .fallbackToDestructiveMigration()
                .addCallback(sqlitePragmasCallback)
                .build()
    }
}
