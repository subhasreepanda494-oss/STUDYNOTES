package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyNote
import com.example.ui.navigation.BottomNavTab
import com.example.ui.navigation.Screen
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmeraldEasy
import com.example.ui.theme.RoseDifficulty
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun StudyBottomBar(
    currentScreen: Screen,
    onTabSelected: (BottomNavTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("study_bottom_bar")
    ) {
        BottomNavTab.values().forEach { tab ->
            val isSelected = when (tab) {
                BottomNavTab.HOME -> currentScreen == Screen.Home
                BottomNavTab.MY_NOTES -> currentScreen == Screen.MyNotes
                BottomNavTab.UPLOAD -> currentScreen == Screen.Upload
                BottomNavTab.AI_TOOLS -> currentScreen in listOf(Screen.AiTools, Screen.Quiz, Screen.Flashcards, Screen.MindMap, Screen.AiChat, Screen.PyqSolver)
                BottomNavTab.PROFILE -> currentScreen == Screen.Profile
            }

            val icon: ImageVector = when (tab) {
                BottomNavTab.HOME -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                BottomNavTab.MY_NOTES -> if (isSelected) Icons.Filled.MenuBook else Icons.Outlined.MenuBook
                BottomNavTab.UPLOAD -> if (isSelected) Icons.Filled.CloudUpload else Icons.Outlined.CloudUpload
                BottomNavTab.AI_TOOLS -> if (isSelected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
                BottomNavTab.PROFILE -> if (isSelected) Icons.Filled.Person else Icons.Outlined.Person
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
            )
        }
    }
}

@Composable
fun StudyNavigationRail(
    currentScreen: Screen,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "StudyAI",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "StudyAI",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = modifier.testTag("study_navigation_rail")
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        BottomNavTab.values().forEach { tab ->
            val isSelected = when (tab) {
                BottomNavTab.HOME -> currentScreen == Screen.Home
                BottomNavTab.MY_NOTES -> currentScreen == Screen.MyNotes
                BottomNavTab.UPLOAD -> currentScreen == Screen.Upload
                BottomNavTab.AI_TOOLS -> currentScreen in listOf(Screen.AiTools, Screen.Quiz, Screen.Flashcards, Screen.MindMap, Screen.AiChat, Screen.PyqSolver)
                BottomNavTab.PROFILE -> currentScreen == Screen.Profile
            }

            val icon: ImageVector = when (tab) {
                BottomNavTab.HOME -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                BottomNavTab.MY_NOTES -> if (isSelected) Icons.Filled.MenuBook else Icons.Outlined.MenuBook
                BottomNavTab.UPLOAD -> if (isSelected) Icons.Filled.CloudUpload else Icons.Outlined.CloudUpload
                BottomNavTab.AI_TOOLS -> if (isSelected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
                BottomNavTab.PROFILE -> if (isSelected) Icons.Filled.Person else Icons.Outlined.Person
            }

            NavigationRailItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
            )
        }
    }
}

@Composable
fun DifficultyBadge(difficulty: String) {
    val (bgColor, textColor) = when (difficulty.lowercase()) {
        "easy" -> Pair(EmeraldEasy.copy(alpha = 0.15f), EmeraldEasy)
        "advanced" -> Pair(RoseDifficulty.copy(alpha = 0.15f), RoseDifficulty)
        else -> Pair(AmberWarm.copy(alpha = 0.15f), AmberWarm)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = difficulty,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
fun SubjectBadge(subject: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = subject,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
fun FileTypeBadge(fileType: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = fileType.uppercase(),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun NoteCard(
    note: StudyNote,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onExportPdf: (() -> Unit)? = null
) {
    val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val formattedDate = dateFormatter.format(Date(note.updatedAt))

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SubjectBadge(subject = note.subject)
                    DifficultyBadge(difficulty = note.difficulty)
                    FileTypeBadge(fileType = note.fileType)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onExportPdf != null) {
                        IconButton(
                            onClick = onExportPdf,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_export_pdf_${note.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = "Export PDF",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("bookmark_button_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (note.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (note.isBookmarked) "Bookmarked" else "Bookmark",
                            tint = if (note.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (note.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.summary,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ToolChip(icon = Icons.Outlined.Quiz, label = "MCQ")
                    ToolChip(icon = Icons.Outlined.ViewCarousel, label = "Cards")
                    ToolChip(icon = Icons.Outlined.AccountTree, label = "Map")
                }
            }
        }
    }
}

@Composable
fun ToolChip(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
