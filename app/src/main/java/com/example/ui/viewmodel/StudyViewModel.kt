package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.FileContentExtractor
import com.example.data.ai.GeminiStudyService
import com.example.data.ai.UploadedFileInfo
import com.example.data.db.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.PyqFollowUpMessage
import com.example.data.model.PyqQuestion
import com.example.data.model.StudyJsonParser
import com.example.data.model.StudyNote
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProcessingState(
    val isProcessing: Boolean = false,
    val currentStepIndex: Int = 0,
    val currentStepName: String = "",
    val error: String? = null
)

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = StudyRepository(db.studyNoteDao(), db.chatDao(), db.pyqDao())
    private val geminiService = GeminiStudyService()

    // App Data Flows
    val allNotes: StateFlow<List<StudyNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedNotes: StateFlow<List<StudyNote>> = repository.bookmarkedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentNotes: StateFlow<List<StudyNote>> = repository.getRecentNotes(6)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // PYQ (Previous Years Questions) State Flows
    val allPyqs: StateFlow<List<PyqQuestion>> = repository.allPyqs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPyqSubject = MutableStateFlow("All")
    val selectedPyqSubject: StateFlow<String> = _selectedPyqSubject.asStateFlow()

    val filteredPyqs: StateFlow<List<PyqQuestion>> = combine(allPyqs, _selectedPyqSubject) { pyqs, subject ->
        if (subject == "All") pyqs else pyqs.filter { it.subject.equals(subject, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPyq = MutableStateFlow<PyqQuestion?>(null)
    val selectedPyq: StateFlow<PyqQuestion?> = _selectedPyq.asStateFlow()

    private val _isPyqSolving = MutableStateFlow(false)
    val isPyqSolving: StateFlow<Boolean> = _isPyqSolving.asStateFlow()

    private val _pyqFollowUps = MutableStateFlow<List<PyqFollowUpMessage>>(emptyList())
    val pyqFollowUps: StateFlow<List<PyqFollowUpMessage>> = _pyqFollowUps.asStateFlow()

    // Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSubjectFilter = MutableStateFlow("All")
    val selectedSubjectFilter: StateFlow<String> = _selectedSubjectFilter.asStateFlow()

    val filteredNotes: StateFlow<List<StudyNote>> = combine(allNotes, _searchQuery, _selectedSubjectFilter) { notes, query, subject ->
        notes.filter { note ->
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.detailedNotes.contains(query, ignoreCase = true) ||
                    note.subject.contains(query, ignoreCase = true) ||
                    note.shortNotes.contains(query, ignoreCase = true)
            val matchesSubject = subject == "All" || note.subject.equals(subject, ignoreCase = true)
            matchesQuery && matchesSubject
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Note Selection
    private val _currentNote = MutableStateFlow<StudyNote?>(null)
    val currentNote: StateFlow<StudyNote?> = _currentNote.asStateFlow()

    // Chat
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Processing State
    private val _processingState = MutableStateFlow(ProcessingState())
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()

    // Upload Draft State
    private val _selectedUploadFile = MutableStateFlow<UploadedFileInfo?>(null)
    val selectedUploadFile: StateFlow<UploadedFileInfo?> = _selectedUploadFile.asStateFlow()

    private val _selectedSubject = MutableStateFlow("Biology")
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow("Medium")
    val selectedDifficulty: StateFlow<String> = _selectedDifficulty.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    // User Profile & Preferences
    private val _userName = MutableStateFlow("Alex Morgan")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("alex.morgan@studyai.edu")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Quiz State
    private val _userAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val userAnswers: StateFlow<Map<Int, Int>> = _userAnswers.asStateFlow()

    private val _quizSubmitted = MutableStateFlow(false)
    val quizSubmitted: StateFlow<Boolean> = _quizSubmitted.asStateFlow()

    // Flashcard State
    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    private val _isCardFlipped = MutableStateFlow(false)
    val isCardFlipped: StateFlow<Boolean> = _isCardFlipped.asStateFlow()

    private val _knownCardsCount = MutableStateFlow(0)
    val knownCardsCount: StateFlow<Int> = _knownCardsCount.asStateFlow()

    private val _needReviewCardsCount = MutableStateFlow(0)
    val needReviewCardsCount: StateFlow<Int> = _needReviewCardsCount.asStateFlow()

    init {
        initSampleDataIfNeeded()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSubjectFilter(subject: String) {
        _selectedSubjectFilter.value = subject
    }

    fun setSelectedUploadFile(fileInfo: UploadedFileInfo?) {
        _selectedUploadFile.value = fileInfo
    }

    fun setSelectedSubject(subject: String) {
        _selectedSubject.value = subject
    }

    fun setSelectedDifficulty(difficulty: String) {
        _selectedDifficulty.value = difficulty
    }

    fun setSelectedLanguage(language: String) {
        _selectedLanguage.value = language
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setLoggedIn(loggedIn: Boolean) {
        _isLoggedIn.value = loggedIn
    }

    fun updateProfile(name: String, email: String) {
        _userName.value = name
        _userEmail.value = email
    }

    fun selectNote(note: StudyNote) {
        _currentNote.value = note
        _userAnswers.value = emptyMap()
        _quizSubmitted.value = false
        _currentCardIndex.value = 0
        _isCardFlipped.value = false
        _knownCardsCount.value = 0
        _needReviewCardsCount.value = 0
        loadChatMessages(note.id)
    }

    private fun loadChatMessages(noteId: Long) {
        viewModelScope.launch {
            repository.getChatMessages(noteId).collect {
                _chatMessages.value = it
            }
        }
    }

    fun toggleBookmark(note: StudyNote) {
        viewModelScope.launch {
            val updatedBookmark = !note.isBookmarked
            repository.toggleBookmark(note.id, updatedBookmark)
            if (_currentNote.value?.id == note.id) {
                _currentNote.value = _currentNote.value?.copy(isBookmarked = updatedBookmark)
            }
        }
    }

    fun deleteNote(note: StudyNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
            if (_currentNote.value?.id == note.id) {
                _currentNote.value = null
            }
        }
    }

    fun updateNote(note: StudyNote, title: String, detailed: String) {
        viewModelScope.launch {
            val updated = note.copy(
                title = title,
                detailedNotes = detailed,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateNote(updated)
            _currentNote.value = updated
        }
    }

    // Process & Generate Study Notes
    fun startProcessing(onSuccess: (StudyNote) -> Unit) {
        val file = _selectedUploadFile.value ?: return
        val subject = _selectedSubject.value
        val difficulty = _selectedDifficulty.value
        val language = _selectedLanguage.value

        viewModelScope.launch {
            _processingState.value = ProcessingState(isProcessing = true, currentStepIndex = 0, currentStepName = "Reading document...")

            try {
                val result = geminiService.generateCompleteStudyMaterial(
                    content = file.contentPreview,
                    subject = subject,
                    difficulty = difficulty,
                    language = language
                ) { stepIndex, stepName ->
                    _processingState.value = ProcessingState(
                        isProcessing = true,
                        currentStepIndex = stepIndex,
                        currentStepName = stepName
                    )
                }

                val newNote = StudyNote(
                    title = result.title,
                    subject = subject,
                    difficulty = difficulty,
                    language = language,
                    originalFileName = file.fileName,
                    fileType = file.fileType,
                    rawContent = file.contentPreview,
                    detailedNotes = result.detailedNotes,
                    shortNotes = result.shortNotes,
                    quickRevisionNotes = result.quickRevisionNotes,
                    examNotes = result.examNotes,
                    definitionsJson = StudyJsonParser.toJson(result.definitions),
                    formulasJson = StudyJsonParser.formulasToJson(result.formulas),
                    keyPointsJson = org.json.JSONArray(result.keyPoints).toString(),
                    summary = result.summary,
                    mcqsJson = StudyJsonParser.mcqsToJson(result.mcqs),
                    shortQuestionsJson = StudyJsonParser.shortQuestionsToJson(result.shortQuestions),
                    flashcardsJson = StudyJsonParser.flashcardsToJson(result.flashcards),
                    mindMapJson = StudyJsonParser.mindMapToJson(result.mindMap),
                    isBookmarked = false
                )

                val newId = repository.insertNote(newNote)
                val savedNote = newNote.copy(id = newId)

                _processingState.value = ProcessingState(isProcessing = false)
                _currentNote.value = savedNote
                _selectedUploadFile.value = null

                onSuccess(savedNote)
            } catch (e: Exception) {
                _processingState.value = ProcessingState(
                    isProcessing = false,
                    error = "Generation failed: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    // AI Chat
    fun sendChatMessage(text: String) {
        val note = _currentNote.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            val userMsg = ChatMessage(noteId = note.id, sender = "user", text = text)
            repository.sendChatMessage(userMsg)

            _isChatLoading.value = true
            val aiResponse = geminiService.askChatQuestion(
                noteTitle = note.title,
                noteContent = note.rawContent.ifBlank { note.detailedNotes },
                chatHistory = _chatMessages.value,
                question = text
            )

            val aiMsg = ChatMessage(noteId = note.id, sender = "ai", text = aiResponse)
            repository.sendChatMessage(aiMsg)
            _isChatLoading.value = false
        }
    }

    // Quiz Actions
    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        if (_quizSubmitted.value) return
        val current = _userAnswers.value.toMutableMap()
        current[questionIndex] = optionIndex
        _userAnswers.value = current
    }

    fun submitQuiz() {
        _quizSubmitted.value = true
    }

    fun resetQuiz() {
        _userAnswers.value = emptyMap()
        _quizSubmitted.value = false
    }

    // Flashcard Actions
    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    fun nextCard(totalCards: Int, knewAnswer: Boolean) {
        if (knewAnswer) {
            _knownCardsCount.value += 1
        } else {
            _needReviewCardsCount.value += 1
        }
        if (totalCards > 0) {
            _currentCardIndex.value = (_currentCardIndex.value + 1) % totalCards
            _isCardFlipped.value = false
        }
    }

    fun resetFlashcards() {
        _currentCardIndex.value = 0
        _isCardFlipped.value = false
        _knownCardsCount.value = 0
        _needReviewCardsCount.value = 0
    }

    // PYQ (Previous Years Questions) Actions
    fun filterPyqsBySubject(subject: String) {
        _selectedPyqSubject.value = subject
    }

    fun selectPyq(pyq: PyqQuestion) {
        _selectedPyq.value = pyq
        _pyqFollowUps.value = emptyList()
    }

    fun clearSelectedPyq() {
        _selectedPyq.value = null
        _pyqFollowUps.value = emptyList()
    }

    fun solvePyq(question: String, subject: String, year: String, marks: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isPyqSolving.value = true
            try {
                val noteContext = allNotes.value.find { it.subject.equals(subject, ignoreCase = true) }?.rawContent
                val result = geminiService.solvePyqQuestion(
                    question = question,
                    subject = subject,
                    year = year,
                    marks = marks,
                    noteContext = noteContext
                )

                val newPyq = PyqQuestion(
                    subject = subject,
                    question = question,
                    year = year,
                    marks = marks,
                    answer = result.answer,
                    markingScheme = result.markingScheme,
                    examinerTips = result.examinerTips,
                    isSolved = true
                )
                val id = repository.insertPyq(newPyq)
                _selectedPyq.value = newPyq.copy(id = id)
                _pyqFollowUps.value = emptyList()
            } catch (e: Exception) {
                Log.e("StudyViewModel", "Error solving PYQ", e)
            } finally {
                _isPyqSolving.value = false
            }
        }
    }

    fun solveExistingPyq(pyq: PyqQuestion) {
        if (pyq.isSolved && pyq.answer.isNotBlank()) {
            _selectedPyq.value = pyq
            _pyqFollowUps.value = emptyList()
            return
        }
        solvePyq(pyq.question, pyq.subject, pyq.year, pyq.marks)
    }

    fun sendPyqFollowUp(prompt: String) {
        val current = _selectedPyq.value ?: return
        val userMsg = PyqFollowUpMessage(sender = "user", text = prompt)
        _pyqFollowUps.value = _pyqFollowUps.value + userMsg

        viewModelScope.launch(Dispatchers.IO) {
            _isPyqSolving.value = true
            try {
                val fullContext = buildString {
                    append("Exam PYQ: ${current.question}\n")
                    append("Subject: ${current.subject} | Marks: ${current.marks} | Year: ${current.year}\n")
                    append("ChatGPT Model Answer:\n${current.answer}\n")
                }
                val aiResponseText = geminiService.askChatQuestion(
                    question = prompt,
                    noteTitle = "PYQ Follow-up: ${current.subject}",
                    noteContent = fullContext,
                    chatHistory = _pyqFollowUps.value.map {
                        ChatMessage(noteId = current.id, sender = it.sender, text = it.text)
                    }
                )
                val aiMsg = PyqFollowUpMessage(sender = "ai", text = aiResponseText)
                _pyqFollowUps.value = _pyqFollowUps.value + aiMsg
            } catch (e: Exception) {
                _pyqFollowUps.value = _pyqFollowUps.value + PyqFollowUpMessage(
                    sender = "ai",
                    text = "Key revision tip: When answering this concept in university exams, always start with the formal one-line definition, draw the architectural diagram, and highlight keywords."
                )
            } finally {
                _isPyqSolving.value = false
            }
        }
    }

    fun togglePyqBookmark(pyq: PyqQuestion) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.togglePyqBookmark(pyq.id, !pyq.isBookmarked)
            if (_selectedPyq.value?.id == pyq.id) {
                _selectedPyq.value = _selectedPyq.value?.copy(isBookmarked = !pyq.isBookmarked)
            }
        }
    }

    fun deletePyq(pyq: PyqQuestion) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePyq(pyq)
            if (_selectedPyq.value?.id == pyq.id) {
                _selectedPyq.value = null
            }
        }
    }

    private fun initSampleDataIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = repository.allNotes.first()
                val existingSubjects = existing.map { it.subject }.toSet()
                val docsToAdd = FileContentExtractor.SAMPLE_DOCUMENTS.filter { it.subject !in existingSubjects }
                
                docsToAdd.forEach { doc ->
                    val result = geminiService.generateLocalStructuredMaterial(
                        content = doc.content,
                        subject = doc.subject,
                        difficulty = doc.difficulty,
                        language = "English"
                    )

                    val note = StudyNote(
                        title = doc.title,
                        subject = doc.subject,
                        difficulty = doc.difficulty,
                        language = "English",
                        originalFileName = "${doc.title.lowercase().replace(" ", "_").replace(":", "")}.${doc.fileType.lowercase()}",
                        fileType = doc.fileType,
                        rawContent = doc.content,
                        detailedNotes = result.detailedNotes,
                        shortNotes = result.shortNotes,
                        quickRevisionNotes = result.quickRevisionNotes,
                        examNotes = result.examNotes,
                        definitionsJson = StudyJsonParser.toJson(result.definitions),
                        formulasJson = StudyJsonParser.formulasToJson(result.formulas),
                        keyPointsJson = org.json.JSONArray(result.keyPoints).toString(),
                        summary = result.summary,
                        mcqsJson = StudyJsonParser.mcqsToJson(result.mcqs),
                        shortQuestionsJson = StudyJsonParser.shortQuestionsToJson(result.shortQuestions),
                        flashcardsJson = StudyJsonParser.flashcardsToJson(result.flashcards),
                        mindMapJson = StudyJsonParser.mindMapToJson(result.mindMap),
                        isBookmarked = doc.subject in listOf("CN", "Math for Data Science", "Biology")
                    )
                    repository.insertNote(note)
                }

                // Populate Sample University PYQ Questions if empty
                val existingPyqs = repository.allPyqs.first()
                if (existingPyqs.isEmpty()) {
                    val samplePyqs = listOf(
                        PyqQuestion(
                            subject = "CN",
                            question = "Explain the OSI 7-Layer Reference Model and compare it with the TCP/IP protocol suite with schematic diagrams and Protocol Data Units.",
                            year = "2023",
                            marks = 10,
                            isSolved = false
                        ),
                        PyqQuestion(
                            subject = "CN",
                            question = "Explain the 3-Way Handshake in TCP connection establishment and termination with sequence diagrams and state transitions.",
                            year = "2024",
                            marks = 5,
                            isSolved = false
                        ),
                        PyqQuestion(
                            subject = "SE",
                            question = "Explain the SOLID principles of Object-Oriented Design with practical code examples, violation scenarios, and architectural benefits.",
                            year = "2024",
                            marks = 10,
                            isSolved = false
                        ),
                        PyqQuestion(
                            subject = "SE",
                            question = "What is Cyclomatic Complexity? Explain McCabe's metric calculation formula and how it is derived from Control Flow Graphs.",
                            year = "2023",
                            marks = 5,
                            isSolved = false
                        ),
                        PyqQuestion(
                            subject = "Math for Data Science",
                            question = "Define Eigenvalues and Eigenvectors. Explain how they are computed and derive their role in Principal Component Analysis (PCA).",
                            year = "2024",
                            marks = 10,
                            isSolved = false
                        ),
                        PyqQuestion(
                            subject = "Math for Data Science",
                            question = "Explain Singular Value Decomposition (SVD) of a matrix A = U Σ V^T and its geometric significance in dimensionality reduction.",
                            year = "2023",
                            marks = 10,
                            isSolved = false
                        ),
                        PyqQuestion(
                            subject = "IKS",
                            question = "Explain the contribution of Baudhayana Sulba Sutra to ancient geometry and diagonal theorem (precursor to Pythagorean theorem) with textual proof.",
                            year = "2024",
                            marks = 10,
                            isSolved = false
                        ),
                        PyqQuestion(
                            subject = "IKS",
                            question = "Discuss the contributions of the Kerala School of Mathematics (Madhava of Sangamagrama) to infinite calculus series for sine, cosine, and π.",
                            year = "2023",
                            marks = 10,
                            isSolved = false
                        )
                    )

                    samplePyqs.forEach { pyq ->
                        val result = geminiService.solvePyqQuestion(
                            question = pyq.question,
                            subject = pyq.subject,
                            year = pyq.year,
                            marks = pyq.marks
                        )
                        repository.insertPyq(
                            pyq.copy(
                                answer = result.answer,
                                markingScheme = result.markingScheme,
                                examinerTips = result.examinerTips,
                                isSolved = true,
                                isBookmarked = pyq.subject in listOf("CN", "Math for Data Science")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("StudyViewModel", "Error populating sample study notes and PYQs", e)
            }
        }
    }
}
