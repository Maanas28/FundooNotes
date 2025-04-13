package com.example.fundoonotes.UI.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fundoonotes.UI.data.dao.LabelDao
import com.example.fundoonotes.UI.data.dao.NoteDao
import com.example.fundoonotes.UI.data.dao.UserDao
import com.example.fundoonotes.UI.data.entity.LabelEntity
import com.example.fundoonotes.UI.data.entity.NoteEntity
import com.example.fundoonotes.UI.data.entity.UserEntity

@Database(entities = [NoteEntity::class, LabelEntity::class, UserEntity::class], version = 1)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun labelDao(): LabelDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile private var instance: NotesDatabase? = null

        fun getInstance(context: Context): NotesDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    NotesDatabase::class.java,
                    "fundoo_notes_db"
                ).build().also { instance = it }
            }
        }
    }
}
