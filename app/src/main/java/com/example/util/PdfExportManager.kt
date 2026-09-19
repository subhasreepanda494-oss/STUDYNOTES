package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfExportOptions(
    val includeSummary: Boolean = true,
    val includeDetailedNotes: Boolean = true,
    val includeShortNotes: Boolean = true,
    val includeQuickRevision: Boolean = true,
    val includeExamNotes: Boolean = true,
    val includeDefinitions: Boolean = true,
    val includeFormulas: Boolean = true,
    val includeKeyPoints: Boolean = true,
    val includePracticeQuestions: Boolean = true
)

data class PdfExportResult(
    val file: File,
    val uri: Uri,
    val pageCount: Int,
    val title: String,
    val formattedSize: String
)

object PdfExportManager {

    private const val PAGE_WIDTH = 595   // Standard A4 width in PostScript points
    private const val PAGE_HEIGHT = 842  // Standard A4 height in PostScript points
    private const val MARGIN_X = 40f
    private const val MARGIN_TOP = 44f
    private const val MARGIN_BOTTOM = 46f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2) // 515 pt

    // Color Palette for Academic PDF
    private val COLOR_HEADER_BG = Color.rgb(30, 41, 59)         // Slate 800
    private val COLOR_PRIMARY = Color.rgb(67, 56, 202)           // Indigo 700
    private val COLOR_PRIMARY_LIGHT = Color.rgb(238, 242, 255)   // Indigo 50
    private val COLOR_ACCENT = Color.rgb(13, 148, 136)           // Teal 600
    private val COLOR_TEXT_DARK = Color.rgb(15, 23, 42)          // Slate 900
    private val COLOR_TEXT_MUTED = Color.rgb(100, 116, 139)      // Slate 500
    private val COLOR_CARD_BG = Color.rgb(248, 250, 252)         // Slate 50
    private val COLOR_BORDER = Color.rgb(226, 232, 240)          // Slate 200
    private val COLOR_AMBER = Color.rgb(217, 119, 6)             // Amber 600

    /**
     * Generates a beautifully formatted, multi-page printable PDF from a StudyNote.
     */
    fun generatePdf(
        context: Context,
        note: StudyNote,
        options: PdfExportOptions = PdfExportOptions()
    ): PdfExportResult {
        val document = PdfDocument()
        val writer = PageWriter(document, note.title, note.subject)

        // 1. Cover / Hero Banner on First Page
        writer.drawHeroBanner(note)

        // 2. Executive Summary Callout Box
        if (options.includeSummary && note.summary.isNotBlank()) {
            writer.drawSummaryBox(note.summary)
        }

        // 3. Key Concepts / High-Yield Points
        if (options.includeKeyPoints && note.keyPointsJson.isNotBlank()) {
            val keyPoints = StudyJsonParser.parseKeyPoints(note.keyPointsJson)
            if (keyPoints.isNotEmpty()) {
                writer.drawSectionHeader("Key Concepts & Takeaways", COLOR_PRIMARY)
                keyPoints.forEachIndexed { index, point ->
                    writer.drawBulletPoint("${index + 1}. $point", isNumbered = true)
                }
                writer.addSpacing(14f)
            }
        }

        // 4. Detailed Comprehensive Notes
        if (options.includeDetailedNotes && note.detailedNotes.isNotBlank()) {
            writer.drawSectionHeader("Comprehensive Study Notes", COLOR_PRIMARY)
            writer.drawFormattedMarkdown(note.detailedNotes)
            writer.addSpacing(14f)
        }

        // 5. Short Notes / Cheat Sheet
        if (options.includeShortNotes && note.shortNotes.isNotBlank()) {
            writer.drawSectionHeader("High-Yield Short Notes", COLOR_ACCENT)
            writer.drawFormattedMarkdown(note.shortNotes)
            writer.addSpacing(14f)
        }

        // 6. Quick Revision Notes
        if (options.includeQuickRevision && note.quickRevisionNotes.isNotBlank()) {
            writer.drawSectionHeader("Quick Revision Pointers", COLOR_AMBER)
            writer.drawFormattedMarkdown(note.quickRevisionNotes)
            writer.addSpacing(14f)
        }

        // 7. Exam Strategy & Scoring Tips
        if (options.includeExamNotes && note.examNotes.isNotBlank()) {
            writer.drawSectionHeader("Exam Strategy & Scoring Traps", COLOR_PRIMARY)
            writer.drawCalloutCard(note.examNotes, "EXAM BLUEPRINT", COLOR_PRIMARY_LIGHT, COLOR_PRIMARY)
            writer.addSpacing(14f)
        }

        // 8. Key Technical Definitions
        if (options.includeDefinitions && note.definitionsJson.isNotBlank()) {
            val defs = StudyJsonParser.parseDefinitions(note.definitionsJson)
            if (defs.isNotEmpty()) {
                writer.drawSectionHeader("Core Terminology & Definitions", COLOR_ACCENT)
                defs.forEach { item ->
                    writer.drawDefinitionCard(item)
                }
                writer.addSpacing(14f)
            }
        }

        // 9. Formulas & Mathematical Formulations
        if (options.includeFormulas && note.formulasJson.isNotBlank()) {
            val formulas = StudyJsonParser.parseFormulas(note.formulasJson)
            if (formulas.isNotEmpty()) {
                writer.drawSectionHeader("Formulas, Equations & Derivations", COLOR_PRIMARY)
                formulas.forEach { formula ->
                    writer.drawFormulaCard(formula)
                }
                writer.addSpacing(14f)
            }
        }

        // 10. Practice Questions & Self-Assessment
        if (options.includePracticeQuestions) {
            val shortQuestions = StudyJsonParser.parseShortQuestions(note.shortQuestionsJson)
            val mcqs = StudyJsonParser.parseMcqs(note.mcqsJson)

            if (shortQuestions.isNotEmpty() || mcqs.isNotEmpty()) {
                writer.drawSectionHeader("Self-Assessment & Practice Questions", COLOR_PRIMARY)

                if (shortQuestions.isNotEmpty()) {
                    writer.drawSubSectionHeader("Revision Questions & Model Answers")
                    shortQuestions.forEachIndexed { i, sq ->
                        writer.drawShortQuestionCard(i + 1, sq)
                    }
                    writer.addSpacing(10f)
                }

                if (mcqs.isNotEmpty()) {
                    writer.drawSubSectionHeader("Multiple Choice Questions (MCQs)")
                    mcqs.forEachIndexed { i, mcq ->
                        writer.drawMcqCard(i + 1, mcq)
                    }
                    writer.addSpacing(10f)
                }
            }
        }

        // Finish document and save to file
        val pageCount = writer.finish()

        val outputDir = File(context.cacheDir, "study_notes_pdf").apply { mkdirs() }
        val sanitizedTitle = note.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
        val file = File(outputDir, "${sanitizedTitle}_StudyNotes.pdf")

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val sizeKb = file.length() / 1024
        val formattedSize = if (sizeKb > 1024) "${String.format("%.1f", sizeKb / 1024f)} MB" else "$sizeKb KB"

        return PdfExportResult(
            file = file,
            uri = uri,
            pageCount = pageCount,
            title = note.title,
            formattedSize = formattedSize
        )
    }

    /**
     * Generates a printable PDF for a PYQ Exam Question and ChatGPT model solution.
     */
    fun generatePyqPdf(
        context: Context,
        pyq: PyqQuestion
    ): PdfExportResult {
        val document = PdfDocument()
        val writer = PageWriter(document, pyq.question.take(40), pyq.subject)

        // Hero Header
        writer.drawPyqHeroBanner(pyq)

        // Marking Scheme Blueprint
        if (pyq.markingScheme.isNotBlank()) {
            writer.drawSectionHeader("Marking Scheme & Evaluation Blueprint", COLOR_AMBER)
            writer.drawCalloutCard(pyq.markingScheme, "EVALUATION CRITERIA", Color.rgb(254, 243, 199), COLOR_AMBER)
            writer.addSpacing(14f)
        }

        // ChatGPT Model Answer
        writer.drawSectionHeader("Model Answer (ChatGPT Step-by-Step)", COLOR_PRIMARY)
        writer.drawFormattedMarkdown(pyq.answer)
        writer.addSpacing(14f)

        // Examiner's Traps & Tips
        if (pyq.examinerTips.isNotBlank()) {
            writer.drawSectionHeader("Examiner's Tips & Common Scoring Traps", COLOR_ACCENT)
            writer.drawCalloutCard(pyq.examinerTips, "HIGH-MARKS SECRET", Color.rgb(204, 251, 241), COLOR_ACCENT)
            writer.addSpacing(14f)
        }

        val pageCount = writer.finish()

        val outputDir = File(context.cacheDir, "pyq_pdf").apply { mkdirs() }
        val sanitizedSubj = pyq.subject.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(outputDir, "PYQ_${sanitizedSubj}_${pyq.year}_${pyq.marks}M.pdf")

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val sizeKb = file.length() / 1024
        val formattedSize = "$sizeKb KB"

        return PdfExportResult(
            file = file,
            uri = uri,
            pageCount = pageCount,
            title = "PYQ: ${pyq.question}",
            formattedSize = formattedSize
        )
    }

    /**
     * Sanitizes a string for safe use as a filename across filesystems.
     */
    fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50)
    }

    /**
     * Opens the generated PDF in any installed viewer.
     */
    fun openPdf(context: Context, result: PdfExportResult) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(result.uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Open Study Notes PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Shares the PDF via standard Android share sheet.
     */
    fun sharePdf(context: Context, result: PdfExportResult) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, result.uri)
            putExtra(Intent.EXTRA_SUBJECT, result.title)
            putExtra(Intent.EXTRA_TEXT, "Here are the study notes for \"${result.title}\" generated with StudyAI.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Study Notes PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Sends the PDF to Android's native PrintManager for printing or "Save as PDF".
     */
    fun printPdf(context: Context, result: PdfExportResult) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("${result.title}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(result.pageCount)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    FileInputStream(result.file).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        printManager.print(
            result.title,
            printAdapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
        )
    }

    /**
     * Saves a permanent copy to the public Downloads folder.
     */
    fun saveToDownloads(context: Context, result: PdfExportResult): Uri? {
        val fileName = "${result.file.nameWithoutExtension}_${System.currentTimeMillis()}.pdf"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StudyAI")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(result.file).use { input -> input.copyTo(out) }
                    }
                }
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val studyAiDir = File(downloadsDir, "StudyAI").apply { mkdirs() }
                val targetFile = File(studyAiDir, fileName)
                FileInputStream(result.file).use { input ->
                    FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                }
                Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            null
        }
    }

    // =========================================================================
    // Multi-page PageWriter Helper
    // =========================================================================
    private class PageWriter(
        private val document: PdfDocument,
        private val docTitle: String,
        private val docSubject: String
    ) {
        var pageIndex = 1
        private var currentPage: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        var currentY: Float = MARGIN_TOP

        private val bottomThreshold: Float = PAGE_HEIGHT - MARGIN_BOTTOM

        init {
            startNewPage()
        }

        fun startNewPage() {
            currentPage?.let { document.finishPage(it) }
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
            val newPage = document.startPage(pageInfo)
            currentPage = newPage
            canvas = newPage.canvas

            // Running header on page 2 onwards
            if (pageIndex > 1) {
                drawRunningHeader(newPage.canvas)
                currentY = MARGIN_TOP + 18f
            } else {
                currentY = MARGIN_TOP
            }

            // Running footer on every page
            drawRunningFooter(newPage.canvas, pageIndex)
            pageIndex++
        }

        fun ensureSpace(neededHeight: Float) {
            if (currentY + neededHeight > bottomThreshold) {
                startNewPage()
            }
        }

        fun addSpacing(spacing: Float) {
            currentY += spacing
        }

        fun finish(): Int {
            currentPage?.let { document.finishPage(it) }
            currentPage = null
            return pageIndex - 1
        }

        private fun drawRunningHeader(c: Canvas) {
            val textPaint = TextPaint().apply {
                color = COLOR_TEXT_MUTED
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            val titleText = if (docTitle.length > 50) docTitle.take(47) + "..." else docTitle
            c.drawText(titleText, MARGIN_X, MARGIN_TOP - 12f, textPaint)

            val subjectPaint = TextPaint(textPaint).apply {
                color = COLOR_PRIMARY
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            c.drawText(docSubject.uppercase(), PAGE_WIDTH - MARGIN_X, MARGIN_TOP - 12f, subjectPaint)

            val linePaint = Paint().apply {
                color = COLOR_BORDER
                strokeWidth = 0.8f
            }
            c.drawLine(MARGIN_X, MARGIN_TOP - 6f, PAGE_WIDTH - MARGIN_X, MARGIN_TOP - 6f, linePaint)
        }

        private fun drawRunningFooter(c: Canvas, pageNum: Int) {
            val linePaint = Paint().apply {
                color = COLOR_BORDER
                strokeWidth = 0.8f
            }
            c.drawLine(MARGIN_X, PAGE_HEIGHT - MARGIN_BOTTOM + 8f, PAGE_WIDTH - MARGIN_X, PAGE_HEIGHT - MARGIN_BOTTOM + 8f, linePaint)

            val footerPaint = TextPaint().apply {
                color = COLOR_TEXT_MUTED
                textSize = 8.5f
                isAntiAlias = true
            }
            c.drawText("StudyAI • Formatted for Academic Printing & Offline Study", MARGIN_X, PAGE_HEIGHT - MARGIN_BOTTOM + 22f, footerPaint)

            val pageNumPaint = TextPaint(footerPaint).apply {
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            c.drawText("Page $pageNum", PAGE_WIDTH - MARGIN_X, PAGE_HEIGHT - MARGIN_BOTTOM + 22f, pageNumPaint)
        }

        fun drawHeroBanner(note: StudyNote) {
            val bannerHeight = 118f
            ensureSpace(bannerHeight + 16f)

            val c = canvas ?: return
            val bannerRect = RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + bannerHeight)

            // Dark Slate Card Background
            val bgPaint = Paint().apply {
                color = COLOR_HEADER_BG
                isAntiAlias = true
            }
            c.drawRoundRect(bannerRect, 12f, 12f, bgPaint)

            // Accent Left Stripe
            val stripePaint = Paint().apply {
                color = COLOR_PRIMARY
                isAntiAlias = true
            }
            c.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + 6f, currentY + bannerHeight), 3f, 3f, stripePaint)

            var innerY = currentY + 22f

            // Overline Label
            val overlinePaint = TextPaint().apply {
                color = Color.rgb(224, 231, 255) // Light Indigo
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.12f
                isAntiAlias = true
            }
            c.drawText("STUDYAI ACADEMIC COMPREHENSIVE NOTES", MARGIN_X + 20f, innerY, overlinePaint)
            innerY += 24f

            // Note Title
            val titlePaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val titleText = if (note.title.length > 55) note.title.take(52) + "..." else note.title
            c.drawText(titleText, MARGIN_X + 20f, innerY, titlePaint)
            innerY += 24f

            // Metadata Chips / Labels
            val metaPaint = TextPaint().apply {
                color = Color.rgb(203, 213, 225) // Slate 300
                textSize = 9f
                isAntiAlias = true
            }
            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.createdAt))
            val metaText = "Subject: ${note.subject}   •   Difficulty: ${note.difficulty}   •   Lang: ${note.language}   •   Created: $dateStr"
            c.drawText(metaText, MARGIN_X + 20f, innerY, metaPaint)

            currentY += bannerHeight + 18f
        }

        fun drawPyqHeroBanner(pyq: PyqQuestion) {
            val bannerHeight = 126f
            ensureSpace(bannerHeight + 16f)

            val c = canvas ?: return
            val bannerRect = RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + bannerHeight)

            val bgPaint = Paint().apply {
                color = COLOR_HEADER_BG
                isAntiAlias = true
            }
            c.drawRoundRect(bannerRect, 12f, 12f, bgPaint)

            val stripePaint = Paint().apply {
                color = COLOR_AMBER
                isAntiAlias = true
            }
            c.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + 6f, currentY + bannerHeight), 3f, 3f, stripePaint)

            var innerY = currentY + 22f

            val overlinePaint = TextPaint().apply {
                color = Color.rgb(254, 215, 170)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.12f
                isAntiAlias = true
            }
            c.drawText("PREVIOUS YEAR QUESTION (PYQ) • CHATGPT MODEL SOLUTION", MARGIN_X + 20f, innerY, overlinePaint)
            innerY += 24f

            val qPaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val qLayout = StaticLayout.Builder.obtain(pyq.question, 0, pyq.question.length, qPaint, (CONTENT_WIDTH - 40).toInt())
                .setMaxLines(2)
                .build()
            c.save()
            c.translate(MARGIN_X + 20f, innerY - 12f)
            qLayout.draw(c)
            c.restore()

            innerY += 40f

            val metaPaint = TextPaint().apply {
                color = Color.rgb(203, 213, 225)
                textSize = 9.5f
                isAntiAlias = true
            }
            val metaText = "Subject: ${pyq.subject}   •   Weightage: ${pyq.marks} Marks   •   Exam Year: ${pyq.year}"
            c.drawText(metaText, MARGIN_X + 20f, innerY, metaPaint)

            currentY += bannerHeight + 18f
        }

        fun drawSummaryBox(summaryText: String) {
            val textPaint = TextPaint().apply {
                color = COLOR_TEXT_DARK
                textSize = 10f
                isAntiAlias = true
            }
            val innerWidth = (CONTENT_WIDTH - 28f).toInt()
            val layout = StaticLayout.Builder.obtain(summaryText, 0, summaryText.length, textPaint, innerWidth)
                .setLineSpacing(2f, 1.25f)
                .build()

            val boxHeight = layout.height + 40f
            ensureSpace(boxHeight + 10f)

            val c = canvas ?: return
            val rect = RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + boxHeight)

            val bgPaint = Paint().apply {
                color = COLOR_CARD_BG
                isAntiAlias = true
            }
            c.drawRoundRect(rect, 8f, 8f, bgPaint)

            val borderPaint = Paint().apply {
                color = COLOR_BORDER
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }
            c.drawRoundRect(rect, 8f, 8f, borderPaint)

            // Accent Left Bar
            val accentPaint = Paint().apply {
                color = COLOR_PRIMARY
                isAntiAlias = true
            }
            c.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + 4f, currentY + boxHeight), 2f, 2f, accentPaint)

            // Header Tag
            val tagPaint = TextPaint().apply {
                color = COLOR_PRIMARY
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.08f
                isAntiAlias = true
            }
            c.drawText("EXECUTIVE SUMMARY", MARGIN_X + 16f, currentY + 18f, tagPaint)

            c.save()
            c.translate(MARGIN_X + 16f, currentY + 28f)
            layout.draw(c)
            c.restore()

            currentY += boxHeight + 16f
        }

        fun drawSectionHeader(title: String, accentColor: Int = COLOR_PRIMARY) {
            ensureSpace(34f)
            val c = canvas ?: return

            // Accent Bar
            val barPaint = Paint().apply {
                color = accentColor
                isAntiAlias = true
            }
            c.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + 4f, currentY + 16f), 2f, 2f, barPaint)

            // Title
            val titlePaint = TextPaint().apply {
                color = COLOR_TEXT_DARK
                textSize = 12.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            c.drawText(title, MARGIN_X + 12f, currentY + 13f, titlePaint)

            // Underline
            val linePaint = Paint().apply {
                color = COLOR_BORDER
                strokeWidth = 0.8f
            }
            c.drawLine(MARGIN_X, currentY + 22f, MARGIN_X + CONTENT_WIDTH, currentY + 22f, linePaint)

            currentY += 30f
        }

        fun drawSubSectionHeader(title: String) {
            ensureSpace(24f)
            val c = canvas ?: return
            val paint = TextPaint().apply {
                color = COLOR_PRIMARY
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            c.drawText(title, MARGIN_X, currentY + 12f, paint)
            currentY += 18f
        }

        fun drawBulletPoint(text: String, isNumbered: Boolean = false) {
            val textPaint = TextPaint().apply {
                color = COLOR_TEXT_DARK
                textSize = 9.5f
                isAntiAlias = true
            }
            val textWidth = (CONTENT_WIDTH - 18f).toInt()
            val cleanText = text.removePrefix("- ").removePrefix("* ")
            val layout = StaticLayout.Builder.obtain(cleanText, 0, cleanText.length, textPaint, textWidth)
                .setLineSpacing(2f, 1.2f)
                .build()

            ensureSpace(layout.height + 6f)
            val c = canvas ?: return

            if (!isNumbered) {
                val bulletPaint = Paint().apply {
                    color = COLOR_PRIMARY
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                c.drawCircle(MARGIN_X + 5f, currentY + 6f, 2.5f, bulletPaint)
            }

            c.save()
            c.translate(MARGIN_X + 14f, currentY)
            layout.draw(c)
            c.restore()

            currentY += layout.height + 6f
        }

        fun drawCalloutCard(content: String, label: String, bgColor: Int, accentColor: Int) {
            val textPaint = TextPaint().apply {
                color = COLOR_TEXT_DARK
                textSize = 9.5f
                isAntiAlias = true
            }
            val innerWidth = (CONTENT_WIDTH - 24f).toInt()
            val layout = StaticLayout.Builder.obtain(content, 0, content.length, textPaint, innerWidth)
                .setLineSpacing(2f, 1.2f)
                .build()

            val cardHeight = layout.height + 34f
            ensureSpace(cardHeight + 10f)

            val c = canvas ?: return
            val rect = RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + cardHeight)

            val bgPaint = Paint().apply {
                color = bgColor
                isAntiAlias = true
            }
            c.drawRoundRect(rect, 8f, 8f, bgPaint)

            val accentPaint = Paint().apply {
                color = accentColor
                isAntiAlias = true
            }
            c.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + 4f, currentY + cardHeight), 2f, 2f, accentPaint)

            val labelPaint = TextPaint().apply {
                color = accentColor
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.08f
                isAntiAlias = true
            }
            c.drawText(label, MARGIN_X + 14f, currentY + 16f, labelPaint)

            c.save()
            c.translate(MARGIN_X + 14f, currentY + 24f)
            layout.draw(c)
            c.restore()

            currentY += cardHeight + 8f
        }

        fun drawDefinitionCard(item: DefinitionItem) {
            val termPaint = TextPaint().apply {
                color = COLOR_PRIMARY
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val defPaint = TextPaint().apply {
                color = COLOR_TEXT_DARK
                textSize = 9.5f
                isAntiAlias = true
            }
            val exPaint = TextPaint().apply {
                color = COLOR_TEXT_MUTED
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }

            val innerWidth = (CONTENT_WIDTH - 24f).toInt()
            val defLayout = StaticLayout.Builder.obtain(item.definition, 0, item.definition.length, defPaint, innerWidth)
                .setLineSpacing(1.5f, 1.15f)
                .build()

            val exLayout = if (item.example.isNotBlank()) {
                val exText = "Example: ${item.example}"
                StaticLayout.Builder.obtain(exText, 0, exText.length, exPaint, innerWidth).build()
            } else null

            val totalHeight = 22f + defLayout.height + (exLayout?.height?.plus(6f) ?: 0f) + 16f
            ensureSpace(totalHeight)

            val c = canvas ?: return
            val rect = RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + totalHeight)

            val bgPaint = Paint().apply {
                color = COLOR_CARD_BG
                isAntiAlias = true
            }
            c.drawRoundRect(rect, 6f, 6f, bgPaint)

            val borderPaint = Paint().apply {
                color = COLOR_BORDER
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
                isAntiAlias = true
            }
            c.drawRoundRect(rect, 6f, 6f, borderPaint)

            var cardInnerY = currentY + 16f
            c.drawText(item.term, MARGIN_X + 12f, cardInnerY, termPaint)
            cardInnerY += 6f

            c.save()
            c.translate(MARGIN_X + 12f, cardInnerY)
            defLayout.draw(c)
            c.restore()
            cardInnerY += defLayout.height + 4f

            exLayout?.let {
                c.save()
                c.translate(MARGIN_X + 12f, cardInnerY)
                it.draw(c)
                c.restore()
            }

            currentY += totalHeight + 8f
        }

        fun drawFormulaCard(item: FormulaItem) {
            val namePaint = TextPaint().apply {
                color = COLOR_TEXT_DARK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val formulaPaint = TextPaint().apply {
                color = COLOR_PRIMARY
                textSize = 10.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                isAntiAlias = true
            }
            val notePaint = TextPaint().apply {
                color = COLOR_TEXT_MUTED
                textSize = 8.5f
                isAntiAlias = true
            }

            val innerWidth = (CONTENT_WIDTH - 24f).toInt()
            val noteLayout = if (item.note.isNotBlank()) {
                StaticLayout.Builder.obtain(item.note, 0, item.note.length, notePaint, innerWidth).build()
            } else null

            val totalHeight = 36f + (noteLayout?.height?.plus(6f) ?: 0f) + 12f
            ensureSpace(totalHeight)

            val c = canvas ?: return
            val rect = RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + totalHeight)

            val bgPaint = Paint().apply {
                color = Color.rgb(240, 249, 255) // Sky 50
                isAntiAlias = true
            }
            c.drawRoundRect(rect, 6f, 6f, bgPaint)

            val borderPaint = Paint().apply {
                color = Color.rgb(186, 230, 253) // Sky 200
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
                isAntiAlias = true
            }
            c.drawRoundRect(rect, 6f, 6f, borderPaint)

            c.drawText(item.name, MARGIN_X + 12f, currentY + 16f, namePaint)
            c.drawText(item.formula, MARGIN_X + 12f, currentY + 32f, formulaPaint)

            noteLayout?.let {
                c.save()
                c.translate(MARGIN_X + 12f, currentY + 38f)
                it.draw(c)
                c.restore()
            }

            currentY += totalHeight + 8f
        }

        fun drawShortQuestionCard(index: Int, sq: ShortQuestionItem) {
            val qPaint = TextPaint().apply {
                color = COLOR_TEXT_DARK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val aPaint = TextPaint().apply {
                color = COLOR_TEXT_DARK
                textSize = 9.5f
                isAntiAlias = true
            }

            val innerWidth = (CONTENT_WIDTH - 24f).toInt()
            val qText = "Q$index: ${sq.question}"
            val qLayout = StaticLayout.Builder.obtain(qText, 0, qText.length, qPaint, innerWidth).build()

            val aText = "Ans: ${sq.answer}"
            val aLayout = StaticLayout.Builder.obtain(aText, 0, aText.length, aPaint, innerWidth)
                .setLineSpacing(1.5f, 1.15f)
                .build()

            val totalHeight = qLayout.height + aLayout.height + 24f
            ensureSpace(totalHeight)

            val c = canvas ?: return
            val rect = RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + totalHeight)

            val bgPaint = Paint().apply {
                color = COLOR_CARD_BG
                isAntiAlias = true
            }
            c.drawRoundRect(rect, 6f, 6f, bgPaint)

            var innerY = currentY + 10f
            c.save()
            c.translate(MARGIN_X + 12f, innerY)
            qLayout.draw(c)
            c.restore()
            innerY += qLayout.height + 6f

            c.save()
            c.translate(MARGIN_X + 12f, innerY)
            aLayout.draw(c)
            c.restore()

            currentY += totalHeight + 8f
        }

        fun drawMcqCard(index: Int, mcq: McqItem) {
            val qPaint = TextPaint().apply {
                color = COLOR_TEXT_DARK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val optPaint = TextPaint().apply {
                color = COLOR_TEXT_DARK
                textSize = 9f
                isAntiAlias = true
            }
            val explPaint = TextPaint().apply {
                color = COLOR_ACCENT
                textSize = 8.5f
                isAntiAlias = true
            }

            val innerWidth = (CONTENT_WIDTH - 24f).toInt()
            val qText = "MCQ $index: ${mcq.question}"
            val qLayout = StaticLayout.Builder.obtain(qText, 0, qText.length, qPaint, innerWidth).build()

            val optionsHeight = mcq.options.size * 16f
            val explText = if (mcq.explanation.isNotBlank()) "Explanation: ${mcq.explanation}" else ""
            val explLayout = if (explText.isNotBlank()) {
                StaticLayout.Builder.obtain(explText, 0, explText.length, explPaint, innerWidth).build()
            } else null

            val totalHeight = qLayout.height + optionsHeight + (explLayout?.height?.plus(6f) ?: 0f) + 24f
            ensureSpace(totalHeight)

            val c = canvas ?: return
            val rect = RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + totalHeight)

            val bgPaint = Paint().apply {
                color = COLOR_CARD_BG
                isAntiAlias = true
            }
            c.drawRoundRect(rect, 6f, 6f, bgPaint)

            var innerY = currentY + 10f
            c.save()
            c.translate(MARGIN_X + 12f, innerY)
            qLayout.draw(c)
            c.restore()
            innerY += qLayout.height + 8f

            mcq.options.forEachIndexed { optIndex, option ->
                val isCorrect = optIndex == mcq.correctIndex
                val optPrefix = ('A' + optIndex).toString()
                val optText = "[$optPrefix] $option ${if (isCorrect) "✓ (Correct)" else ""}"
                val currentOptPaint = if (isCorrect) {
                    TextPaint(optPaint).apply {
                        color = Color.rgb(22, 101, 52) // Green 800
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                } else optPaint
                c.drawText(optText, MARGIN_X + 16f, innerY + 10f, currentOptPaint)
                innerY += 16f
            }

            explLayout?.let {
                innerY += 4f
                c.save()
                c.translate(MARGIN_X + 12f, innerY)
                it.draw(c)
                c.restore()
            }

            currentY += totalHeight + 8f
        }

        fun drawFormattedMarkdown(markdown: String) {
            val lines = markdown.lines()
            var inCodeBlock = false
            val codeBuffer = StringBuilder()

            for (line in lines) {
                val trimmed = line.trim()

                if (trimmed.startsWith("```")) {
                    if (inCodeBlock) {
                        // Flush code block
                        drawCodeBlock(codeBuffer.toString())
                        codeBuffer.clear()
                        inCodeBlock = false
                    } else {
                        inCodeBlock = true
                    }
                    continue
                }

                if (inCodeBlock) {
                    codeBuffer.append(line).append("\n")
                    continue
                }

                if (trimmed.isEmpty()) {
                    addSpacing(6f)
                    continue
                }

                when {
                    trimmed.startsWith("### ") -> {
                        val headingText = trimmed.removePrefix("### ").removePrefix("**").removeSuffix("**")
                        drawHeading(headingText, 10.5f, Typeface.BOLD, COLOR_TEXT_DARK)
                    }
                    trimmed.startsWith("## ") -> {
                        val headingText = trimmed.removePrefix("## ").removePrefix("**").removeSuffix("**")
                        drawHeading(headingText, 11.5f, Typeface.BOLD, COLOR_PRIMARY)
                    }
                    trimmed.startsWith("# ") -> {
                        val headingText = trimmed.removePrefix("# ").removePrefix("**").removeSuffix("**")
                        drawHeading(headingText, 12.5f, Typeface.BOLD, COLOR_HEADER_BG)
                    }
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                        drawBulletPoint(trimmed.removePrefix("- ").removePrefix("* "))
                    }
                    Regex("^\\d+\\.\\s+.*").matches(trimmed) -> {
                        drawBulletPoint(trimmed, isNumbered = true)
                    }
                    else -> {
                        drawParagraph(trimmed)
                    }
                }
            }

            if (inCodeBlock && codeBuffer.isNotEmpty()) {
                drawCodeBlock(codeBuffer.toString())
            }
        }

        private fun drawHeading(text: String, size: Float, style: Int, color: Int) {
            ensureSpace(20f)
            val c = canvas ?: return
            val paint = TextPaint().apply {
                this.color = color
                textSize = size
                typeface = Typeface.create(Typeface.DEFAULT, style)
                isAntiAlias = true
            }
            c.drawText(text, MARGIN_X, currentY + size, paint)
            currentY += size + 8f
        }

        private fun drawParagraph(text: String) {
            val clean = text.replace("**", "").replace("*", "")
            val paint = TextPaint().apply {
                color = COLOR_TEXT_DARK
                textSize = 9.5f
                isAntiAlias = true
            }
            val layout = StaticLayout.Builder.obtain(clean, 0, clean.length, paint, CONTENT_WIDTH.toInt())
                .setLineSpacing(2f, 1.25f)
                .build()

            ensureSpace(layout.height + 4f)
            val c = canvas ?: return
            c.save()
            c.translate(MARGIN_X, currentY)
            layout.draw(c)
            c.restore()

            currentY += layout.height + 6f
        }

        private fun drawCodeBlock(code: String) {
            val paint = TextPaint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                isAntiAlias = true
            }
            val innerWidth = (CONTENT_WIDTH - 20f).toInt()
            val layout = StaticLayout.Builder.obtain(code.trimEnd(), 0, code.trimEnd().length, paint, innerWidth)
                .setLineSpacing(1f, 1.15f)
                .build()

            val blockHeight = layout.height + 20f
            ensureSpace(blockHeight + 8f)

            val c = canvas ?: return
            val rect = RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + blockHeight)

            val bgPaint = Paint().apply {
                color = Color.rgb(241, 245, 249) // Slate 100
                isAntiAlias = true
            }
            c.drawRoundRect(rect, 6f, 6f, bgPaint)

            c.save()
            c.translate(MARGIN_X + 10f, currentY + 10f)
            layout.draw(c)
            c.restore()

            currentY += blockHeight + 8f
        }
    }
}
