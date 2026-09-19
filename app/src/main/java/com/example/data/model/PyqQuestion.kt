package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pyq_questions")
data class PyqQuestion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val question: String,
    val year: String = "2024",
    val marks: Int = 10,
    val answer: String = "",
    val markingScheme: String = "",
    val examinerTips: String = "",
    val relatedNoteId: Long? = null,
    val isSolved: Boolean = false,
    val isBookmarked: Boolean = false,
    val followUpChatJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)

data class PyqFollowUpMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
