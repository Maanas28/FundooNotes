package com.example.fundoonotes.UI.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fundoonotes.UI.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)

    @Query("SELECT * FROM notes WHERE userId = :userId AND archived = 0 AND deleted = 0 AND inBin = 0 ORDER BY timestamp DESC")
    fun observeNotes(userId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE userId = :userId AND archived = 1 AND deleted = 0 AND inBin = 0 ORDER BY timestamp DESC")
    fun observeArchivedNotes(userId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE userId = :userId AND deleted = 0 AND inBin = 1 ORDER BY timestamp DESC")
    fun observeBinNotes(userId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE userId = :userId AND hasReminder = 1 AND inBin = 0 AND deleted = 0 ORDER BY timestamp DESC")
    fun observeReminderNotes(userId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE userId = :userId")
    suspend fun getAllNotes(userId: String): List<NoteEntity>

    @Query("DELETE FROM notes WHERE userId = :userId")
    suspend fun clearNotesForUser(userId: String)

    @Query("SELECT * FROM notes WHERE archived = 0 AND inBin = 0 AND deleted = 0 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedNotes(limit: Int, offset: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE archived = 1 AND inBin = 0 AND deleted = 0 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedArchivedNotes(limit: Int, offset: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE inBin = 1 AND deleted = 0 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedBinNotes(limit: Int, offset: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE hasReminder = 1 AND inBin = 0 AND deleted = 0 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedReminderNotes(limit: Int, offset: Int): List<NoteEntity>

    @Query("""
    SELECT * FROM notes
    WHERE labels LIKE '%' || :separator || :labelName || :separator || '%'
       OR labels LIKE :labelName || :separator || '%'
       OR labels LIKE '%' || :separator || :labelName
       OR labels = :labelName
    AND archived = 0 AND inBin = 0 AND deleted = 0
    ORDER BY timestamp DESC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getPagedNotesByLabel(
        labelName: String,
        separator: String = "|:|",
        limit: Int,
        offset: Int
    ): List<NoteEntity>


}
