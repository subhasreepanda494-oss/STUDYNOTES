package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.FileContentExtractor
import com.example.data.ai.UploadedFileInfo
import com.example.data.model.StudyNote
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.FileTypeBadge
import com.example.ui.components.NoteCard
import com.example.ui.components.SubjectBadge
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.StudyViewModel

@Composable
fun HomeScreen(
    viewModel: StudyViewModel,
    onNavigateToUpload: () -> Unit,
    onNavigateToNoteDetail: (StudyNote) -> Unit,
    onNavigateToMyNotes: () -> Unit,
    onNavigateToAiTools: () -> Unit,
    onNavigateToPyq: () -> Unit
) {
    val recentNotes by viewModel.recentNotes.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val bookmarkedNotes by viewModel.bookmarkedNotes.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1100.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
        // Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF312E81),
                                IndigoPrimary
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Welcome back, $userName 👋",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "“Upload. Learn. Understand. Ace.”",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFC7D2FE),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Stats Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCounterCard(
                            title = "Generated Notes",
                            value = allNotes.size.toString(),
                            icon = Icons.Outlined.MenuBook,
                            modifier = Modifier.weight(1f)
                        )
                        StatCounterCard(
                            title = "Bookmarked",
                            value = bookmarkedNotes.size.toString(),
                            icon = Icons.Outlined.Bookmark,
                            modifier = Modifier.weight(1f)
                        )
                        StatCounterCard(
                            title = "AI Tools Ready",
                            value = "5",
                            icon = Icons.Outlined.AutoAwesome,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Primary Action: Upload Study Material Banner
        item {
            Card(
                onClick = onNavigateToUpload,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .testTag("home_upload_banner")
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Upload Study Material",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "PDF, DOCX, PPTX, TXT, JPG, PNG",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Upload",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Quick AI Tools Row
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text(
                    text = "AI Study Tools",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolShortcutCard(
                        title = "MCQ Quiz",
                        icon = Icons.Outlined.Quiz,
                        color = IndigoPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val target = recentNotes.firstOrNull() ?: allNotes.firstOrNull()
                            if (target != null) {
                                viewModel.selectNote(target)
                            }
                            onNavigateToAiTools()
                        }
                    )
                    ToolShortcutCard(
                        title = "Flashcards",
                        icon = Icons.Outlined.ViewCarousel,
                        color = PurpleAccent,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val target = recentNotes.firstOrNull() ?: allNotes.firstOrNull()
                            if (target != null) {
                                viewModel.selectNote(target)
                            }
                            onNavigateToAiTools()
                        }
                    )
                    ToolShortcutCard(
                        title = "Mind Map",
                        icon = Icons.Outlined.AccountTree,
                        color = TealAccent,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val target = recentNotes.firstOrNull() ?: allNotes.firstOrNull()
                            if (target != null) {
                                viewModel.selectNote(target)
                            }
                            onNavigateToAiTools()
                        }
                    )
                    ToolShortcutCard(
                        title = "AI Chat",
                        icon = Icons.Outlined.Chat,
                        color = Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val target = recentNotes.firstOrNull() ?: allNotes.firstOrNull()
                            if (target != null) {
                                viewModel.selectNote(target)
                            }
                            onNavigateToAiTools()
                        }
                    )
                }
            }
        }

        // PYQ Exam Question Solver Banner
        item {
            Card(
                onClick = onNavigateToPyq,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFECFDF5)
                ),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .testTag("home_pyq_banner")
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
                            .background(Color(0xFF059669))
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PYQ Exam Solver",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFD1FAE5)
                            ) {
                                Text(
                                    text = "ChatGPT Style",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Add university questions & get step-by-step model answers with marking schemes.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF047857)
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Open PYQ",
                        tint = Color(0xFF059669)
                    )
                }
            }
        }

        // Pre-loaded Quick Study Packs (for instant 1-tap testing)
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Study Samples",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "Instant 1-Tap",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(end = 12.dp)
                ) {
                    items(FileContentExtractor.SAMPLE_DOCUMENTS) { sample ->
                        Card(
                            onClick = {
                                viewModel.setSelectedUploadFile(
                                    UploadedFileInfo(
                                        uri = null,
                                        fileName = "${sample.title.lowercase().replace(" ", "_")}.${sample.fileType.lowercase()}",
                                        fileType = sample.fileType,
                                        sizeBytes = sample.content.length.toLong(),
                                        contentPreview = sample.content
                                    )
                                )
                                viewModel.setSelectedSubject(sample.subject)
                                viewModel.setSelectedDifficulty(sample.difficulty)
                                onNavigateToUpload()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.width(220.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    SubjectBadge(subject = sample.subject)
                                    FileTypeBadge(fileType = sample.fileType)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = sample.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Load & Generate",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Notes Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Study Notes",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                TextButton(onClick = onNavigateToMyNotes) {
                    Text(
                        text = "View All",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (recentNotes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes generated yet.\nUpload your first document above!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            }
        } else {
            items(recentNotes) { note ->
                NoteCard(
                    note = note,
                    onClick = {
                        viewModel.selectNote(note)
                        onNavigateToNoteDetail(note)
                    },
                    onBookmarkToggle = { viewModel.toggleBookmark(note) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
        }
    }
}

@Composable
fun StatCounterCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFC7D2FE),
                    fontSize = 9.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ToolShortcutCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 6.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
