package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.data.model.*
import java.util.UUID
import kotlin.math.*

enum class MindMapLayoutMode(val label: String) {
    HORIZONTAL_TREE("Horizontal Graph"),
    RADIAL_STAR("Radial Mind Map"),
    VERTICAL_TREE("Vertical Flow")
}

data class VisualGraphNode(
    val id: String,
    val title: String,
    val tag: String = "",
    val description: String = "",
    val depth: Int, // 0 = root, 1 = branch, 2 = subtopic, 3+ = leaf
    val branchIndex: Int,
    val parentId: String? = null,
    val childCount: Int = 0,
    val isExpanded: Boolean = true,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 170f,
    val height: Float = 54f,
    val color: Color = Color.Unspecified,
    val isMatchedBySearch: Boolean = false,
    val isSelected: Boolean = false,
    val isPathHighlighted: Boolean = false
)

data class VisualGraphEdge(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val color: Color,
    val isHighlighted: Boolean = false,
    val layoutMode: MindMapLayoutMode = MindMapLayoutMode.HORIZONTAL_TREE
)

data class VisualGraphLayout(
    val root: VisualGraphNode?,
    val nodes: List<VisualGraphNode>,
    val edges: List<VisualGraphEdge>,
    val boundsMinX: Float,
    val boundsMinY: Float,
    val boundsMaxX: Float,
    val boundsMaxY: Float
) {
    val totalWidth: Float get() = (boundsMaxX - boundsMinX).coerceAtLeast(100f)
    val totalHeight: Float get() = (boundsMaxY - boundsMinY).coerceAtLeast(100f)
    val centerX: Float get() = (boundsMinX + boundsMaxX) / 2f
    val centerY: Float get() = (boundsMinY + boundsMaxY) / 2f
}

object MindMapTransformer {

    val BranchColors = listOf(
        Color(0xFF4F46E5), // Indigo
        Color(0xFF0D9488), // Teal
        Color(0xFF8B5CF6), // Purple
        Color(0xFFD97706), // Amber
        Color(0xFF059669), // Emerald
        Color(0xFFE11D48), // Rose
        Color(0xFF0284C7), // Sky Blue
        Color(0xFFC026D3), // Fuchsia
        Color(0xFFF97316)  // Tangerine
    )

