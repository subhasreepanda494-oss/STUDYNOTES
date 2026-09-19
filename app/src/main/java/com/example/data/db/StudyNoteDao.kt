package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StudyNote
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyNoteDao {
    @Query("SELECT * FROM study_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<StudyNote>>

    @Query("SELECT * FROM study_notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<StudyNote?>

    @Query("SELECT * FROM study_notes ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentNotes(limit: Int): Flow<List<StudyNote>>

    @Query("SELECT * FROM study_notes WHERE isBookmarked = 1 ORDER BY updatedAt DESC")
    fun getBookmarkedNotes(): Flow<List<StudyNote>>

    @Query("SELECT * FROM study_notes WHERE title LIKE '%' || :query || '%' OR detailedNotes LIKE '%' || :query || '%' OR subject LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchNotes(query: String): Flow<List<StudyNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: StudyNote): Long

    @Update
    suspend fun updateNote(note: StudyNote)

    @Delete
    suspend fun deleteNote(note: StudyNote)

    @Query("DELETE FROM study_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("UPDATE study_notes SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun setBookmark(id: Long, isBookmarked: Boolean)
}
