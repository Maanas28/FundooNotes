package com.example.fundoonotes.common.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fundoonotes.common.data.entity.LabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: LabelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<LabelEntity>)

    @Update
    suspend fun updateLabel(label: LabelEntity)

    @Query("DELETE FROM labels WHERE id = :labelId")
    suspend fun deleteLabel(labelId: String)

    @Query("SELECT * FROM labels WHERE userId = :userId ORDER BY name ASC")
    fun observeLabels(userId: String): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE userId = :userId ORDER BY name ASC")
    suspend fun getLabels(userId: String): List<LabelEntity>

    @Query("DELETE FROM labels WHERE userId = :userId")
    suspend fun clearLabelsForUser(userId: String)
}

