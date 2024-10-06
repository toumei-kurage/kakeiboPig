package com.websarva.wings.android.kakeibo.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.websarva.wings.android.kakeibo.room.member.Person
import com.websarva.wings.android.kakeibo.room.member.PersonDao
import com.websarva.wings.android.kakeibo.room.payrecord.Payment
import com.websarva.wings.android.kakeibo.room.payrecord.PaymentDao

@Database(entities = [Person::class, Payment::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
