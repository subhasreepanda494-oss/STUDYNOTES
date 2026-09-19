package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiStudyService
import com.example.data.model.PyqQuestion
import com.example.data.model.StudyNote
import com.example.util.PdfExportManager
import com.example.util.PdfExportOptions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StudyAI", appName)
  }

  @Test
  fun `verify pyq solver generates structured chatgpt style answer`() = runBlocking {
    val service = GeminiStudyService()
    val result = service.solvePyqQuestion(
      question = "Explain OSI 7-Layer Reference Model with diagram and compare with TCP/IP",
      subject = "CN",
      year = "2024",
      marks = 10
    )
    assertTrue(result.answer.isNotBlank())
    assertTrue(result.markingScheme.isNotBlank())
    assertTrue(result.examinerTips.isNotBlank())
    assertTrue(result.answer.contains("Executive Summary") || result.answer.contains("OSI"))
  }

  @Test
  fun `verify study notes export options and json parsers`() {
    val sampleNote = StudyNote(
      id = 1,
      title = "Computer Networks: OSI and TCP/IP Architecture!",
      subject = "Computer Networks",
      difficulty = "Medium",
      language = "English",
      originalFileName = "cn_unit1.pdf",
      fileType = "PDF",
      rawContent = "Sample networking content",
      detailedNotes = "# OSI Model Architecture\n\n## 1. Physical Layer\nTransmits raw bit stream over physical media.",
      shortNotes = "- OSI has 7 layers\n- TCP/IP has 4 layers",
      quickRevisionNotes = "- Layer 7: Application\n- Layer 4: Transport (TCP/UDP)",
      examNotes = "Focus on the 3-way handshake diagram and sliding window protocol derivation for 10-mark questions.",
      summary = "A comprehensive overview of network protocols and layered architectures.",
      definitionsJson = """[{"term":"Throughput","definition":"Rate of successful message delivery over communication channel.","example":"100 Mbps"}]""",
      formulasJson = """[{"name":"Bandwidth-Delay Product","formula":"BDP = Bandwidth × RTT","note":"Calculates pipe capacity"}]""",
      keyPointsJson = """["Layered architecture decouples protocols","TCP provides reliable stream service","UDP is connectionless"]""",
      mcqsJson = """[{"question":"Which layer handles routing?","options":["Data Link","Network","Transport","Session"],"correctIndex":1,"explanation":"Network layer routes packets."}]"""
    )

    // Verify Default Options
    val defaultOptions = PdfExportOptions()
    assertTrue(defaultOptions.includeSummary)
    assertTrue(defaultOptions.includeDetailedNotes)
    assertTrue(defaultOptions.includeKeyPoints)
    assertTrue(defaultOptions.includeDefinitions)
    assertTrue(defaultOptions.includeFormulas)
    assertTrue(defaultOptions.includeExamNotes)
    assertTrue(defaultOptions.includePracticeQuestions)

    // Verify Custom Options
    val customOptions = defaultOptions.copy(includePracticeQuestions = false, includeExamNotes = false)
    assertEquals(false, customOptions.includePracticeQuestions)
    assertEquals(false, customOptions.includeExamNotes)

    // Verify Safe Filename Sanitization
    val safeName = PdfExportManager.sanitizeFileName(sampleNote.title)
    assertTrue(!safeName.contains(":"))
    assertTrue(!safeName.contains("!"))
    assertTrue(safeName.startsWith("Computer_Networks"))

    // Verify JSON structures used by PDF Generator
    val keyPoints = com.example.data.model.StudyJsonParser.parseKeyPoints(sampleNote.keyPointsJson)
    assertEquals(3, keyPoints.size)
    assertEquals("Layered architecture decouples protocols", keyPoints[0])

    val defs = com.example.data.model.StudyJsonParser.parseDefinitions(sampleNote.definitionsJson)
    assertEquals(1, defs.size)
    assertEquals("Throughput", defs[0].term)

    val formulas = com.example.data.model.StudyJsonParser.parseFormulas(sampleNote.formulasJson)
    assertEquals(1, formulas.size)
    assertEquals("Bandwidth-Delay Product", formulas[0].name)

    val mcqs = com.example.data.model.StudyJsonParser.parseMcqs(sampleNote.mcqsJson)
    assertEquals(1, mcqs.size)
    assertEquals(4, mcqs[0].options.size)
    assertEquals(1, mcqs[0].correctIndex)
  }

  @Test
  fun `verify pyq question data model and formatting`() {
    val pyq = PyqQuestion(
      id = 101,
      subject = "CN",
      question = "Explain Distance Vector Routing with Count to Infinity Problem and Solution",
      year = "2023",
      marks = 10,
      answer = "# Distance Vector Routing\n\nDistance Vector routing algorithms use Bellman-Ford equation to compute shortest paths.\n\n### Count to Infinity Problem\nOccurs when a link fails and routing loops cause metric to increment indefinitely.",
      markingScheme = "Algorithm explanation: 4M\nCount to infinity diagram: 3M\nSplit horizon solution: 3M",
      examinerTips = "Always mention Split Horizon with Poisoned Reverse to secure full 10 marks."
    )

    val safeSubject = PdfExportManager.sanitizeFileName(pyq.subject)
    assertEquals("CN", safeSubject)
    assertTrue(pyq.markingScheme.contains("Algorithm explanation"))
    assertTrue(pyq.examinerTips.contains("Split Horizon"))
  }

  @Test
  fun `verify mind map automatic transformation from study note`() {
    val note = StudyNote(
      id = 1,
      title = "Operating Systems - Concurrency and Synchronization",
      subject = "Computer Science",
      difficulty = "Advanced",
      language = "English",
      originalFileName = "os_sync.pdf",
      fileType = "PDF",
      rawContent = "Semaphores and mutexes ensure mutual exclusion.",
      detailedNotes = "# Process Synchronization\n\n## Mutex Locks\n- Simplest software tool\n- Has acquire and release methods\n\n## Semaphores\n- Integer variable accessed through wait and signal\n- Counting and binary semaphores",
      shortNotes = "- Critical section problem\n- Mutual exclusion\n- Progress and bounded waiting",
      quickRevisionNotes = "- Deadlock conditions: Mutual exclusion, Hold and wait, No preemption, Circular wait",
      examNotes = "Write Dekker's or Peterson's algorithm code snippet for 7 marks.",
      definitionsJson = """[{"term":"Semaphore","definition":"A synchronization variable that controls access to common resource","example":"Counting semaphore initialized to 3"}]""",
      formulasJson = """[{"name":"Little's Law","formula":"L = lambda * W","note":"Applies to queueing networks"}]""",
      keyPointsJson = """["Mutual exclusion prevents race conditions","Semaphores avoid busy waiting"]""",
      summary = "A comprehensive exploration of concurrency control mechanisms in modern operating systems.",
      mcqsJson = """[{"question":"Which condition is NOT required for deadlock?","options":["Hold and wait","Mutual exclusion","Starvation","Circular wait"],"correctIndex":2,"explanation":"Starvation is distinct from deadlock."}]"""
    )

    // 1. Verify automatic transformation
    val graph = com.example.util.MindMapTransformer.transform(note, forceAutoGenerate = true)
    assertEquals("Operating Systems - Concurrency and Synchronization", graph.title)
    assertTrue("Should have multiple branches generated", graph.children.size >= 4)

    // Verify branch titles exist
    val branchTitles = graph.children.map { it.title }
    assertTrue(branchTitles.any { it.contains("Key Concepts") || it.contains("Core Principles") })
    assertTrue(branchTitles.any { it.contains("Definitions") || it.contains("Terminology") })
    assertTrue(branchTitles.any { it.contains("Formulas") })
    assertTrue(branchTitles.any { it.contains("Synchronization") || it.contains("Mutex") })
    assertTrue(branchTitles.any { it.contains("Exam") })

    // Verify unique IDs assigned
    assertEquals("root", graph.id)
    assertTrue(graph.children.all { it.id.isNotBlank() && it.id != "root" })
    assertTrue(graph.children.first().children.all { it.id.startsWith("root_0") })

    // 2. Verify Horizontal Tree Layout
    val horizontalLayout = com.example.util.MindMapTransformer.layoutGraph(
      root = graph,
      mode = com.example.util.MindMapLayoutMode.HORIZONTAL_TREE
    )
    assertTrue("Horizontal layout nodes should not be empty", horizontalLayout.nodes.isNotEmpty())
    assertTrue("Edges should connect nodes", horizontalLayout.edges.isNotEmpty())
    assertTrue("Total width should be greater than 0", horizontalLayout.totalWidth > 0)
    assertTrue("Total height should be greater than 0", horizontalLayout.totalHeight > 0)

    // 3. Verify Radial Star Layout
    val radialLayout = com.example.util.MindMapTransformer.layoutGraph(
      root = graph,
      mode = com.example.util.MindMapLayoutMode.RADIAL_STAR
    )
    assertTrue("Radial layout nodes should not be empty", radialLayout.nodes.isNotEmpty())
    assertTrue("Radial layout edges should not be empty", radialLayout.edges.isNotEmpty())

    // 4. Verify Vertical Tree Layout
    val verticalLayout = com.example.util.MindMapTransformer.layoutGraph(
      root = graph,
      mode = com.example.util.MindMapLayoutMode.VERTICAL_TREE
    )
    assertTrue("Vertical layout nodes should not be empty", verticalLayout.nodes.isNotEmpty())
    assertTrue("Vertical layout edges should not be empty", verticalLayout.edges.isNotEmpty())

    // 5. Verify Search Matching and Selection Highlighting
    val searchLayout = com.example.util.MindMapTransformer.layoutGraph(
      root = graph,
      mode = com.example.util.MindMapLayoutMode.HORIZONTAL_TREE,
      searchQuery = "Semaphore"
    )
    val matchedNodes = searchLayout.nodes.filter { it.isMatchedBySearch }
    assertTrue("Search should match Semaphore nodes", matchedNodes.isNotEmpty())

    // Select matched node and check path highlight
    val targetNodeId = matchedNodes.first().id
    val pathHighlightLayout = com.example.util.MindMapTransformer.layoutGraph(
      root = graph,
      mode = com.example.util.MindMapLayoutMode.HORIZONTAL_TREE,
      selectedNodeId = targetNodeId
    )
    val selectedNode = pathHighlightLayout.nodes.firstOrNull { it.id == targetNodeId }
    assertNotNull(selectedNode)
    assertTrue(selectedNode!!.isSelected)
    assertTrue(selectedNode.isPathHighlighted)

    // 6. Verify Subtree Collapsing
    val firstBranchId = graph.children.first().id
    val collapsedLayout = com.example.util.MindMapTransformer.layoutGraph(
      root = graph,
      mode = com.example.util.MindMapLayoutMode.HORIZONTAL_TREE,
      collapsedIds = setOf(firstBranchId)
    )
    val collapsedBranch = collapsedLayout.nodes.firstOrNull { it.id == firstBranchId }
    assertNotNull(collapsedBranch)
    assertEquals(false, collapsedBranch!!.isExpanded)
    // Children of collapsed branch should not appear in active layout nodes
    val childPrefix = "${firstBranchId}_"
    assertTrue(collapsedLayout.nodes.none { it.id.startsWith(childPrefix) })
  }
}
