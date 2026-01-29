package com.example.noty.data

import kotlinx.coroutines.flow.Flow

class NotyRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun insert(task: Task): Long {
        return taskDao.insertTask(task)
    }

    suspend fun delete(task: Task) {
        taskDao.deleteTask(task)
    }
    
    suspend fun deleteById(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }
}
