package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FixedScheduleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedScheduleDao {
    @Query("SELECT * FROM fixed_schedules ORDER BY startTime ASC")
    fun getAllFixedSchedules(): Flow<List<FixedScheduleItem>>

    @Query("SELECT * FROM fixed_schedules ORDER BY startTime ASC")
    suspend fun getAllFixedSchedulesSync(): List<FixedScheduleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(item: FixedScheduleItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(items: List<FixedScheduleItem>)

    @Update
    suspend fun updateSchedule(item: FixedScheduleItem)

    @Delete
    suspend fun deleteSchedule(item: FixedScheduleItem)
}
