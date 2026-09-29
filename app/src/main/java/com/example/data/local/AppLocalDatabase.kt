package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Database Master Room untuk Chatin Enterprise Replicator.
 * Mengimplementasikan Singleton Pattern thread-safe dengan fallbackToDestructiveMigration()
 * untuk mencegah crash database akibat skema migrasi.
 */
@Database(
    entities = [LocalMessage::class],
    version = 1,
    exportSchema = false
)
abstract class AppLocalDatabase : RoomDatabase() {

    abstract fun localMessageDao(): LocalMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppLocalDatabase? = null

        fun getInstance(context: Context): AppLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppLocalDatabase::class.java,
                    "chatin_enterprise_local.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
