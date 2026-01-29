package com.example.noty.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val type:  TaskType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TaskType {
    TASK, REMINDER, WORK
}
