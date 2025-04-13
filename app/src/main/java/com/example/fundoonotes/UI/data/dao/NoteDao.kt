package com.example.fundoonotes.UI.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fundoonotes.UI.data.entity.NoteEntity

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)

    @Query("SELECT * FROM notes WHERE userId = :userId AND archived = 0 AND deleted = 0 AND inBin = 0 ORDER BY timestamp DESC")
    suspend fun getNotes(userId: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE userId = :userId AND archived = 1 AND deleted = 0 AND inBin = 0 ORDER BY timestamp DESC")
    suspend fun getArchivedNotes(userId: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE userId = :userId AND deleted = 0 AND inBin = 1 ORDER BY timestamp DESC")
    suspend fun getBinNotes(userId: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE userId = :userId AND hasReminder = 1 AND inBin = 0 AND deleted = 0 ORDER BY timestamp DESC")
    suspend fun getReminderNotes(userId: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): NoteEntity?
}