    /**
     * Automatically transforms a StudyNote into a structured, multi-level MindMapNode tree.
     * If the note already contains a rich mind map in JSON, it enriches it with identifiers and fallback data.
     * Otherwise, it parses detailed markdown, definitions, formulas, key points, and exam tips.
     */
    fun transform(note: StudyNote, forceAutoGenerate: Boolean = false): MindMapNode {
        val parsed = if (!forceAutoGenerate && note.mindMapJson.isNotBlank()) {
            StudyJsonParser.parseMindMap(note.mindMapJson)
        } else null

        if (parsed != null && parsed.children.isNotEmpty()) {
            return assignNodeIds(parsed, "root", 0, 0)
        }

        // Automatic Transformation from Study Note contents
        val rootTitle = note.title.ifBlank { note.subject.ifBlank { "Study Knowledge Map" } }
        val rootDesc = note.summary.ifBlank { "Interactive knowledge graph for ${note.title}" }

        val branches = mutableListOf<MindMapNode>()

        // 1. Core Principles & Key Points
        val keyPoints = StudyJsonParser.parseKeyPoints(note.keyPointsJson)
        if (keyPoints.isNotEmpty()) {
            val children = keyPoints.take(6).mapIndexed { idx, point ->
                MindMapNode(
                    title = cleanText(point).take(55),
                    tag = "Core Concept",
                    description = point
                )
            }
            branches.add(
                MindMapNode(
                    title = "Key Concepts & Principles",
                    tag = "Core",
                    description = "Fundamental principles and takeaways from ${note.subject}",
                    children = children
                )
            )
        } else if (note.shortNotes.isNotBlank()) {
            val shortBullets = extractBullets(note.shortNotes).take(5)
            if (shortBullets.isNotEmpty()) {
                branches.add(
                    MindMapNode(
                        title = "Core Principles",
                        tag = "Key Takeaways",
                        children = shortBullets.map { MindMapNode(title = it.take(55), description = it) }
                    )
                )
            }
        }

        // 2. Key Definitions & Terminology
        val definitions = StudyJsonParser.parseDefinitions(note.definitionsJson)
        if (definitions.isNotEmpty()) {
            val children = definitions.take(8).map { def ->
                val fullDesc = buildString {
                    append(def.definition)
                    if (def.example.isNotBlank()) {
                        append("\n\nExample: ").append(def.example)
                    }
                }
                MindMapNode(
                    title = def.term,
                    tag = "Definition",
                    description = fullDesc
                )
            }
            branches.add(
                MindMapNode(
                    title = "Definitions & Vocabulary",
                    tag = "Glossary",
                    description = "Key academic terms and formal definitions",
                    children = children
                )
            )
        }

        // 3. Mathematical Formulas & Theorems
        val formulas = StudyJsonParser.parseFormulas(note.formulasJson)
        if (formulas.isNotEmpty()) {
            val children = formulas.take(6).map { form ->
                MindMapNode(
                    title = form.name,
                    tag = form.formula,
                    description = "${form.formula}\n\nNote: ${form.note}"
                )
            }
            branches.add(
                MindMapNode(
                    title = "Formulas & Equations",
                    tag = "Math / Rules",
                    description = "Mathematical expressions and operational formulas",
                    children = children
                )
            )
        }

        // 4. Detailed Syllabus Breakdown (Parsed from Markdown headings and bullets)
        val syllabusBranches = extractMarkdownHierarchy(note.detailedNotes)
        if (syllabusBranches.isNotEmpty()) {
            branches.addAll(syllabusBranches.take(4))
        }

        // 5. Exam Highlights & Traps
        val examTips = mutableListOf<String>()
        if (note.examNotes.isNotBlank()) {
            examTips.addAll(extractBullets(note.examNotes).take(4))
        }
        if (note.quickRevisionNotes.isNotBlank()) {
            examTips.addAll(extractBullets(note.quickRevisionNotes).take(3))
        }
        if (examTips.isNotEmpty()) {
            branches.add(
                MindMapNode(
                    title = "Exam Focus & High Yield",
                    tag = "Exam Prep",
                    description = "Crucial scoring pointers and examiner expectations",
                    children = examTips.distinct().take(5).map {
                        MindMapNode(title = it.take(55), tag = "Important", description = it)
                    }
                )
            )
        }

        // 6. Practice Assessment (MCQs)
        val mcqs = StudyJsonParser.parseMcqs(note.mcqsJson)
        if (mcqs.isNotEmpty()) {
            val children = mcqs.take(4).mapIndexed { idx, mcq ->
                val correctOpt = mcq.options.getOrNull(mcq.correctIndex) ?: ""
                MindMapNode(
                    title = "Q: ${cleanText(mcq.question).take(48)}...",
                    tag = "Practice",
                    description = "Question: ${mcq.question}\n\nAnswer: $correctOpt\n\nExplanation: ${mcq.explanation}"
                )
            }
            branches.add(
                MindMapNode(
                    title = "Review Questions",
                    tag = "Self Test",
                    description = "Quick knowledge checks to reinforce learning",
                    children = children
                )
            )
        }

        // Fallback branch if content was minimal
        if (branches.isEmpty()) {
            branches.add(
                MindMapNode(
                    title = "Topic Overview",
                    tag = "Fundamentals",
                    description = note.summary.ifBlank { note.rawContent.take(120) },
                    children = listOf(
                        MindMapNode(title = "Primary Definitions", tag = "Core"),
                        MindMapNode(title = "Architectural Layers", tag = "Structure"),
                        MindMapNode(title = "Applied Practical Cases", tag = "Application")
                    )
                )
            )
        }

        val unindexedRoot = MindMapNode(
            title = rootTitle,
            tag = note.subject.ifBlank { "Concept Graph" },
            description = rootDesc,
            children = branches
        )

        return assignNodeIds(unindexedRoot, "root", 0, 0)
    }

    private fun assignNodeIds(node: MindMapNode, prefix: String, depth: Int, branchIdx: Int): MindMapNode {
        val assignedId = if (node.id.isNotBlank()) node.id else prefix
        val assignedChildren = node.children.mapIndexed { idx, child ->
            val childBranchIdx = if (depth == 0) idx else branchIdx
            val childPrefix = "${assignedId}_${idx}"
            assignNodeIds(child, childPrefix, depth + 1, childBranchIdx)
        }
        return node.copy(id = assignedId, children = assignedChildren)
    }

    private fun cleanText(text: String): String {
        return text.replace(Regex("^[#*\\-\\d.\\s]+"), "").trim()
    }

