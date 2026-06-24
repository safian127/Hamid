package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class GTDProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#9C27B0"
)

@Entity(tableName = "tasks")
data class GTDTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val gtdStatus: String = "INBOX", // INBOX, NEXT, WAITING, SOMEDAY, SCHEDULED
    val projectId: Int? = null,
    val tags: String = "", // Comma separated tag names
    val dueDate: Long? = null,
    val creationDate: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val waitingFor: String? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val googleCalendarEventId: String? = null,
    val syncId: String = java.util.UUID.randomUUID().toString()
)

@Entity(tableName = "tags")
data class GTDTag(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String = "#03A9F4"
)
