package com.eina.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMetricDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bodyMetric: BodyMetricEntity): Long

    @Query("SELECT * FROM body_metrics ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): BodyMetricEntity?

    @Query("SELECT * FROM body_metrics ORDER BY date DESC")
    fun getAll(): Flow<List<BodyMetricEntity>>
}
