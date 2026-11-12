package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SelfMenuItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SelfMenuDao {
    @Query("SELECT * FROM self_menu_items ORDER BY isFavorite DESC, timesChosen DESC, id ASC")
    fun getAllMenuItems(): Flow<List<SelfMenuItem>>

    @Query("SELECT * FROM self_menu_items ORDER BY isFavorite DESC, timesChosen DESC, id ASC")
    suspend fun getAllMenuItemsSync(): List<SelfMenuItem>

    @Query("SELECT COUNT(*) FROM self_menu_items")
    suspend fun getMenuCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: SelfMenuItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMenuItems(items: List<SelfMenuItem>)

    @Update
    suspend fun updateMenuItem(item: SelfMenuItem)

    @Delete
    suspend fun deleteMenuItem(item: SelfMenuItem)
}
