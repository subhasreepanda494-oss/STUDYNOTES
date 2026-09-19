package com.example.data.repository

import com.example.data.db.ChatDao
import com.example.data.db.PyqDao
import com.example.data.db.StudyNoteDao
import com.example.data.model.ChatMessage
import com.example.data.model.PyqQuestion
import com.example.data.model.StudyNote
import kotlinx.coroutines.flow.Flow

class StudyRepository(
    private val studyNoteDao: StudyNoteDao,
    private val chatDao: ChatDao,
    private val pyqDao: PyqDao
) {
    val allNotes: Flow<List<StudyNote>> = studyNoteDao.getAllNotes()
    val bookmarkedNotes: Flow<List<StudyNote>> = studyNoteDao.getBookmarkedNotes()
    val allPyqs: Flow<List<PyqQuestion>> = pyqDao.getAllPyqs()

    fun getPyqsBySubject(subject: String): Flow<List<PyqQuestion>> = pyqDao.getPyqsBySubject(subject)

    suspend fun getPyqById(id: Long): PyqQuestion? = pyqDao.getPyqById(id)

    suspend fun insertPyq(pyq: PyqQuestion): Long = pyqDao.insertPyq(pyq)

    suspend fun updatePyq(pyq: PyqQuestion) = pyqDao.updatePyq(pyq)

    suspend fun deletePyq(pyq: PyqQuestion) = pyqDao.deletePyq(pyq)

    suspend fun togglePyqBookmark(id: Long, isBookmarked: Boolean) =
        pyqDao.toggleBookmark(id, isBookmarked)

    fun getRecentNotes(limit: Int = 5): Flow<List<StudyNote>> = studyNoteDao.getRecentNotes(limit)

    fun getNoteById(id: Long): Flow<StudyNote?> = studyNoteDao.getNoteById(id)

    fun searchNotes(query: String): Flow<List<StudyNote>> = studyNoteDao.searchNotes(query)

    suspend fun insertNote(note: StudyNote): Long = studyNoteDao.insertNote(note)

    suspend fun updateNote(note: StudyNote) = studyNoteDao.updateNote(note)

    suspend fun deleteNote(note: StudyNote) {
        studyNoteDao.deleteNote(note)
        chatDao.deleteMessagesForNote(note.id)
    }

    suspend fun deleteNoteById(id: Long) {
        studyNoteDao.deleteNoteById(id)
        chatDao.deleteMessagesForNote(id)
    }

    suspend fun toggleBookmark(id: Long, isBookmarked: Boolean) =
        studyNoteDao.setBookmark(id, isBookmarked)

    fun getChatMessages(noteId: Long): Flow<List<ChatMessage>> = chatDao.getMessagesForNote(noteId)

    suspend fun sendChatMessage(message: ChatMessage): Long = chatDao.insertMessage(message)

    suspend fun clearChatMessages(noteId: Long) = chatDao.deleteMessagesForNote(noteId)
}
