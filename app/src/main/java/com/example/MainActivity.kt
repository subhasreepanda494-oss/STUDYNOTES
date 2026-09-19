package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.StudyBottomBar
import com.example.ui.components.StudyNavigationRail
import com.example.ui.navigation.BottomNavTab
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val studyViewModel: StudyViewModel = viewModel()
            val isDarkMode by studyViewModel.isDarkMode.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkMode) {
                MainAppContent(viewModel = studyViewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: StudyViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var previousScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val currentNote by viewModel.currentNote.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    fun navigateTo(screen: Screen) {
        previousScreen = currentScreen
        currentScreen = screen
    }

    // Handle System Back button
    BackHandler(enabled = currentScreen != Screen.Home && currentScreen != Screen.Splash && currentScreen != Screen.Auth) {
        when (currentScreen) {
            Screen.NoteDetail -> navigateTo(previousScreen.takeIf { it != Screen.Processing } ?: Screen.Home)
            Screen.Quiz, Screen.Flashcards, Screen.MindMap, Screen.AiChat, Screen.PyqSolver -> {
                if (currentNote != null && currentScreen != Screen.PyqSolver) navigateTo(Screen.NoteDetail) else navigateTo(Screen.AiTools)
            }
            Screen.Processing -> navigateTo(Screen.Upload)
            Screen.MyNotes, Screen.Upload, Screen.AiTools, Screen.Profile -> navigateTo(Screen.Home)
            else -> navigateTo(Screen.Home)
        }
    }

    val showNav = currentScreen in listOf(
        Screen.Home,
        Screen.MyNotes,
        Screen.Upload,
        Screen.AiTools,
        Screen.Profile
    )

    @Composable
    fun AppScreenContent(targetScreen: Screen) {
        when (targetScreen) {
            Screen.Splash -> {
                SplashScreen(
                    onSplashFinished = {
                        if (isLoggedIn) {
                            navigateTo(Screen.Home)
                        } else {
                            navigateTo(Screen.Auth)
                        }
                    }
                )
            }
            Screen.Auth -> {
                AuthScreen(
                    onAuthSuccess = { name, email ->
                        viewModel.updateProfile(name, email)
                        viewModel.setLoggedIn(true)
                        navigateTo(Screen.Home)
                    },
                    onContinueGuest = {
                        viewModel.setLoggedIn(true)
                        navigateTo(Screen.Home)
                    }
                )
            }
            Screen.Home -> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToUpload = { navigateTo(Screen.Upload) },
                    onNavigateToNoteDetail = { note ->
                        viewModel.selectNote(note)
                        navigateTo(Screen.NoteDetail)
                    },
                    onNavigateToMyNotes = { navigateTo(Screen.MyNotes) },
                    onNavigateToAiTools = { navigateTo(Screen.AiTools) },
                    onNavigateToPyq = { navigateTo(Screen.PyqSolver) }
                )
            }
            Screen.MyNotes -> {
                MyNotesScreen(
                    viewModel = viewModel,
                    onNoteClick = { note ->
                        viewModel.selectNote(note)
                        navigateTo(Screen.NoteDetail)
                    },
                    onUploadClick = { navigateTo(Screen.Upload) }
                )
            }
            Screen.Upload -> {
                UploadScreen(
                    viewModel = viewModel,
                    onStartProcessing = {
                        navigateTo(Screen.Processing)
                    }
                )
            }
            Screen.Processing -> {
                AiProcessingScreen(
                    viewModel = viewModel,
                    onProcessingFinished = { savedNote ->
                        viewModel.selectNote(savedNote)
                        navigateTo(Screen.NoteDetail)
                    }
                )
            }
            Screen.NoteDetail -> {
                currentNote?.let { note ->
                    NoteDetailScreen(
                        note = note,
                        viewModel = viewModel,
                        onBackClick = { navigateTo(previousScreen.takeIf { it != Screen.Processing } ?: Screen.Home) },
                        onNavigateToQuiz = { navigateTo(Screen.Quiz) },
                        onNavigateToFlashcards = { navigateTo(Screen.Flashcards) },
                        onNavigateToMindMap = { navigateTo(Screen.MindMap) },
                        onNavigateToChat = { navigateTo(Screen.AiChat) },
                        onNavigateToPyq = { navigateTo(Screen.PyqSolver) }
                    )
                } ?: run {
                    navigateTo(Screen.Home)
                }
            }
            Screen.Quiz -> {
                QuizScreen(
                    note = currentNote,
                    viewModel = viewModel,
                    onBackClick = {
                        if (currentNote != null) navigateTo(Screen.NoteDetail) else navigateTo(Screen.AiTools)
                    }
                )
            }
            Screen.Flashcards -> {
                FlashcardsScreen(
                    note = currentNote,
                    viewModel = viewModel,
                    onBackClick = {
                        if (currentNote != null) navigateTo(Screen.NoteDetail) else navigateTo(Screen.AiTools)
                    }
                )
            }
            Screen.MindMap -> {
                MindMapScreen(
                    note = currentNote,
                    viewModel = viewModel,
                    onBackClick = {
                        if (currentNote != null) navigateTo(Screen.NoteDetail) else navigateTo(Screen.AiTools)
                    }
                )
            }
            Screen.AiChat -> {
                AiChatScreen(
                    note = currentNote,
                    viewModel = viewModel,
                    onBackClick = {
                        if (currentNote != null) navigateTo(Screen.NoteDetail) else navigateTo(Screen.AiTools)
                    }
                )
            }
            Screen.PyqSolver -> {
                PyqSolverScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        navigateTo(previousScreen.takeIf { it != Screen.PyqSolver } ?: Screen.AiTools)
                    }
                )
            }
            Screen.AiTools -> {
                AiToolsScreen(
                    viewModel = viewModel,
                    onNavigateToQuiz = { navigateTo(Screen.Quiz) },
                    onNavigateToFlashcards = { navigateTo(Screen.Flashcards) },
                    onNavigateToMindMap = { navigateTo(Screen.MindMap) },
                    onNavigateToChat = { navigateTo(Screen.AiChat) },
                    onNavigateToUpload = { navigateTo(Screen.Upload) },
                    onNavigateToPyq = { navigateTo(Screen.PyqSolver) }
                )
            }
            Screen.Profile -> {
                ProfileScreen(
                    viewModel = viewModel,
                    onSignOut = {
                        viewModel.setLoggedIn(false)
                        navigateTo(Screen.Auth)
                    }
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 600.dp

        if (isExpanded) {
            // Desktop, Laptop, and Tablet Layout
            Row(modifier = Modifier.fillMaxSize()) {
                if (showNav) {
                    StudyNavigationRail(
                        currentScreen = currentScreen,
                        onTabSelected = { tab ->
                            when (tab) {
                                BottomNavTab.HOME -> navigateTo(Screen.Home)
                                BottomNavTab.MY_NOTES -> navigateTo(Screen.MyNotes)
                                BottomNavTab.UPLOAD -> navigateTo(Screen.Upload)
                                BottomNavTab.AI_TOOLS -> navigateTo(Screen.AiTools)
                                BottomNavTab.PROFILE -> navigateTo(Screen.Profile)
                            }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 1400.dp)
                    ) {
                        Crossfade(targetState = currentScreen, label = "desktop_screen_transition") { screen ->
                            AppScreenContent(screen)
                        }
                    }
                }
            }
        } else {
            // Mobile Handheld Layout
            Scaffold(
                bottomBar = {
                    if (showNav) {
                        StudyBottomBar(
                            currentScreen = currentScreen,
                            onTabSelected = { tab ->
                                when (tab) {
                                    BottomNavTab.HOME -> navigateTo(Screen.Home)
                                    BottomNavTab.MY_NOTES -> navigateTo(Screen.MyNotes)
                                    BottomNavTab.UPLOAD -> navigateTo(Screen.Upload)
                                    BottomNavTab.AI_TOOLS -> navigateTo(Screen.AiTools)
                                    BottomNavTab.PROFILE -> navigateTo(Screen.Profile)
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Crossfade(targetState = currentScreen, label = "mobile_screen_transition") { screen ->
                        AppScreenContent(screen)
                    }
                }
            }
        }
    }
}
