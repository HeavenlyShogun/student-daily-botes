package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyHealthLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Query("SELECT * FROM daily_health_logs WHERE date = :date")
    fun getHealthLogByDate(date: String): Flow<DailyHealthLog?>

    @Query("SELECT * FROM daily_health_logs WHERE date = :date")
    suspend fun getHealthLogByDateSync(date: String): DailyHealthLog?

    @Query("SELECT * FROM daily_health_logs ORDER BY date DESC LIMIT 7")
    fun getRecentHealthLogs(): Flow<List<DailyHealthLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(log: DailyHealthLog)

    @Update
    suspend fun update(log: DailyHealthLog)
}
