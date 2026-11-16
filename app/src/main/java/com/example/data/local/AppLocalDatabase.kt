package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database Master Room untuk Chatin Enterprise Replicator.
 * Menggunakan migrasi eksplisit agar riwayat pesan lokal tidak terhapus diam-diam
 * ketika skema database berubah.
 */
@Database(
    entities = [LocalMessage::class],
    version = 2,
    exportSchema = false
)
abstract class AppLocalDatabase : RoomDatabase() {

    abstract fun localMessageDao(): LocalMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppLocalDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE local_messages ADD COLUMN chatId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE local_messages ADD COLUMN firebaseSenderUid TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppLocalDatabase::class.java,
                    "chatin_enterprise_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
