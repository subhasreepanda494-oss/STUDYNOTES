package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.StudyNote
import com.example.ui.theme.EmeraldEasy
import com.example.ui.theme.IndigoPrimary
import com.example.util.PdfExportManager
import com.example.util.PdfExportOptions
import com.example.util.PdfExportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfExportDialog(
    note: StudyNote,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var options by remember { mutableStateOf(PdfExportOptions()) }
    var isGenerating by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<PdfExportResult?>(null) }
    var downloadSaved by remember { mutableStateOf(false) }

    // Auto-generate on open with defaults
    LaunchedEffect(note.id) {
        isGenerating = true
        withContext(Dispatchers.IO) {
            try {
                val res = PdfExportManager.generatePdf(context, note, options)
                exportResult = res
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isGenerating = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 560.dp)
                .padding(vertical = 20.dp)
                .testTag("pdf_export_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = "PDF Export",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Study Notes to PDF",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Formatted for printing & offline study",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Generated Status Card
                if (exportResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${exportResult?.pageCount} Page(s) Document Ready",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Standard A4 • ${exportResult?.formattedSize} • High Resolution",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action Buttons
                    Text(
                        text = "ACTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Print / Save as PDF Button
                        Button(
                            onClick = {
                                exportResult?.let {
                                    PdfExportManager.printPdf(context, it)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_print_pdf")
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Print / PDF", fontWeight = FontWeight.Bold)
                        }

                        // Open / View PDF Button
                        FilledTonalButton(
                            onClick = {
                                exportResult?.let {
                                    PdfExportManager.openPdf(context, it)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_open_pdf")
                        ) {
                            Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Share Button
                        OutlinedButton(
                            onClick = {
                                exportResult?.let {
                                    PdfExportManager.sharePdf(context, it)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_share_pdf")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share")
                        }

                        // Save to Downloads Button
                        OutlinedButton(
                            onClick = {
                                exportResult?.let {
                                    val uri = PdfExportManager.saveToDownloads(context, it)
                                    if (uri != null) {
                                        downloadSaved = true
                                        Toast.makeText(context, "Saved to Downloads / StudyAI folder!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Saved PDF in app documents", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_download_pdf")
                        ) {
                            Icon(
                                imageVector = if (downloadSaved) Icons.Filled.Check else Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (downloadSaved) "Saved" else "Save File")
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Section Customization
                Text(
                    text = "CUSTOMIZE SECTIONS TO INCLUDE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PdfSectionToggle(
                        title = "Executive Summary",
                        subtitle = "Key takeaways and high-level synopsis",
                        checked = options.includeSummary,
                        onCheckedChange = {
                            options = options.copy(includeSummary = it)
                            regenerate(context, note, options, coroutineScope) { exportResult = it }
                        }
                    )
                    PdfSectionToggle(
                        title = "Key Concepts & Bullet Points",
                        subtitle = "Numbered high-yield highlights",
                        checked = options.includeKeyPoints,
                        onCheckedChange = {
                            options = options.copy(includeKeyPoints = it)
                            regenerate(context, note, options, coroutineScope) { exportResult = it }
                        }
                    )
                    PdfSectionToggle(
                        title = "Detailed Comprehensive Notes",
                        subtitle = "In-depth explanations and core subject coverage",
                        checked = options.includeDetailedNotes,
                        onCheckedChange = {
                            options = options.copy(includeDetailedNotes = it)
                            regenerate(context, note, options, coroutineScope) { exportResult = it }
                        }
                    )
                    PdfSectionToggle(
                        title = "Short Notes (Cheat Sheet)",
                        subtitle = "Rapid recall bulleted summaries",
                        checked = options.includeShortNotes,
                        onCheckedChange = {
                            options = options.copy(includeShortNotes = it)
                            regenerate(context, note, options, coroutineScope) { exportResult = it }
                        }
                    )
                    PdfSectionToggle(
                        title = "Quick Revision Notes",
                        subtitle = "Last-minute exam review points",
                        checked = options.includeQuickRevision,
                        onCheckedChange = {
                            options = options.copy(includeQuickRevision = it)
                            regenerate(context, note, options, coroutineScope) { exportResult = it }
                        }
                    )
                    PdfSectionToggle(
                        title = "Exam Strategy & Scoring Traps",
                        subtitle = "Common mistakes and examiner scoring secrets",
                        checked = options.includeExamNotes,
                        onCheckedChange = {
                            options = options.copy(includeExamNotes = it)
                            regenerate(context, note, options, coroutineScope) { exportResult = it }
                        }
                    )
                    PdfSectionToggle(
                        title = "Definitions & Formulas",
                        subtitle = "Formatted boxed equations and terminology",
                        checked = options.includeDefinitions && options.includeFormulas,
                        onCheckedChange = {
                            options = options.copy(includeDefinitions = it, includeFormulas = it)
                            regenerate(context, note, options, coroutineScope) { exportResult = it }
                        }
                    )
                    PdfSectionToggle(
                        title = "Practice Questions & MCQs",
                        subtitle = "Revision questions, answers, and multiple-choice quizzes",
                        checked = options.includePracticeQuestions,
                        onCheckedChange = {
                            options = options.copy(includePracticeQuestions = it)
                            regenerate(context, note, options, coroutineScope) { exportResult = it }
                        }
                    )
                }

                if (isGenerating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Formatting and rendering PDF pages...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun regenerate(
    context: android.content.Context,
    note: StudyNote,
    options: PdfExportOptions,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onResult: (PdfExportResult) -> Unit
) {
    coroutineScope.launch(Dispatchers.IO) {
        try {
            val res = PdfExportManager.generatePdf(context, note, options)
            withContext(Dispatchers.Main) {
                onResult(res)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
private fun PdfSectionToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            )
        }
    }
}
