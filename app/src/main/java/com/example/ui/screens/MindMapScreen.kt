package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ListAlt
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MindMapNode
import com.example.data.model.StudyNote
import com.example.ui.components.InteractiveMindMapCanvas
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmeraldEasy
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.MindMapLayoutMode
import com.example.util.MindMapTransformer
import com.example.util.VisualGraphNode

enum class MindMapViewMode {
    GRAPH,
    OUTLINE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapScreen(
    note: StudyNote?,
    viewModel: StudyViewModel? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // Active Note Selection (if viewModel is provided and notes exist)
    val allNotes by viewModel?.allNotes?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyList()) }
    var currentNoteState by remember(note) { mutableStateOf(note) }
    val effectiveNote = currentNoteState ?: allNotes.firstOrNull()

    // Screen States
    var viewMode by remember { mutableStateOf(MindMapViewMode.GRAPH) }
    var layoutMode by remember { mutableStateOf(MindMapLayoutMode.HORIZONTAL_TREE) }
    var forceAutoGenerate by remember { mutableStateOf(false) }

    var selectedNode by remember { mutableStateOf<VisualGraphNode?>(null) }
    var collapsedNodeIds by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchField by remember { mutableStateOf(false) }
    var showLayoutMenu by remember { mutableStateOf(false) }
    var showNoteSelectorMenu by remember { mutableStateOf(false) }

    // Transform StudyNote into Concept Graph
    val mindMapRoot = remember(effectiveNote, forceAutoGenerate) {
        if (effectiveNote != null) {
            MindMapTransformer.transform(effectiveNote, forceAutoGenerate = forceAutoGenerate)
        } else {
            MindMapNode(
                title = "Study Knowledge Map",
                tag = "Overview",
                description = "Select or upload study material to generate an interactive graph.",
                children = listOf(
                    MindMapNode(title = "Core Principles", tag = "Foundations"),
                    MindMapNode(title = "Key Vocabulary", tag = "Glossary"),
                    MindMapNode(title = "Exam Scoring Strategy", tag = "Tips")
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearchField) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search concepts in graph...", fontSize = 14.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mind_map_search_field")
                        )
                    } else {
                        Column(
                            modifier = Modifier.clickable(enabled = allNotes.size > 1) {
                                showNoteSelectorMenu = true
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Interactive Mind Map",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (allNotes.size > 1) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Switch Note",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = effectiveNote?.title ?: "Visual Knowledge Graph",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Note Selector Dropdown
                        DropdownMenu(
                            expanded = showNoteSelectorMenu,
                            onDismissRequest = { showNoteSelectorMenu = false }
                        ) {
                            Text(
                                text = "Select Study Material",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            allNotes.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            Text(item.subject, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        currentNoteState = item
                                        selectedNode = null
                                        showNoteSelectorMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(
                        onClick = {
                            showSearchField = !showSearchField
                            if (!showSearchField) searchQuery = ""
                        },
                        modifier = Modifier.testTag("btn_mind_map_search")
                    ) {
                        Icon(
                            imageVector = if (showSearchField) Icons.Filled.Close else Icons.Outlined.Search,
                            contentDescription = "Search"
                        )
                    }

                    // Layout Mode Selector Dropdown
                    Box {
                        IconButton(
                            onClick = { showLayoutMenu = true },
                            modifier = Modifier.testTag("btn_layout_mode_menu")
                        ) {
                            Icon(
                                imageVector = when (layoutMode) {
                                    MindMapLayoutMode.HORIZONTAL_TREE -> Icons.Outlined.AccountTree
                                    MindMapLayoutMode.RADIAL_STAR -> Icons.Filled.Hub
                                    MindMapLayoutMode.VERTICAL_TREE -> Icons.Outlined.DeviceHub
                                },
                                contentDescription = "Layout Mode"
                            )
                        }

                        DropdownMenu(
                            expanded = showLayoutMenu,
                            onDismissRequest = { showLayoutMenu = false }
                        ) {
                            Text(
                                text = "Graph Layout Geometry",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            MindMapLayoutMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (mode) {
                                                MindMapLayoutMode.HORIZONTAL_TREE -> Icons.Outlined.AccountTree
                                                MindMapLayoutMode.RADIAL_STAR -> Icons.Filled.Hub
                                                MindMapLayoutMode.VERTICAL_TREE -> Icons.Outlined.DeviceHub
                                            },
                                            contentDescription = null,
                                            tint = if (layoutMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = {
                                        if (layoutMode == mode) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = {
                                        layoutMode = mode
                                        showLayoutMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Re-extract Graph from Note") },
                                leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
                                onClick = {
                                    forceAutoGenerate = !forceAutoGenerate
                                    showLayoutMenu = false
                                    Toast.makeText(context, "Graph refreshed from note structure", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    // Toggle View Mode: Graph vs Outline
                    IconButton(
                        onClick = {
                            viewMode = if (viewMode == MindMapViewMode.GRAPH) MindMapViewMode.OUTLINE else MindMapViewMode.GRAPH
                        },
                        modifier = Modifier.testTag("btn_toggle_view_mode")
                    ) {
                        Icon(
                            imageVector = if (viewMode == MindMapViewMode.GRAPH) Icons.AutoMirrored.Outlined.ListAlt else Icons.Outlined.Hub,
                            contentDescription = if (viewMode == MindMapViewMode.GRAPH) "Switch to Outline" else "Switch to Graph"
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("mind_map_screen")
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (viewMode) {
                MindMapViewMode.GRAPH -> {
                    // Main Interactive Zoomable Canvas
                    InteractiveMindMapCanvas(
                        rootNode = mindMapRoot,
                        layoutMode = layoutMode,
                        selectedNodeId = selectedNode?.id,
                        searchQuery = searchQuery,
                        collapsedNodeIds = collapsedNodeIds,
                        onNodeSelected = { node ->
                            selectedNode = if (selectedNode?.id == node.id) null else node
                        },
                        onToggleExpand = { nodeId ->
                            collapsedNodeIds = if (collapsedNodeIds.contains(nodeId)) {
                                collapsedNodeIds - nodeId
                            } else {
                                collapsedNodeIds + nodeId
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Node Inspection Detail Bottom Card
                    AnimatedVisibility(
                        visible = selectedNode != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .widthIn(max = 600.dp)
                    ) {
                        selectedNode?.let { node ->
                            NodeDetailInspectionCard(
                                node = node,
                                onCollapseToggle = {
                                    collapsedNodeIds = if (collapsedNodeIds.contains(node.id)) {
                                        collapsedNodeIds - node.id
                                    } else {
                                        collapsedNodeIds + node.id
                                    }
                                },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Concept", "${node.title}\n${node.description}")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied concept to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onClose = { selectedNode = null }
                            )
                        }
                    }
                }

                MindMapViewMode.OUTLINE -> {
                    // Linear Outline View
                    MindMapOutlineView(
                        root = mindMapRoot,
                        searchQuery = searchQuery,
                        onNodeClick = { node ->
                            // Switch back to graph view focusing on clicked concept
                            selectedNode = VisualGraphNode(
                                id = node.id,
                                title = node.title,
                                tag = node.tag,
                                description = node.description,
                                depth = 1,
                                branchIndex = 0
                            )
                            viewMode = MindMapViewMode.GRAPH
                        }
                    )
                }
            }
        }
    }
}

/**
 * Inspection preview card for a tapped node in the interactive mind map.
 */
@Composable
private fun NodeDetailInspectionCard(
    node: VisualGraphNode,
    onCollapseToggle: () -> Unit,
    onCopy: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("node_inspection_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(node.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = node.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = when (node.depth) {
                                0 -> "CENTRAL TOPIC"
                                1 -> "PRIMARY BRANCH"
                                else -> "SUBTOPIC CONCEPT"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = node.color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (node.tag.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = node.tag,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = node.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            if (node.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = node.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    ),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (node.childCount > 0) {
                    FilledTonalButton(
                        onClick = onCollapseToggle,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (node.isExpanded) Icons.Filled.RemoveCircleOutline else Icons.Filled.AddCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (node.isExpanded) "Collapse (${node.childCount})" else "Expand (${node.childCount})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onCopy,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Hierarchical outline view of the study mind map tree.
 */
@Composable
private fun MindMapOutlineView(
    root: MindMapNode,
    searchQuery: String,
    onNodeClick: (MindMapNode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Central Theme Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Hub,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CENTRAL CONCEPT GRAPH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.82f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = root.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (root.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = root.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.9f)
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Text(
            text = "Structured Concept Branches (${root.children.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        // Render each branch
        root.children.forEachIndexed { index, branch ->
            val matchesSearch = searchQuery.isBlank() ||
                branch.title.contains(searchQuery, ignoreCase = true) ||
                branch.children.any { it.title.contains(searchQuery, ignoreCase = true) }

            if (matchesSearch) {
                MindMapBranchCard(
                    branchIndex = index + 1,
                    branch = branch,
                    searchQuery = searchQuery,
                    onNodeClick = onNodeClick
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MindMapBranchCard(
    branchIndex: Int,
    branch: MindMapNode,
    searchQuery: String = "",
    onNodeClick: ((MindMapNode) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(true) }

    val accentColor = MindMapTransformer.BranchColors[(branchIndex - 1) % MindMapTransformer.BranchColors.size]

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Branch Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = branchIndex.toString(),
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = branch.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (branch.tag.isNotBlank()) {
                            Text(
                                text = branch.tag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = accentColor
                                )
                            )
                        }
                    }
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle Branch"
                    )
                }
            }

            if (branch.description.isNotBlank() && isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = branch.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(start = 44.dp)
                )
            }

            // Expandable Children / Leaf Nodes
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    branch.children.forEach { child ->
                        val isChildMatched = searchQuery.isNotBlank() && child.title.contains(searchQuery, ignoreCase = true)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isChildMatched) AmberWarm.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = if (isChildMatched) 1.5.dp else 1.dp,
                                    color = if (isChildMatched) AmberWarm else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onNodeClick?.invoke(child) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = child.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                if (child.description.isNotBlank()) {
                                    Text(
                                        text = child.description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (child.tag.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = child.tag,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
