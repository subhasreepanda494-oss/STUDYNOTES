package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.FileContentExtractor
import com.example.data.ai.UploadedFileInfo
import com.example.ui.components.FileTypeBadge
import com.example.ui.components.StudyTopBar
import com.example.ui.theme.EmeraldEasy
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseDifficulty
import com.example.ui.viewmodel.StudyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadScreen(
    viewModel: StudyViewModel,
    onStartProcessing: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectedFile by viewModel.selectedUploadFile.collectAsStateWithLifecycle()
    val selectedSubject by viewModel.selectedSubject.collectAsStateWithLifecycle()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    var manualText by remember { mutableStateOf("") }
    var uploadMode by remember { mutableStateOf(0) } // 0: File Upload, 1: Paste Text, 2: Sample Study Packs
    var uploadProgress by remember { mutableStateOf(0f) }
    var isSimulatingUpload by remember { mutableStateOf(false) }

    val subjects = listOf(
        "CN",
        "SE",
        "Math for Data Science",
        "IKS",
        "Computer Science",
        "Mathematics",
        "Biology",
        "Physics",
        "Chemistry",
        "History",
        "Literature",
        "General"
    )
    val difficulties = listOf("Easy", "Medium", "Advanced")
    val languages = listOf("English", "Spanish", "French", "German", "Hindi")

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isSimulatingUpload = true
                uploadProgress = 0.1f
                delay(200)
                uploadProgress = 0.45f

                val (fileName, fileSize) = getFileMetadata(context, uri)
                val ext = fileName.substringAfterLast(".", "TXT").uppercase()
                val extractedContent = FileContentExtractor.extractTextFromUri(context, uri, fileName)

                uploadProgress = 0.85f
                delay(200)
                uploadProgress = 1f
                isSimulatingUpload = false

                viewModel.setSelectedUploadFile(
                    UploadedFileInfo(
                        uri = uri,
                        fileName = fileName,
                        fileType = ext,
                        sizeBytes = fileSize,
                        contentPreview = extractedContent
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            StudyTopBar(
                title = "Upload Study Material",
                subtitle = "Supports PDF, DOCX, PPTX, TXT, JPG, PNG"
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("upload_screen")
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 900.dp)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Mode Selector: Upload File / Paste Text / Pick Sample
            TabRow(
                selectedTabIndex = uploadMode,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = uploadMode == 0,
                    onClick = { uploadMode = 0 },
                    text = { Text("Upload File", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                )
                Tab(
                    selected = uploadMode == 1,
                    onClick = { uploadMode = 1 },
                    text = { Text("Paste Text", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                )
                Tab(
                    selected = uploadMode == 2,
                    onClick = { uploadMode = 2 },
                    text = { Text("Sample Packs", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                )
            }

            // Mode 0: File Upload Zone
            if (uploadMode == 0) {
                Card(
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                "text/plain",
                                "image/*"
                            )
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .testTag("upload_dropzone")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Tap or Select Study Material",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "PDF, DOCX, PPTX, TXT, JPG, PNG",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        FilledTonalButton(
                            onClick = {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                        "text/plain",
                                        "image/*"
                                    )
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Browse Device Files")
                        }
                    }
                }
            }

            // Mode 1: Paste Text
            if (uploadMode == 1) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Paste Lecture Notes or Syllabus Text",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualText,
                            onValueChange = {
                                manualText = it
                                if (it.isNotBlank()) {
                                    viewModel.setSelectedUploadFile(
                                        UploadedFileInfo(
                                            uri = null,
                                            fileName = "Pasted_Notes.txt",
                                            fileType = "TXT",
                                            sizeBytes = it.length.toLong(),
                                            contentPreview = it
                                        )
                                    )
                                }
                            },
                            placeholder = { Text("Paste lecture notes, book chapters, exam summaries, or typed text...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("upload_paste_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Mode 2: Sample Study Packs
            if (uploadMode == 2) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Choose a Pre-formatted Academic Sample",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FileContentExtractor.SAMPLE_DOCUMENTS.forEach { sample ->
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
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FileTypeBadge(fileType = sample.fileType)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sample.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${sample.subject} • ${sample.difficulty}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Filled.CheckCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // Upload Progress Simulation
            AnimatedVisibility(visible = isSimulatingUpload) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Reading & Validating Document...", fontWeight = FontWeight.SemiBold)
                            Text("${(uploadProgress * 100).toInt()}%", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }

            // File Selected Confirmation Card
            if (selectedFile != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldEasy.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = EmeraldEasy,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedFile?.fileName ?: "Uploaded Material",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${selectedFile?.fileType} • ${(selectedFile?.sizeBytes ?: 0) / 1024 + 1} KB • Validated",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        IconButton(onClick = { viewModel.setSelectedUploadFile(null) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Subject Selector
            Column {
                Text(
                    text = "Subject Category",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.forEach { subject ->
                        val isSelected = selectedSubject == subject
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSelectedSubject(subject) },
                            label = { Text(subject) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            // Difficulty Selector (Easy, Medium, Advanced)
            Column {
                Text(
                    text = "Difficulty Level",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    difficulties.forEach { diff ->
                        val isSelected = selectedDifficulty == diff
                        Card(
                            onClick = { viewModel.setSelectedDifficulty(diff) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = diff,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Language Selector
            Column {
                Text(
                    text = "Generated Notes Language",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languages.forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSelectedLanguage(lang) },
                            label = { Text(lang) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Big CTA: Generate AI Notes
            val canGenerate = selectedFile != null || manualText.isNotBlank()
            Button(
                onClick = {
                    if (selectedFile == null && manualText.isNotBlank()) {
                        viewModel.setSelectedUploadFile(
                            UploadedFileInfo(
                                uri = null,
                                fileName = "Lecture_Notes.txt",
                                fileType = "TXT",
                                sizeBytes = manualText.length.toLong(),
                                contentPreview = manualText
                            )
                        )
                    }
                    onStartProcessing()
                },
                enabled = canGenerate,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("upload_generate_button")
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate AI Study Kit",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
        }
    }
}

private fun getFileMetadata(context: Context, uri: Uri): Pair<String, Long> {
    var name = "uploaded_study_material.txt"
    var size = 1024L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
    } catch (_: Exception) {}
    return Pair(name, size)
}
