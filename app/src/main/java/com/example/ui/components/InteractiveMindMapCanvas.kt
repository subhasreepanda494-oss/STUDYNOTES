package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MindMapNode
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmeraldEasy
import com.example.ui.theme.IndigoPrimary
import com.example.util.MindMapLayoutMode
import com.example.util.MindMapTransformer
import com.example.util.VisualGraphEdge
import com.example.util.VisualGraphLayout
import com.example.util.VisualGraphNode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun InteractiveMindMapCanvas(
    rootNode: MindMapNode,
    layoutMode: MindMapLayoutMode,
    selectedNodeId: String?,
    searchQuery: String,
    collapsedNodeIds: Set<String>,
    onNodeSelected: (VisualGraphNode) -> Unit,
    onToggleExpand: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFitScreenRequested: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Graph Layout Computation
    val graphLayout = remember(rootNode, layoutMode, collapsedNodeIds, selectedNodeId, searchQuery) {
        MindMapTransformer.layoutGraph(
            root = rootNode,
            mode = layoutMode,
            collapsedIds = collapsedNodeIds,
            selectedNodeId = selectedNodeId,
            searchQuery = searchQuery
        )
    }

    // Interactive View Transformation (Scale & Pan)
    var scale by remember { mutableFloatStateOf(0.9f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }
    var hasAutoFitted by remember { mutableStateOf(false) }

    // Fit Graph Centered in Viewport
    fun fitGraphToScreen(w: Float = containerWidth, h: Float = containerHeight, animated: Boolean = true) {
        if (w <= 0f || h <= 0f) return
        val margin = 80f
        val gw = graphLayout.totalWidth
        val gh = graphLayout.totalHeight

        val optimalScale = minOf(
            (w - margin * 2) / gw,
            (h - margin * 2) / gh
        ).coerceIn(0.32f, 1.35f)

        val targetPanX = w / 2f - graphLayout.centerX * optimalScale
        val targetPanY = h / 2f - graphLayout.centerY * optimalScale

        scale = optimalScale
        panX = targetPanX
        panY = targetPanY
    }

    // Auto-fit once container dimensions are measured or layout mode changes
    LaunchedEffect(layoutMode, rootNode.id) {
        if (containerWidth > 0 && containerHeight > 0) {
            fitGraphToScreen(containerWidth, containerHeight, animated = false)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(0.dp))
            .background(MaterialTheme.colorScheme.background)
            .testTag("interactive_mind_map_canvas")
    ) {
        val currentW = with(density) { maxWidth.toPx() }
        val currentH = with(density) { maxHeight.toPx() }

        LaunchedEffect(currentW, currentH) {
            containerWidth = currentW
            containerHeight = currentH
            if (!hasAutoFitted && currentW > 0 && currentH > 0) {
                fitGraphToScreen(currentW, currentH, animated = false)
                hasAutoFitted = true
            }
        }

        // Gesture handling layer (Pan & Pinch-to-zoom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(layoutMode) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (scale * zoom).coerceIn(0.22f, 3.2f)
                        scale = newScale
                        panX += pan.x + (centroid.x - panX) * (1f - newScale / oldScale)
                        panY += pan.y + (centroid.y - panY) * (1f - newScale / oldScale)
                    }
                }
                .pointerInput(layoutMode) {
                    detectTapGestures(
                        onDoubleTap = {
                            fitGraphToScreen(currentW, currentH)
                        }
                    )
                }
        ) {
            // 1. Dotted Engineering Background Grid
            val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = 40f * scale
                if (gridSpacing > 12f) {
                    val startX = (panX % gridSpacing + gridSpacing) % gridSpacing
                    val startY = (panY % gridSpacing + gridSpacing) % gridSpacing
                    var x = startX
                    while (x < size.width) {
                        var y = startY
                        while (y < size.height) {
                            drawCircle(
                                color = gridColor,
                                radius = (1.5f * scale).coerceIn(0.8f, 2.5f),
                                center = Offset(x, y)
                            )
                            y += gridSpacing
                        }
                        x += gridSpacing
                    }
                }
            }

            // 2. Transformed Graph Content Box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = panX
                        translationY = panY
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                // Connecting Bezier Curves Canvas
                val outlineColor = MaterialTheme.colorScheme.outline
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    graphLayout.edges.forEach { edge ->
                        drawGraphEdge(edge, outlineColor)
                    }
                }

                // Node Cards placed at calculated coordinates
                graphLayout.nodes.forEach { node ->
                    MindMapNodeCard(
                        node = node,
                        onClick = { onNodeSelected(node) },
                        onToggleExpand = { onToggleExpand(node.id) }
                    )
                }
            }
        }

        // 3. Floating Interactive Zoom & Viewport Controls
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("mind_map_zoom_controls")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Zoom Out Button
                IconButton(
                    onClick = {
                        val newScale = (scale * 0.8f).coerceIn(0.22f, 3.2f)
                        panX += (containerWidth / 2f - panX) * (1f - newScale / scale)
                        panY += (containerHeight / 2f - panY) * (1f - newScale / scale)
                        scale = newScale
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Zoom Out",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Percentage Badge
                Text(
                    text = "${(scale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Zoom In Button
                IconButton(
                    onClick = {
                        val newScale = (scale * 1.25f).coerceIn(0.22f, 3.2f)
                        panX += (containerWidth / 2f - panX) * (1f - newScale / scale)
                        panY += (containerHeight / 2f - panY) * (1f - newScale / scale)
                        scale = newScale
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Zoom In",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp))

                // Fit to Screen Button
                IconButton(
                    onClick = { fitGraphToScreen() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_fit_screen")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FitScreen,
                        contentDescription = "Fit to Screen",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Center on Root
                IconButton(
                    onClick = {
                        scale = 1.0f
                        panX = containerWidth / 2f
                        panY = containerHeight / 2f
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_center_root")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CenterFocusStrong,
                        contentDescription = "Center Root",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Floating Info Chip (Node count, Layout mode hint)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EmeraldEasy)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${graphLayout.nodes.size} Concepts • ${layoutMode.label}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

/**
 * Renders an individual graph node as a styled Material 3 card positioned in virtual canvas space.
 */
@Composable
private fun MindMapNodeCard(
    node: VisualGraphNode,
    onClick: () -> Unit,
    onToggleExpand: () -> Unit
) {
    val isRoot = node.depth == 0
    val isBranch = node.depth == 1
    val isLeaf = node.depth >= 2

    val cardColor = when {
        isRoot -> MaterialTheme.colorScheme.primary
        node.isSelected -> node.color.copy(alpha = 0.16f)
        node.isPathHighlighted -> node.color.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        node.isSelected -> node.color
        node.isMatchedBySearch -> AmberWarm
        node.isPathHighlighted -> node.color.copy(alpha = 0.8f)
        isRoot -> Color.Transparent
        else -> node.color.copy(alpha = 0.35f)
    }

    val borderWidth = when {
        node.isSelected -> 2.5.dp
        node.isMatchedBySearch -> 2.5.dp
        node.isPathHighlighted -> 1.8.dp
        isRoot -> 0.dp
        else -> 1.dp
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(node.x.roundToInt(), node.y.roundToInt()) }
            .size(width = node.width.dp, height = node.height.dp)
            .shadow(
                elevation = if (node.isSelected || isRoot) 6.dp else 2.dp,
                shape = RoundedCornerShape(if (isRoot) 16.dp else 12.dp)
            )
            .clip(RoundedCornerShape(if (isRoot) 16.dp else 12.dp))
            .background(cardColor)
            .border(borderWidth, borderColor, RoundedCornerShape(if (isRoot) 16.dp else 12.dp))
            .clickable { onClick() }
            .testTag("node_${node.id}")
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (isRoot) {
            // Root Node Content
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Hub,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (node.tag.isNotBlank()) {
                        Text(
                            text = node.tag.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            // Branch & Leaf Node Content
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Branch indicator dot / pill
                    Box(
                        modifier = Modifier
                            .size(if (isBranch) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(node.color)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = node.title,
                            style = if (isBranch) {
                                MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (node.tag.isNotBlank()) {
                            Text(
                                text = node.tag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = node.color,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Expand/Collapse Badge for nodes with children
                if (node.childCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = node.color.copy(alpha = if (node.isExpanded) 0.15f else 0.28f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onToggleExpand() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (node.isExpanded) Icons.Filled.Remove else Icons.Filled.Add,
                                contentDescription = if (node.isExpanded) "Collapse" else "Expand",
                                tint = node.color,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = node.childCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = node.color,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws connecting curved splines and bezier curves between parent and child nodes.
 */
private fun DrawScope.drawGraphEdge(edge: VisualGraphEdge, defaultOutline: Color) {
    val strokeColor = if (edge.isHighlighted) edge.color else edge.color.copy(alpha = 0.55f)
    val strokeWidth = if (edge.isHighlighted) 3.8.dp.toPx() else 2.0.dp.toPx()

    val path = Path().apply {
        moveTo(edge.startX, edge.startY)
        when (edge.layoutMode) {
            MindMapLayoutMode.HORIZONTAL_TREE -> {
                val midX = (edge.startX + edge.endX) / 2f
                cubicTo(
                    midX, edge.startY,
                    midX, edge.endY,
                    edge.endX, edge.endY
                )
            }
            MindMapLayoutMode.VERTICAL_TREE -> {
                val midY = (edge.startY + edge.endY) / 2f
                cubicTo(
                    edge.startX, midY,
                    edge.endX, midY,
                    edge.endX, edge.endY
                )
            }
            MindMapLayoutMode.RADIAL_STAR -> {
                // Radial curve: smooth arc toward child
                val midX = (edge.startX + edge.endX) / 2f
                val midY = (edge.startY + edge.endY) / 2f
                quadraticTo(midX, midY, edge.endX, edge.endY)
            }
        }
    }

    // Edge Path Stroke
    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Little connection anchor dot at target
    drawCircle(
        color = edge.color,
        radius = if (edge.isHighlighted) 3.5.dp.toPx() else 2.2.dp.toPx(),
        center = Offset(edge.endX, edge.endY)
    )
}
