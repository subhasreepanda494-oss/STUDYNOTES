package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StudyNote
import com.example.ui.components.NoteCard
import com.example.ui.components.PdfExportDialog
import com.example.ui.components.StudyTopBar
import com.example.ui.viewmodel.StudyViewModel

@Composable
fun MyNotesScreen(
    viewModel: StudyViewModel,
    onNoteClick: (StudyNote) -> Unit,
    onUploadClick: () -> Unit
) {
    val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedSubjectFilter by viewModel.selectedSubjectFilter.collectAsStateWithLifecycle()
    val bookmarkedNotes by viewModel.bookmarkedNotes.collectAsStateWithLifecycle()

    var filterTab by remember { mutableStateOf(0) } // 0: All Notes, 1: Bookmarks
    var noteForPdfExport by remember { mutableStateOf<StudyNote?>(null) }
    val subjects = listOf("All", "CN", "SE", "Math for Data Science", "IKS", "Computer Science", "Mathematics", "Biology", "Physics", "Chemistry", "History")

    val displayNotes = if (filterTab == 1) {
        notes.filter { it.isBookmarked }
    } else {
        notes
    }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "My Study Notes",
                subtitle = "${displayNotes.size} saved materials"
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("my_notes_screen")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by title, subject, formula, or concept...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("notes_search_input")
            )

            // Tabs: All Notes | Bookmarks
            TabRow(
                selectedTabIndex = filterTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = filterTab == 0,
                    onClick = { filterTab = 0 },
                    text = { Text("All Notes (${notes.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = filterTab == 1,
                    onClick = { filterTab = 1 },
                    text = { Text("Bookmarked (${bookmarkedNotes.size})", fontWeight = FontWeight.Bold) }
                )
            }

            // Subject Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subjects) { subject ->
                    val isSelected = selectedSubjectFilter == subject
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSubjectFilter(subject) },
                        label = { Text(subject, fontSize = 12.sp) }
                    )
                }
            }

            // List of Notes
            if (displayNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (filterTab == 1) "No bookmarked notes yet." else "No notes found matching your search.",
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onUploadClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Study Document")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 340.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note) },
                            onBookmarkToggle = { viewModel.toggleBookmark(note) },
                            onExportPdf = { noteForPdfExport = note }
                        )
                    }
                }
            }
        }

        if (noteForPdfExport != null) {
            PdfExportDialog(
                note = noteForPdfExport!!,
                onDismiss = { noteForPdfExport = null }
            )
        }
    }
}
