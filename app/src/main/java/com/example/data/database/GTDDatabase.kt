package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.GTDProjectDao
import com.example.data.dao.GTDTagDao
import com.example.data.dao.GTDTaskDao
import com.example.data.model.GTDProject
import com.example.data.model.GTDTag
import com.example.data.model.GTDTask

@Database(
    entities = [GTDTask::class, GTDProject::class, GTDTag::class],
    version = 1,
    exportSchema = false
)
abstract class GTDDatabase : RoomDatabase() {
    abstract fun taskDao(): GTDTaskDao
    abstract fun projectDao(): GTDProjectDao
    abstract fun tagDao(): GTDTagDao

    companion object {
        @Volatile
        private var INSTANCE: GTDDatabase? = null

        fun getDatabase(context: Context): GTDDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GTDDatabase::class.java,
                    "gtd_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
