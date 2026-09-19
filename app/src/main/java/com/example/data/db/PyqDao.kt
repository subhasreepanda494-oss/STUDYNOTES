package com.example.data.db

import androidx.room.*
import com.example.data.model.PyqQuestion
import kotlinx.coroutines.flow.Flow

@Dao
interface PyqDao {
    @Query("SELECT * FROM pyq_questions ORDER BY createdAt DESC")
    fun getAllPyqs(): Flow<List<PyqQuestion>>

    @Query("SELECT * FROM pyq_questions WHERE subject = :subject ORDER BY createdAt DESC")
    fun getPyqsBySubject(subject: String): Flow<List<PyqQuestion>>

    @Query("SELECT * FROM pyq_questions WHERE id = :id")
    suspend fun getPyqById(id: Long): PyqQuestion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPyq(pyq: PyqQuestion): Long

    @Update
    suspend fun updatePyq(pyq: PyqQuestion)

    @Delete
    suspend fun deletePyq(pyq: PyqQuestion)

    @Query("UPDATE pyq_questions SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun toggleBookmark(id: Long, isBookmarked: Boolean)
}
