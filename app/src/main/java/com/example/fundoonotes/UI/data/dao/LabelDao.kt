package com.example.fundoonotes.UI.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fundoonotes.UI.data.entity.LabelEntity

@Dao
interface LabelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: LabelEntity)

    @Update
    suspend fun updateLabel(label: LabelEntity)

    @Query("DELETE FROM labels WHERE id = :labelId")
    suspend fun deleteLabel(labelId: String)

    @Query("SELECT * FROM labels WHERE userId = :userId")
    suspend fun getLabels(userId: String): List<LabelEntity>

    @Query("SELECT * FROM labels WHERE id = :labelId")
    suspend fun getLabelById(labelId: String): LabelEntity?
}
