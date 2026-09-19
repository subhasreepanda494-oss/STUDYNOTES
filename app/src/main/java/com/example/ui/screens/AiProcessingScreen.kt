package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StudyNote
import com.example.ui.theme.EmeraldEasy
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.StudyViewModel

@Composable
fun AiProcessingScreen(
    viewModel: StudyViewModel,
    onProcessingFinished: (StudyNote) -> Unit
) {
    val processingState by viewModel.processingState.collectAsStateWithLifecycle()

    val steps = listOf(
        "Reading complete study document",
        "Analyzing key concepts & structure",
        "Organizing topics & exam priorities",
        "Generating notes, MCQs & mind maps"
    )

    val stepIcons = listOf(
        Icons.Filled.Description,
        Icons.Filled.Analytics,
        Icons.Filled.Category,
        Icons.Filled.AutoAwesome
    )

    // Pulsing animation for the AI Brain Core
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        viewModel.startProcessing { note ->
            onProcessingFinished(note)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B),
                        Color(0xFF312E81),
                        Color(0xFF0F172A)
                    )
                )
            )
            .testTag("ai_processing_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Brain / AI Core
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(IndigoPrimary, PurpleAccent, TealAccent)
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = "AI Processing",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "StudyAI Engine",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = processingState.currentStepName.ifBlank { "Synthesizing full document..." },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFC7D2FE),
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Step by Step Progress Indicators
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    steps.forEachIndexed { index, stepText ->
                        val isFinished = processingState.currentStepIndex > index
                        val isCurrent = processingState.currentStepIndex == index

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isFinished -> EmeraldEasy
                                            isCurrent -> IndigoPrimary
                                            else -> Color.White.copy(alpha = 0.15f)
                                        }
                                    )
                            ) {
                                if (isFinished) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else if (isCurrent) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = stepIcons[index],
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Step ${index + 1}: ${stepText.substringBefore(" ")}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isCurrent || isFinished) TealAccent else Color.White.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = stepText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isCurrent || isFinished) Color.White else Color.White.copy(alpha = 0.5f),
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // AI Rule Banner: reads entire document, no skipping
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = TealAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Full document deep read in progress • Zero hallucination",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFC7D2FE),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
