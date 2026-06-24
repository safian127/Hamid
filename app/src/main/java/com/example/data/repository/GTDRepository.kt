package com.example.data.repository

import com.example.data.dao.GTDProjectDao
import com.example.data.dao.GTDTagDao
import com.example.data.dao.GTDTaskDao
import com.example.data.model.GTDProject
import com.example.data.model.GTDTag
import com.example.data.model.GTDTask
import kotlinx.coroutines.flow.Flow

class GTDRepository(
    private val taskDao: GTDTaskDao,
    private val projectDao: GTDProjectDao,
    private val tagDao: GTDTagDao
) {
    val allTasks: Flow<List<GTDTask>> = taskDao.getAllTasks()
    val allActiveTasks: Flow<List<GTDTask>> = taskDao.getAllActiveTasks()
    val allProjects: Flow<List<GTDProject>> = projectDao.getAllProjects()
    val allTags: Flow<List<GTDTag>> = tagDao.getAllTags()

    fun getActiveTasksByStatus(status: String): Flow<List<GTDTask>> {
        return taskDao.getActiveTasksByStatus(status)
    }

    suspend fun getTaskById(id: Int): GTDTask? {
        return taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: GTDTask): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: GTDTask) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: GTDTask) {
        taskDao.deleteTask(task)
    }

    suspend fun clearAllTasks() {
        taskDao.clearAllTasks()
    }

    suspend fun insertProject(project: GTDProject): Long {
        return projectDao.insertProject(project)
    }

    suspend fun deleteProject(project: GTDProject) {
        projectDao.deleteProject(project)
    }

    suspend fun insertTag(tag: GTDTag): Long {
        return tagDao.insertTag(tag)
    }

    suspend fun deleteTag(tag: GTDTag) {
        tagDao.deleteTag(tag)
    }
}
