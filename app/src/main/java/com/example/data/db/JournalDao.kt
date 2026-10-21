package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyJournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM daily_journal_entries WHERE date = :date")
    fun getJournalEntryByDate(date: String): Flow<DailyJournalEntry?>

    @Query("SELECT * FROM daily_journal_entries WHERE date = :date")
    suspend fun getJournalEntryByDateSync(date: String): DailyJournalEntry?

    @Query("SELECT * FROM daily_journal_entries ORDER BY date DESC")
    fun getAllJournalEntries(): Flow<List<DailyJournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: DailyJournalEntry)

    @Update
    suspend fun update(entry: DailyJournalEntry)
}
