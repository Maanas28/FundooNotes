package com.example.fundoonotes.UI.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.fundoonotes.UI.data.entity.OfflineOperation

@Dao
interface OfflineOperationDao {

    @Insert
    suspend fun insertOfflineOperation(op: OfflineOperation)

    @Query("SELECT * FROM offline_operations ORDER BY timestamp ASC")
    suspend fun getOfflineOperations(): List<OfflineOperation>

    @Query("DELETE FROM offline_operations WHERE id = :id")
    suspend fun removeOfflineOperation(id: Int)
}