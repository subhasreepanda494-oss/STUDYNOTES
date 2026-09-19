package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PyqFollowUpMessage
import com.example.data.model.PyqQuestion
import com.example.ui.components.StudyTopBar
import com.example.util.PdfExportManager
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.EmeraldEasy
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PurpleAccent
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PyqSolverScreen(
    viewModel: StudyViewModel,
    onBackClick: () -> Unit
) {
    val selectedPyq by viewModel.selectedPyq.collectAsStateWithLifecycle()
    val isSolving by viewModel.isPyqSolving.collectAsStateWithLifecycle()
    val filteredPyqs by viewModel.filteredPyqs.collectAsStateWithLifecycle()
    val selectedSubjectFilter by viewModel.selectedPyqSubject.collectAsStateWithLifecycle()
    val followUps by viewModel.pyqFollowUps.collectAsStateWithLifecycle()

    val context = LocalContext.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pyq_solver_screen")
    ) {
        val isDualPane = maxWidth >= 840.dp

        // Handle back button: on mobile, if a PYQ is open in detail view, go back to PYQ list
        BackHandler {
            if (!isDualPane && selectedPyq != null) {
                viewModel.clearSelectedPyq()
            } else {
                onBackClick()
            }
        }

        Scaffold(
            topBar = {
                StudyTopBar(
                    title = if (isDualPane) "PYQ Exam Solver • ChatGPT Model Answers" else (if (selectedPyq != null) "ChatGPT Model Answer" else "PYQ Exam Solver"),
                    subtitle = if (isDualPane) {
                        if (selectedPyq != null) "${selectedPyq?.subject} • ${selectedPyq?.marks} Marks • ${selectedPyq?.year}" else "Select or add any university question"
                    } else {
                        if (selectedPyq != null) "${selectedPyq?.subject} • ${selectedPyq?.marks} Marks • ${selectedPyq?.year}" else "Previous Years Questions & ChatGPT Solutions"
                    },
                    onBackClick = {
                        if (!isDualPane && selectedPyq != null) {
                            viewModel.clearSelectedPyq()
                        } else {
                            onBackClick()
                        }
                    },
                    actions = {
                        if (selectedPyq != null) {
                            val pyq = selectedPyq!!
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("PYQ Answer", "${pyq.question}\n\n${pyq.answer}")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Answer copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy Answer")
                            }
                            IconButton(
                                onClick = {
                                    try {
                                        val result = PdfExportManager.generatePyqPdf(context, pyq)
                                        PdfExportManager.printPdf(context, result)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not export PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.PictureAsPdf, contentDescription = "Export PDF / Print")
                            }
                            IconButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "PYQ: ${pyq.question}\n\n[Model Answer by StudyAI ChatGPT]:\n${pyq.answer}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share PYQ Answer"))
                                }
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = "Share")
                            }
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (isDualPane) {
                    // Desktop / Laptop Dual-Pane Layout
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Pane: Question List, Search, and Add Question Form
                        Box(
                            modifier = Modifier
                                .width(380.dp)
                                .fillMaxHeight()
                        ) {
                            PyqListView(
                                filteredPyqs = filteredPyqs,
                                selectedSubjectFilter = selectedSubjectFilter,
                                isSolving = isSolving,
                                onSubjectFilterChange = { viewModel.filterPyqsBySubject(it) },
                                onSelectPyq = { viewModel.solveExistingPyq(it) },
                                onSolveNewPyq = { q, subj, yr, marks ->
                                    viewModel.solvePyq(q, subj, yr, marks)
                                },
                                onBookmarkToggle = { viewModel.togglePyqBookmark(it) },
                                onDeletePyq = { viewModel.deletePyq(it) }
                            )
                        }

                        // Subtle Divider
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        // Right Pane: ChatGPT Model Answer & Follow-up Chat or Placeholder
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (selectedPyq != null) {
                                PyqAnswerDetailView(
                                    pyq = selectedPyq!!,
                                    followUps = followUps,
                                    isSolving = isSolving,
                                    onSendFollowUp = { prompt ->
                                        viewModel.sendPyqFollowUp(prompt)
                                    },
                                    onBookmarkToggle = {
                                        viewModel.togglePyqBookmark(selectedPyq!!)
                                    },
                                    onSolveAnother = {
                                        viewModel.clearSelectedPyq()
                                    }
                                )
                            } else {
                                // Desktop empty state on right pane
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp)
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = CardDefaults.outlinedCardBorder(),
                                        modifier = Modifier.widthIn(max = 480.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .background(IndigoPrimary.copy(alpha = 0.1f))
                                            ) {
                                                Icon(
                                                    Icons.Filled.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = IndigoPrimary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Select a Question to View Solution",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Click any PYQ on the left or tap '+ Add Question' to get a complete ChatGPT-style model answer with marking scheme, step-by-step solutions, and examiner tips.",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Mobile Single-Pane Layout
                    if (selectedPyq != null) {
                        PyqAnswerDetailView(
                            pyq = selectedPyq!!,
                            followUps = followUps,
                            isSolving = isSolving,
                            onSendFollowUp = { prompt ->
                                viewModel.sendPyqFollowUp(prompt)
                            },
                            onBookmarkToggle = {
                                viewModel.togglePyqBookmark(selectedPyq!!)
                            },
                            onSolveAnother = {
                                viewModel.clearSelectedPyq()
                            }
                        )
                    } else {
                        PyqListView(
                            filteredPyqs = filteredPyqs,
                            selectedSubjectFilter = selectedSubjectFilter,
                            isSolving = isSolving,
                            onSubjectFilterChange = { viewModel.filterPyqsBySubject(it) },
                            onSelectPyq = { viewModel.solveExistingPyq(it) },
                            onSolveNewPyq = { q, subj, yr, marks ->
                                viewModel.solvePyq(q, subj, yr, marks)
                            },
                            onBookmarkToggle = { viewModel.togglePyqBookmark(it) },
                            onDeletePyq = { viewModel.deletePyq(it) }
                        )
                    }
                }

                // Global Solving Overlay
                if (isSolving && selectedPyq == null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .widthIn(max = 440.dp)
                                .fillMaxWidth(0.85f)
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = IndigoPrimary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "ChatGPT is Solving PYQ...",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Synthesizing step-by-step university model answer, diagrams, marking scheme, and examiner traps.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PyqListView(
    filteredPyqs: List<PyqQuestion>,
    selectedSubjectFilter: String,
    isSolving: Boolean,
    onSubjectFilterChange: (String) -> Unit,
    onSelectPyq: (PyqQuestion) -> Unit,
    onSolveNewPyq: (question: String, subject: String, year: String, marks: Int) -> Unit,
    onBookmarkToggle: (PyqQuestion) -> Unit,
    onDeletePyq: (PyqQuestion) -> Unit
) {
    val subjects = listOf("All", "CN", "SE", "Math for Data Science", "IKS", "Computer Science", "Physics", "Biology")

    var inputQuestion by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("CN") }
    var selectedYear by remember { mutableStateOf("2024") }
    var selectedMarks by remember { mutableStateOf(10) }
    var showAddForm by remember { mutableStateOf(true) }

    val years = listOf("2024", "2023", "2022", "2021", "2020")
    val marksOptions = listOf(2, 5, 10, 15)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Banner Card: ChatGPT Exam Answering Engine
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ChatGPT PYQ Answer Engine",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Add past university questions (2M, 5M, 10M, 15M) and get step-by-step scoring answers like ChatGPT.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }

        // Add New PYQ Question Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ADD EXAM QUESTION (PYQ)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                        IconButton(
                            onClick = { showAddForm = !showAddForm },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (showAddForm) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Toggle Form"
                            )
                        }
                    }

                    AnimatedVisibility(visible = showAddForm) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = inputQuestion,
                                onValueChange = { inputQuestion = it },
                                label = { Text("Enter or paste your PYQ question") },
                                placeholder = { Text("e.g. Explain 3-Way Handshake in TCP or Calculate Cyclomatic Complexity...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pyq_question_input"),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 3,
                                maxLines = 6
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Subject Selection
                            Text(
                                text = "Select Subject:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("CN", "SE", "Math for Data Science", "IKS", "Biology", "Physics", "Chemistry", "CS")) { subj ->
                                    FilterChip(
                                        selected = selectedSubject == subj,
                                        onClick = { selectedSubject = subj },
                                        label = { Text(subj, fontSize = 12.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Year & Marks Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Year Picker
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Exam Year:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(years) { yr ->
                                            FilterChip(
                                                selected = selectedYear == yr,
                                                onClick = { selectedYear = yr },
                                                label = { Text(yr, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Marks Picker
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Weightage:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(marksOptions) { m ->
                                            FilterChip(
                                                selected = selectedMarks == m,
                                                onClick = { selectedMarks = m },
                                                label = { Text("${m}M", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (inputQuestion.isNotBlank()) {
                                        onSolveNewPyq(inputQuestion.trim(), selectedSubject, selectedYear, selectedMarks)
                                        inputQuestion = ""
                                    }
                                },
                                enabled = inputQuestion.isNotBlank() && !isSolving,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("solve_pyq_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Get Answer Like ChatGPT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Subject Filter Header
        item {
            Column {
                Text(
                    text = "PREVIOUS YEARS QUESTIONS REPOSITORY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subjects) { subj ->
                        FilterChip(
                            selected = selectedSubjectFilter == subj,
                            onClick = { onSubjectFilterChange(subj) },
                            label = { Text(subj, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        // List of Solved / Sample PYQs
        if (filteredPyqs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No PYQs found for $selectedSubjectFilter", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add a question above to generate an instant ChatGPT model answer!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filteredPyqs) { pyq ->
                PyqQuestionCard(
                    pyq = pyq,
                    onClick = { onSelectPyq(pyq) },
                    onBookmarkToggle = { onBookmarkToggle(pyq) },
                    onDelete = { onDeletePyq(pyq) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PyqQuestionCard(
    pyq: PyqQuestion,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SubjectBadge(subject = pyq.subject)

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${pyq.marks} Marks",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = pyq.year,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (pyq.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (pyq.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = pyq.question,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (pyq.isSolved) "ChatGPT Model Answer Ready" else "Tap to Solve",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = IndigoPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Text(
                    text = "View Solution →",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun PyqAnswerDetailView(
    pyq: PyqQuestion,
    followUps: List<PyqFollowUpMessage>,
    isSolving: Boolean,
    onSendFollowUp: (String) -> Unit,
    onBookmarkToggle: () -> Unit,
    onSolveAnother: () -> Unit
) {
    var followUpInput by remember { mutableStateOf("") }

    val quickFollowUps = listOf(
        "Explain this in simpler terms",
        "Give me an analogy to remember",
        "What are the examiner traps?",
        "Shorten this into a 2-mark summary",
        "Give another real-world example"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
        ) {
            // ChatGPT Model Answer Header Banner
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(IndigoPrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "ChatGPT Exam Master",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Structured for maximum university marks",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }

                            IconButton(onClick = onBookmarkToggle) {
                                Icon(
                                    imageVector = if (pyq.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (pyq.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubjectBadge(subject = pyq.subject)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${pyq.marks} Marks",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Year ${pyq.year}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = pyq.question,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                lineHeight = 24.sp
                            )
                        )
                    }
                }
            }

            // Marking Scheme Card
            if (pyq.markingScheme.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Scoreboard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "EXAM SCORING BLUEPRINT (${pyq.marks} MARKS)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = pyq.markingScheme,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Main ChatGPT Model Answer Content
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COMPLETE STEP-BY-STEP SOLUTION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldEasy.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Full Marks Guaranteed",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldEasy
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        SelectionContainer {
                            MarkdownTextRenderer(markdown = pyq.answer)
                        }
                    }
                }
            }

            // Examiner Traps & Tips
            if (pyq.examinerTips.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF3C7) // warm amber
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "EXAMINER'S SCORING ADVICE & TRAPS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = pyq.examinerTips,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF78350F),
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }
                }
            }

            // Follow-up Conversation Items
            if (followUps.isNotEmpty()) {
                item {
                    Text(
                        text = "FOLLOW-UP CONVERSATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(followUps) { msg ->
                    PyqFollowUpBubble(message = msg)
                }
            }

            if (isSolving) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = IndigoPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ChatGPT is generating response...",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            // Solve Another Button
            item {
                OutlinedButton(
                    onClick = onSolveAnother,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Solve Another PYQ Question")
                }
            }
        }

        // Bottom Follow-up Input Bar
        Surface(
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Quick follow-up suggestions
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    items(quickFollowUps) { chipText ->
                        SuggestionChip(
                            onClick = { onSendFollowUp(chipText) },
                            label = { Text(chipText, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = followUpInput,
                        onValueChange = { followUpInput = it },
                        placeholder = { Text("Ask follow-up like ChatGPT...", fontSize = 13.sp) },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pyq_followup_input"),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (followUpInput.isNotBlank() && !isSolving) {
                                val text = followUpInput.trim()
                                followUpInput = ""
                                onSendFollowUp(text)
                            }
                        },
                        enabled = followUpInput.isNotBlank() && !isSolving,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PyqFollowUpBubble(message: PyqFollowUpMessage) {
    val isUser = message.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(IndigoPrimary)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun MarkdownTextRenderer(markdown: String) {
    val lines = markdown.split("\n")
    var inCodeBlock = false
    val codeBlockLines = mutableListOf<String>()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (line in lines) {
            when {
                line.startsWith("```") -> {
                    if (inCodeBlock) {
                        // Flush code block
                        CodeBlockCard(code = codeBlockLines.joinToString("\n"))
                        codeBlockLines.clear()
                        inCodeBlock = false
                    } else {
                        inCodeBlock = true
                    }
                }
                inCodeBlock -> {
                    codeBlockLines.add(line)
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                line.startsWith("#### ") -> {
                    Text(
                        text = line.removePrefix("#### "),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                line.startsWith("---") -> {
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        Text("• ", fontWeight = FontWeight.Bold, color = IndigoPrimary)
                        Text(
                            text = line.substring(2),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                        )
                    }
                }
                line.matches(Regex("^\\d+\\..*")) -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                line.isNotBlank() -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                }
            }
        }

        // Handle unclosed code block if any
        if (codeBlockLines.isNotEmpty()) {
            CodeBlockCard(code = codeBlockLines.joinToString("\n"))
        }
    }
}

@Composable
fun CodeBlockCard(code: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B), // slate dark
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = code,
            color = Color(0xFFE2E8F0),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}
