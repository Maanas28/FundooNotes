package com.example.fundoonotes.common.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fundoonotes.common.data.dao.LabelDao
import com.example.fundoonotes.common.data.dao.NoteDao
import com.example.fundoonotes.common.data.dao.OfflineOperationDao
import com.example.fundoonotes.common.data.dao.UserDao
import com.example.fundoonotes.common.data.entity.LabelEntity
import com.example.fundoonotes.common.data.entity.NoteEntity
import com.example.fundoonotes.common.data.entity.OfflineOperation
import com.example.fundoonotes.common.data.entity.UserEntity

@Database(entities = [NoteEntity::class, LabelEntity::class, UserEntity::class, OfflineOperation::class], version = 1)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun labelDao(): LabelDao
    abstract fun userDao(): UserDao
    abstract fun offlineOperationDao(): OfflineOperationDao

    companion object {
        @Volatile private var instance: NotesDatabase? = null

        fun getInstance(context: Context): NotesDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    NotesDatabase::class.java,
                    "fundoo_notes_db"
                )
                    .build()
                    .also { instance = it }
            }
        }
    }
}