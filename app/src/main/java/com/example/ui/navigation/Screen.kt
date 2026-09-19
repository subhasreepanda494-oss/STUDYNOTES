package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Home : Screen("home")
    object MyNotes : Screen("my_notes")
    object Upload : Screen("upload")
    object AiTools : Screen("ai_tools")
    object Profile : Screen("profile")
    object Processing : Screen("processing")
    object NoteDetail : Screen("note_detail")
    object Quiz : Screen("quiz")
    object Flashcards : Screen("flashcards")
    object MindMap : Screen("mind_map")
    object AiChat : Screen("ai_chat")
    object PyqSolver : Screen("pyq_solver")
}

enum class BottomNavTab(val title: String, val screen: Screen) {
    HOME("Home", Screen.Home),
    MY_NOTES("My Notes", Screen.MyNotes),
    UPLOAD("Upload", Screen.Upload),
    AI_TOOLS("AI Tools", Screen.AiTools),
    PROFILE("Profile", Screen.Profile)
}
