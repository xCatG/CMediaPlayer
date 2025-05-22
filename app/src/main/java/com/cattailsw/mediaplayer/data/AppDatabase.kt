package com.cattailsw.mediaplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PlaybackHistory::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playbackHistoryDao(): PlaybackHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE playback_history ADD COLUMN title TEXT")
                database.execSQL("ALTER TABLE playback_history ADD COLUMN duration INTEGER")
                database.execSQL("ALTER TABLE playback_history ADD COLUMN artist TEXT")
                database.execSQL("ALTER TABLE playback_history ADD COLUMN artwork_uri TEXT")
                database.execSQL("ALTER TABLE playback_history ADD COLUMN thumbnail_path TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "media_player_database"
                )
                .addMigrations(MIGRATION_1_2) // Add migration here
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
