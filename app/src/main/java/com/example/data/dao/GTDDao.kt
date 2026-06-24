package com.example.data.dao

import androidx.room.*
import com.example.data.model.GTDProject
import com.example.data.model.GTDTag
import com.example.data.model.GTDTask
import kotlinx.coroutines.flow.Flow

@Dao
interface GTDTaskDao {
    @Query("SELECT * FROM tasks ORDER BY creationDate DESC")
    fun getAllTasks(): Flow<List<GTDTask>>

    @Query("SELECT * FROM tasks WHERE gtdStatus = :status AND isCompleted = 0 ORDER BY creationDate DESC")
    fun getActiveTasksByStatus(status: String): Flow<List<GTDTask>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    fun getAllActiveTasks(): Flow<List<GTDTask>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): GTDTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: GTDTask): Long

    @Update
    suspend fun updateTask(task: GTDTask)

    @Delete
    suspend fun deleteTask(task: GTDTask)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
}

@Dao
interface GTDProjectDao {
    @Query("SELECT * FROM projects ORDER BY id DESC")
    fun getAllProjects(): Flow<List<GTDProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: GTDProject): Long

    @Delete
    suspend fun deleteProject(project: GTDProject)
}

@Dao
interface GTDTagDao {
    @Query("SELECT * FROM tags ORDER BY id DESC")
    fun getAllTags(): Flow<List<GTDTag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: GTDTag): Long

    @Delete
    suspend fun deleteTag(tag: GTDTag)
}
