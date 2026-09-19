package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StudyNote
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.StudyTopBar
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.EmeraldEasy
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiToolsScreen(
    viewModel: StudyViewModel,
    onNavigateToQuiz: () -> Unit,
    onNavigateToFlashcards: () -> Unit,
    onNavigateToMindMap: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToPyq: () -> Unit
) {
    val currentNote by viewModel.currentNote.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()

    var showNoteSelectorMenu by remember { mutableStateOf(false) }

    // If no note is currently selected, pick the first available note
    LaunchedEffect(currentNote, allNotes) {
        if (currentNote == null && allNotes.isNotEmpty()) {
            viewModel.selectNote(allNotes.first())
        }
    }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "AI Study Tools",
                subtitle = "Quiz • Flashcards • Mind Map • Chat"
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("ai_tools_screen")
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1000.dp)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
            // Active Study Material Selector Header
            item {
                Card(
                    onClick = { showNoteSelectorMenu = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CURRENT STUDY CONTEXT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Switch",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (currentNote != null) {
                            Text(
                                text = currentNote?.title ?: "",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SubjectBadge(subject = currentNote?.subject ?: "")
                                DifficultyBadge(difficulty = currentNote?.difficulty ?: "")
                            }
                        } else {
                            Text(
                                text = "No study material selected. Tap here or upload a document!",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }

            // Dropdown menu for switching note
            item {
                DropdownMenu(
                    expanded = showNoteSelectorMenu,
                    onDismissRequest = { showNoteSelectorMenu = false }
                ) {
                    allNotes.forEach { note ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(note.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${note.subject} • ${note.difficulty}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                viewModel.selectNote(note)
                                showNoteSelectorMenu = false
                            }
                        )
                    }
                    if (allNotes.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No notes found. Upload one first!") },
                            onClick = {
                                showNoteSelectorMenu = false
                                onNavigateToUpload()
                            }
                        )
                    }
                }
            }

            // AI Tool 0: PYQ Exam Solver (ChatGPT Style)
            item {
                AiToolBigCard(
                    title = "PYQ Exam Solver (ChatGPT Style)",
                    description = "Add past university questions (2M, 5M, 10M, 15M) and get step-by-step scoring answers, marking schemes, and examiner traps.",
                    icon = Icons.Filled.AutoAwesome,
                    color = EmeraldEasy,
                    tag = "ChatGPT Engine",
                    onClick = onNavigateToPyq
                )
            }

            // AI Tool 1: Practice Quiz & MCQs
            item {
                AiToolBigCard(
                    title = "Interactive MCQs & Practice Quiz",
                    description = "Take AI-generated multiple choice and short answer tests with immediate step-by-step explanations.",
                    icon = Icons.Outlined.Quiz,
                    color = IndigoPrimary,
                    tag = "High-Yield",
                    onClick = onNavigateToQuiz
                )
            }

            // AI Tool 2: Smart Flashcards
            item {
                AiToolBigCard(
                    title = "Smart Flashcard Flip Deck",
                    description = "Spaced-repetition flashcards for definitions, key equations, laws, and difficult concepts.",
                    icon = Icons.Outlined.ViewCarousel,
                    color = PurpleAccent,
                    tag = "Memory & Recall",
                    onClick = onNavigateToFlashcards
                )
            }

            // AI Tool 3: Mind Map
            item {
                AiToolBigCard(
                    title = "Hierarchical Concept Mind Map",
                    description = "Interactive tree graph linking core topics to sub-branches, laws, and atomic details.",
                    icon = Icons.Outlined.AccountTree,
                    color = TealAccent,
                    tag = "Visual Learners",
                    onClick = onNavigateToMindMap
                )
            }

            // AI Tool 4: AI Chat Tutor
            item {
                AiToolBigCard(
                    title = "AI Study Material Tutor",
                    description = "Ask questions grounded directly in your uploaded syllabus or document. No hallucinations.",
                    icon = Icons.Outlined.Chat,
                    color = Color(0xFFD97706),
                    tag = "24/7 Tutor",
                    onClick = onNavigateToChat
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        }
    }
}

@Composable
fun AiToolBigCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = tag,
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
