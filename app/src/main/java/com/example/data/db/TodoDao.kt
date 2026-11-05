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
    @Query(
        """
        SELECT * FROM todo_tasks
        ORDER BY
            isCompleted ASC,
            CASE priority
                WHEN 'HIGH_URGENT' THEN 1
                WHEN 'HIGH_NOT_URGENT' THEN 2
                WHEN 'NORMAL' THEN 3
                WHEN 'LOW' THEN 4
                ELSE 5
            END ASC,
            CASE WHEN dueDate = '' THEN '9999-12-31' ELSE dueDate END ASC,
            CASE WHEN dueTime = '' THEN '23:59' ELSE dueTime END ASC,
            postponeCount DESC,
            id DESC
        """
    )
    fun getAllTasks(): Flow<List<TodoTask>>

    @Query(
        """
        SELECT * FROM todo_tasks
        WHERE isCompleted = 0
        ORDER BY
            CASE priority
                WHEN 'HIGH_URGENT' THEN 1
                WHEN 'HIGH_NOT_URGENT' THEN 2
                WHEN 'NORMAL' THEN 3
                WHEN 'LOW' THEN 4
                ELSE 5
            END ASC,
            CASE WHEN dueDate = '' THEN '9999-12-31' ELSE dueDate END ASC,
            CASE WHEN dueTime = '' THEN '23:59' ELSE dueTime END ASC,
            postponeCount DESC,
            id DESC
        """
    )
    fun getActiveTasks(): Flow<List<TodoTask>>

    @Query(
        """
        SELECT * FROM todo_tasks
        WHERE isCompleted = 0
        ORDER BY
            CASE priority
                WHEN 'HIGH_URGENT' THEN 1
                WHEN 'HIGH_NOT_URGENT' THEN 2
                WHEN 'NORMAL' THEN 3
                WHEN 'LOW' THEN 4
                ELSE 5
            END ASC,
            CASE WHEN dueDate = '' THEN '9999-12-31' ELSE dueDate END ASC,
            CASE WHEN dueTime = '' THEN '23:59' ELSE dueTime END ASC,
            postponeCount DESC,
            id DESC
        """
    )
    suspend fun getActiveTasksSync(): List<TodoTask>

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
