package io.closedtest.sdk.internal.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
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
                    // Avoid mmap on some OEMs (SELinux ioctl on DB). PRAGMA returns rows on many SQLite builds —
                    // must use query + close, not execSQL (Samsung/Android 12: "Queries can be performed … rawQuery only").
                    db.query(SimpleSQLiteQuery("PRAGMA mmap_size = 0")).close()
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