    private fun extractBullets(text: String): List<String> {
        val lines = text.lines()
        val bullets = mutableListOf<String>()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue
            if (line.startsWith("-") || line.startsWith("*") || line.matches(Regex("^\\d+[.)].*"))) {
                val cleaned = cleanText(line)
                if (cleaned.length >= 6) {
                    bullets.add(cleaned)
                }
            } else if (line.length in 10..90 && !line.startsWith("#")) {
                bullets.add(line)
            }
        }
        return bullets
    }

    private fun extractMarkdownHierarchy(markdown: String): List<MindMapNode> {
        if (markdown.isBlank()) return emptyList()
        val branches = mutableListOf<MindMapNode>()
        var currentSectionTitle: String? = null
        val currentSubItems = mutableListOf<String>()

        fun flushSection() {
            val title = currentSectionTitle ?: return
            val cleanTitle = cleanText(title)
            if (cleanTitle.isNotBlank()) {
                val children = currentSubItems.take(5).map {
                    MindMapNode(title = it.take(50), description = it)
                }
                branches.add(
                    MindMapNode(
                        title = cleanTitle.take(45),
                        tag = "Module",
                        description = "Section covering $cleanTitle",
                        children = children
                    )
                )
            }
            currentSubItems.clear()
            currentSectionTitle = null
        }

        for (line in markdown.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("# ") || trimmed.startsWith("## ") || trimmed.startsWith("### ")) {
                flushSection()
                currentSectionTitle = cleanText(trimmed)
            } else if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.matches(Regex("^\\d+[.)].*"))) {
                val bullet = cleanText(trimmed)
                if (bullet.length >= 5) {
                    currentSubItems.add(bullet)
                }
            }
        }
        flushSection()
        return branches
    }

    /**
     * Builds the visual graph layout coordinates and edges according to the requested LayoutMode.
     */
    fun layoutGraph(
        root: MindMapNode,
        mode: MindMapLayoutMode,
        collapsedIds: Set<String> = emptySet(),
        selectedNodeId: String? = null,
        searchQuery: String = ""
    ): VisualGraphLayout {
        val highlightedPathIds = if (!selectedNodeId.isNullOrBlank()) {
            findPathToNode(root, selectedNodeId)
        } else emptySet()

        return when (mode) {
            MindMapLayoutMode.HORIZONTAL_TREE -> layoutHorizontalTree(root, collapsedIds, selectedNodeId, highlightedPathIds, searchQuery)
            MindMapLayoutMode.RADIAL_STAR -> layoutRadialStar(root, collapsedIds, selectedNodeId, highlightedPathIds, searchQuery)
            MindMapLayoutMode.VERTICAL_TREE -> layoutVerticalTree(root, collapsedIds, selectedNodeId, highlightedPathIds, searchQuery)
        }
    }

    // --- HORIZONTAL TREE LAYOUT ---
    private fun layoutHorizontalTree(
        root: MindMapNode,
        collapsedIds: Set<String>,
        selectedId: String?,
        highlightedIds: Set<String>,
        searchQuery: String
    ): VisualGraphLayout {
        val nodes = mutableListOf<VisualGraphNode>()
        val edges = mutableListOf<VisualGraphEdge>()

        val nodeWidth = 180f
        val rootWidth = 230f
        val nodeHeight = 56f
        val rootHeight = 70f
        val levelGap = 130f
        val verticalSpacing = 24f

        // Helper to measure required subtree vertical span
        fun measureSubtreeSpan(node: MindMapNode): Float {
            val isCollapsed = collapsedIds.contains(node.id)
            if (isCollapsed || node.children.isEmpty()) {
                return nodeHeight + verticalSpacing
            }
            val childrenSpan = node.children.sumOf { measureSubtreeSpan(it).toDouble() }.toFloat()
            return max(nodeHeight + verticalSpacing, childrenSpan)
        }

        fun placeNode(
            node: MindMapNode,
            depth: Int,
            branchIdx: Int,
            parentId: String?,
            x: Float,
            centerY: Float
        ): VisualGraphNode {
            val isRoot = depth == 0
            val w = if (isRoot) rootWidth else nodeWidth
            val h = if (isRoot) rootHeight else nodeHeight
            val color = if (isRoot) Color(0xFF4F46E5) else BranchColors[branchIdx % BranchColors.size]
            val isExpanded = !collapsedIds.contains(node.id)
            val isMatched = searchQuery.isNotBlank() && (node.title.contains(searchQuery, ignoreCase = true) || node.tag.contains(searchQuery, ignoreCase = true))
            val isSelected = node.id == selectedId
            val isPathHighlighted = highlightedIds.contains(node.id)

            val vNode = VisualGraphNode(
                id = node.id,
                title = node.title,
                tag = node.tag,
                description = node.description,
                depth = depth,
                branchIndex = branchIdx,
                parentId = parentId,
                childCount = node.children.size,
                isExpanded = isExpanded,
                x = x,
                y = centerY - h / 2f,
                width = w,
                height = h,
                color = color,
                isMatchedBySearch = isMatched,
                isSelected = isSelected,
                isPathHighlighted = isPathHighlighted
            )
            nodes.add(vNode)

            if (isExpanded && node.children.isNotEmpty()) {
                val totalChildrenSpan = node.children.sumOf { measureSubtreeSpan(it).toDouble() }.toFloat()
                var currentChildCenterY = centerY - totalChildrenSpan / 2f
                val childX = x + w + levelGap

                node.children.forEachIndexed { idx, child ->
                    val childSpan = measureSubtreeSpan(child)
                    val nextCenterY = currentChildCenterY + childSpan / 2f
                    val childBranch = if (depth == 0) idx else branchIdx
                    val childVNode = placeNode(child, depth + 1, childBranch, node.id, childX, nextCenterY)

                    // Edge
                    val edgeColor = if (depth == 0) BranchColors[idx % BranchColors.size] else color
                    val isEdgeHighlighted = highlightedIds.contains(node.id) && highlightedIds.contains(child.id)
                    edges.add(
                        VisualGraphEdge(
                            id = "${node.id}_to_${child.id}",
                            fromNodeId = node.id,
                            toNodeId = child.id,
                            startX = x + w,
                            startY = centerY,
                            endX = childVNode.x,
                            endY = childVNode.y + childVNode.height / 2f,
                            color = edgeColor,
                            isHighlighted = isEdgeHighlighted,
                            layoutMode = MindMapLayoutMode.HORIZONTAL_TREE
                        )
                    )

                    currentChildCenterY += childSpan
                }
            }

            return vNode
        }

        val rootNode = placeNode(root, 0, 0, null, 0f, 0f)

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        nodes.forEach { n ->
            minX = min(minX, n.x)
            minY = min(minY, n.y)
            maxX = max(maxX, n.x + n.width)
            maxY = max(maxY, n.y + n.height)
        }

        return VisualGraphLayout(rootNode, nodes, edges, minX, minY, maxX, maxY)
    }

    // --- RADIAL STAR LAYOUT ---
    private fun layoutRadialStar(
        root: MindMapNode,
        collapsedIds: Set<String>,
        selectedId: String?,
        highlightedIds: Set<String>,
        searchQuery: String
    ): VisualGraphLayout {
        val nodes = mutableListOf<VisualGraphNode>()
        val edges = mutableListOf<VisualGraphEdge>()

        val rootW = 210f
        val rootH = 70f
        val branchW = 160f
        val branchH = 50f
        val leafW = 140f
        val leafH = 44f

        val radiusL1 = 300f
        val radiusL2 = 560f

        val isRootExpanded = !collapsedIds.contains(root.id)
        val isRootSelected = root.id == selectedId
        val isRootMatched = searchQuery.isNotBlank() && root.title.contains(searchQuery, ignoreCase = true)

        val rootVNode = VisualGraphNode(
            id = root.id,
            title = root.title,
            tag = root.tag,
            description = root.description,
            depth = 0,
            branchIndex = 0,
            childCount = root.children.size,
            isExpanded = isRootExpanded,
            x = -rootW / 2f,
            y = -rootH / 2f,
            width = rootW,
            height = rootH,
            color = Color(0xFF4F46E5),
            isMatchedBySearch = isRootMatched,
            isSelected = isRootSelected,
            isPathHighlighted = highlightedIds.contains(root.id)
        )
        nodes.add(rootVNode)

        if (isRootExpanded && root.children.isNotEmpty()) {
            val nBranches = root.children.size
            root.children.forEachIndexed { bIdx, branch ->
                val angle = (2.0 * Math.PI / nBranches * bIdx - Math.PI / 2.0).toFloat()
                val branchCenterX = radiusL1 * cos(angle)
                val branchCenterY = radiusL1 * sin(angle)
                val branchColor = BranchColors[bIdx % BranchColors.size]
                val isBranchExpanded = !collapsedIds.contains(branch.id)
                val isBranchSelected = branch.id == selectedId
                val isBranchMatched = searchQuery.isNotBlank() && (branch.title.contains(searchQuery, ignoreCase = true) || branch.tag.contains(searchQuery, ignoreCase = true))

                val branchVNode = VisualGraphNode(
                    id = branch.id,
                    title = branch.title,
                    tag = branch.tag,
                    description = branch.description,
                    depth = 1,
                    branchIndex = bIdx,
                    parentId = root.id,
                    childCount = branch.children.size,
                    isExpanded = isBranchExpanded,
                    x = branchCenterX - branchW / 2f,
                    y = branchCenterY - branchH / 2f,
                    width = branchW,
                    height = branchH,
                    color = branchColor,
                    isMatchedBySearch = isBranchMatched,
                    isSelected = isBranchSelected,
                    isPathHighlighted = highlightedIds.contains(branch.id)
                )
                nodes.add(branchVNode)

                // Edge from Root to Branch
                val isEdgeHighlighted = highlightedIds.contains(root.id) && highlightedIds.contains(branch.id)
                edges.add(
                    VisualGraphEdge(
                        id = "${root.id}_to_${branch.id}",
                        fromNodeId = root.id,
                        toNodeId = branch.id,
                        startX = 0f,
                        startY = 0f,
                        endX = branchCenterX,
                        endY = branchCenterY,
                        color = branchColor,
                        isHighlighted = isEdgeHighlighted,
                        layoutMode = MindMapLayoutMode.RADIAL_STAR
                    )
                )

                // Level 2 Leaves
                if (isBranchExpanded && branch.children.isNotEmpty()) {
                    val nLeaves = branch.children.size
                    val sectorSpan = (2.0 * Math.PI / nBranches * 0.75).toFloat()
                    branch.children.forEachIndexed { lIdx, leaf ->
                        val leafAngle = if (nLeaves == 1) angle else {
                            angle - sectorSpan / 2f + (sectorSpan / (nLeaves - 1)) * lIdx
                        }
                        val leafCenterX = radiusL2 * cos(leafAngle)
                        val leafCenterY = radiusL2 * sin(leafAngle)
                        val isLeafSelected = leaf.id == selectedId
                        val isLeafMatched = searchQuery.isNotBlank() && (leaf.title.contains(searchQuery, ignoreCase = true) || leaf.tag.contains(searchQuery, ignoreCase = true))

                        val leafVNode = VisualGraphNode(
                            id = leaf.id,
                            title = leaf.title,
                            tag = leaf.tag,
                            description = leaf.description,
                            depth = 2,
                            branchIndex = bIdx,
                            parentId = branch.id,
                            childCount = leaf.children.size,
                            isExpanded = true,
                            x = leafCenterX - leafW / 2f,
                            y = leafCenterY - leafH / 2f,
                            width = leafW,
                            height = leafH,
                            color = branchColor,
                            isMatchedBySearch = isLeafMatched,
                            isSelected = isLeafSelected,
                            isPathHighlighted = highlightedIds.contains(leaf.id)
                        )
                        nodes.add(leafVNode)

                        // Edge from Branch to Leaf
                        val isLeafEdgeHighlighted = highlightedIds.contains(branch.id) && highlightedIds.contains(leaf.id)
                        edges.add(
                            VisualGraphEdge(
                                id = "${branch.id}_to_${leaf.id}",
                                fromNodeId = branch.id,
                                toNodeId = leaf.id,
                                startX = branchCenterX,
                                startY = branchCenterY,
                                endX = leafCenterX,
                                endY = leafCenterY,
                                color = branchColor.copy(alpha = 0.8f),
                                isHighlighted = isLeafEdgeHighlighted,
                                layoutMode = MindMapLayoutMode.RADIAL_STAR
                            )
                        )
                    }
                }
            }
        }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        nodes.forEach { n ->
            minX = min(minX, n.x)
            minY = min(minY, n.y)
            maxX = max(maxX, n.x + n.width)
            maxY = max(maxY, n.y + n.height)
        }

        return VisualGraphLayout(rootVNode, nodes, edges, minX, minY, maxX, maxY)
    }

    // --- VERTICAL TREE LAYOUT ---
    private fun layoutVerticalTree(
        root: MindMapNode,
        collapsedIds: Set<String>,
        selectedId: String?,
        highlightedIds: Set<String>,
        searchQuery: String
    ): VisualGraphLayout {
        val nodes = mutableListOf<VisualGraphNode>()
        val edges = mutableListOf<VisualGraphEdge>()

        val nodeWidth = 170f
        val rootWidth = 220f
        val nodeHeight = 54f
        val rootHeight = 64f
        val levelGapY = 100f
        val horizontalGap = 24f

        fun measureSubtreeWidth(node: MindMapNode): Float {
            val isCollapsed = collapsedIds.contains(node.id)
            if (isCollapsed || node.children.isEmpty()) {
                return nodeWidth + horizontalGap
            }
            val childrenWidth = node.children.sumOf { measureSubtreeWidth(it).toDouble() }.toFloat()
            return max(nodeWidth + horizontalGap, childrenWidth)
        }

        fun placeNode(
            node: MindMapNode,
            depth: Int,
            branchIdx: Int,
            parentId: String?,
            centerX: Float,
            y: Float
        ): VisualGraphNode {
            val isRoot = depth == 0
            val w = if (isRoot) rootWidth else nodeWidth
            val h = if (isRoot) rootHeight else nodeHeight
            val color = if (isRoot) Color(0xFF4F46E5) else BranchColors[branchIdx % BranchColors.size]
            val isExpanded = !collapsedIds.contains(node.id)
            val isMatched = searchQuery.isNotBlank() && (node.title.contains(searchQuery, ignoreCase = true) || node.tag.contains(searchQuery, ignoreCase = true))
            val isSelected = node.id == selectedId

            val vNode = VisualGraphNode(
                id = node.id,
                title = node.title,
                tag = node.tag,
                description = node.description,
                depth = depth,
                branchIndex = branchIdx,
                parentId = parentId,
                childCount = node.children.size,
                isExpanded = isExpanded,
                x = centerX - w / 2f,
                y = y,
                width = w,
                height = h,
                color = color,
                isMatchedBySearch = isMatched,
                isSelected = isSelected,
                isPathHighlighted = highlightedIds.contains(node.id)
            )
            nodes.add(vNode)

            if (isExpanded && node.children.isNotEmpty()) {
                val totalChildrenWidth = node.children.sumOf { measureSubtreeWidth(it).toDouble() }.toFloat()
                var currentChildLeft = centerX - totalChildrenWidth / 2f
                val childY = y + h + levelGapY

                node.children.forEachIndexed { idx, child ->
                    val childWidthSpan = measureSubtreeWidth(child)
                    val nextCenterX = currentChildLeft + childWidthSpan / 2f
                    val childBranch = if (depth == 0) idx else branchIdx
                    val childVNode = placeNode(child, depth + 1, childBranch, node.id, nextCenterX, childY)

                    // Edge
                    val edgeColor = if (depth == 0) BranchColors[idx % BranchColors.size] else color
                    val isEdgeHighlighted = highlightedIds.contains(node.id) && highlightedIds.contains(child.id)
                    edges.add(
                        VisualGraphEdge(
                            id = "${node.id}_to_${child.id}",
                            fromNodeId = node.id,
                            toNodeId = child.id,
                            startX = centerX,
                            startY = y + h,
                            endX = childVNode.x + childVNode.width / 2f,
                            endY = childVNode.y,
                            color = edgeColor,
                            isHighlighted = isEdgeHighlighted,
                            layoutMode = MindMapLayoutMode.VERTICAL_TREE
                        )
                    )

                    currentChildLeft += childWidthSpan
                }
            }

            return vNode
        }

        val rootNode = placeNode(root, 0, 0, null, 0f, 0f)

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        nodes.forEach { n ->
            minX = min(minX, n.x)
            minY = min(minY, n.y)
            maxX = max(maxX, n.x + n.width)
            maxY = max(maxY, n.y + n.height)
        }

        return VisualGraphLayout(rootNode, nodes, edges, minX, minY, maxX, maxY)
    }

    private fun findPathToNode(root: MindMapNode, targetId: String): Set<String> {
        val path = mutableSetOf<String>()
        fun dfs(curr: MindMapNode): Boolean {
            if (curr.id == targetId) {
                path.add(curr.id)
                return true
            }
            for (child in curr.children) {
                if (dfs(child)) {
                    path.add(curr.id)
                    return true
                }
            }
            return false
        }
        dfs(root)
        return path
    }
}
