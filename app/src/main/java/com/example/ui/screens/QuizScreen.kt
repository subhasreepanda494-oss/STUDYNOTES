package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.McqItem
import com.example.data.model.ShortQuestionItem
import com.example.data.model.StudyJsonParser
import com.example.data.model.StudyNote
import com.example.ui.theme.EmeraldEasy
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseDifficulty
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    note: StudyNote?,
    viewModel: StudyViewModel,
    onBackClick: () -> Unit
) {
    val userAnswers by viewModel.userAnswers.collectAsStateWithLifecycle()
    val quizSubmitted by viewModel.quizSubmitted.collectAsStateWithLifecycle()

    var quizMode by remember { mutableStateOf(0) } // 0: MCQs, 1: Short Questions
    val mcqs = remember(note?.mcqsJson) {
        if (note != null) StudyJsonParser.parseMcqs(note.mcqsJson) else emptyList()
    }
    val shortQuestions = remember(note?.shortQuestionsJson) {
        if (note != null) StudyJsonParser.parseShortQuestions(note.shortQuestionsJson) else emptyList()
    }

    val correctCount = mcqs.indices.count { index ->
        userAnswers[index] == mcqs[index].correctIndex
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Practice Quiz",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = note?.title ?: "Study Material",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetQuiz() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Restart Quiz")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("quiz_screen")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mode Selector: MCQs vs Short Questions
            TabRow(
                selectedTabIndex = quizMode,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = quizMode == 0,
                    onClick = { quizMode = 0 },
                    text = { Text("MCQs (${mcqs.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = quizMode == 1,
                    onClick = { quizMode = 1 },
                    text = { Text("Short Questions (${shortQuestions.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (mcqs.isEmpty() && shortQuestions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No questions available for this note.\nGenerate study notes first!",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else if (quizMode == 0) {
                // MCQs Mode
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Score Card Banner
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (quizSubmitted) {
                                    if (correctCount >= mcqs.size / 2) EmeraldEasy.copy(alpha = 0.15f) else RoseDifficulty.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (quizSubmitted) "Quiz Completed 🎉" else "Interactive Practice",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (quizSubmitted) "Score: $correctCount / ${mcqs.size} correct" else "Answered: ${userAnswers.size} / ${mcqs.size}",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }

                                if (!quizSubmitted && userAnswers.size == mcqs.size) {
                                    Button(
                                        onClick = { viewModel.submitQuiz() },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Submit Quiz")
                                    }
                                }
                            }
                        }
                    }

                    // Question Cards
                    itemsIndexed(mcqs) { qIndex, mcq ->
                        val selectedOpt = userAnswers[qIndex]
                        val isAnswered = selectedOpt != null

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Question ${qIndex + 1}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    if (isAnswered) {
                                        val isCorrect = selectedOpt == mcq.correctIndex
                                        Text(
                                            text = if (isCorrect) "✓ Correct" else "✗ Incorrect",
                                            color = if (isCorrect) EmeraldEasy else RoseDifficulty,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = mcq.question,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Options
                                mcq.options.forEachIndexed { optIndex, optionText ->
                                    val isOptionSelected = selectedOpt == optIndex
                                    val isCorrectOption = optIndex == mcq.correctIndex

                                    val optionBgColor = when {
                                        !isAnswered -> if (isOptionSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        isCorrectOption -> EmeraldEasy.copy(alpha = 0.2f)
                                        isOptionSelected -> RoseDifficulty.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    }

                                    val optionBorderColor = when {
                                        !isAnswered && isOptionSelected -> MaterialTheme.colorScheme.primary
                                        isAnswered && isCorrectOption -> EmeraldEasy
                                        isAnswered && isOptionSelected -> RoseDifficulty
                                        else -> Color.Transparent
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(optionBgColor)
                                            .border(1.dp, optionBorderColor, RoundedCornerShape(10.dp))
                                            .clickable(enabled = !quizSubmitted) {
                                                viewModel.selectAnswer(qIndex, optIndex)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val optionLetter = ('A' + optIndex).toString()
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isOptionSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                            ) {
                                                Text(
                                                    text = optionLetter,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOptionSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = optionText,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isOptionSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                        }
                                    }
                                }

                                // Explanation Card
                                AnimatedVisibility(visible = isAnswered) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "💡 Explanation:",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = mcq.explanation,
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Short Questions Mode
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(shortQuestions) { index, item ->
                        ShortQuestionCard(index = index + 1, item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun ShortQuestionCard(index: Int, item: ShortQuestionItem) {
    var isAnswerRevealed by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Question $index",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.question,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!isAnswerRevealed) {
                OutlinedButton(
                    onClick = { isAnswerRevealed = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reveal Answer & Rationale")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Answer:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.answer, style = MaterialTheme.typography.bodyMedium)

                    if (item.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Academic Explanation:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.explanation,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }
    }
}
