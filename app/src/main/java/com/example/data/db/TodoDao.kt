package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TodoTask
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_tasks ORDER BY isCompleted ASC, priority ASC, id DESC")
    fun getAllTasks(): Flow<List<TodoTask>>

    @Query("SELECT * FROM todo_tasks WHERE isCompleted = 0 ORDER BY priority ASC, id DESC")
    fun getActiveTasks(): Flow<List<TodoTask>>

    @Query("SELECT * FROM todo_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TodoTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TodoTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TodoTask>)

    @Update
    suspend fun updateTask(task: TodoTask)

    @Delete
    suspend fun deleteTask(task: TodoTask)

    @Query("DELETE FROM todo_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}
