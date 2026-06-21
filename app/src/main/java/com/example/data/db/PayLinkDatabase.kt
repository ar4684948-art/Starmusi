package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        SupportMessageEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PayLinkDatabase : RoomDatabase() {
    abstract fun payLinkDao(): PayLinkDao

    companion object {
        @Volatile
        private var INSTANCE: PayLinkDatabase? = null

        fun getDatabase(context: Context): PayLinkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PayLinkDatabase::class.java,
                    "paylink_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
