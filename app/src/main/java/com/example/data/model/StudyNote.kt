package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "study_notes")
data class StudyNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String,
    val difficulty: String, // "Easy", "Medium", "Advanced"
    val language: String,   // "English", "Spanish", etc.
    val originalFileName: String,
    val fileType: String,   // "PDF", "DOCX", "PPTX", "TXT", "JPG", "PNG", "CUSTOM"
    val rawContent: String,
    val detailedNotes: String,
    val shortNotes: String,
    val quickRevisionNotes: String,
    val examNotes: String,
    val definitionsJson: String = "[]",
    val formulasJson: String = "[]",
    val keyPointsJson: String = "[]",
    val summary: String = "",
    val mcqsJson: String = "[]",
    val shortQuestionsJson: String = "[]",
    val flashcardsJson: String = "[]",
    val mindMapJson: String = "{}",
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class DefinitionItem(
    val term: String,
    val definition: String,
    val example: String = ""
)

data class FormulaItem(
    val name: String,
    val formula: String,
    val note: String = ""
)

data class McqItem(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class ShortQuestionItem(
    val question: String,
    val answer: String,
    val explanation: String = ""
)

data class FlashcardItem(
    val front: String,
    val back: String,
    val category: String = "Core Concept"
)

data class MindMapNode(
    val title: String,
    val children: List<MindMapNode> = emptyList(),
    val tag: String = "",
    val description: String = "",
    val id: String = ""
)

// Helper converters between JSON and data classes
object StudyJsonParser {
    fun parseDefinitions(json: String): List<DefinitionItem> {
        val list = mutableListOf<DefinitionItem>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DefinitionItem(
                        term = obj.optString("term", ""),
                        definition = obj.optString("definition", ""),
                        example = obj.optString("example", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun parseFormulas(json: String): List<FormulaItem> {
        val list = mutableListOf<FormulaItem>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FormulaItem(
                        name = obj.optString("name", ""),
                        formula = obj.optString("formula", ""),
                        note = obj.optString("note", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun parseKeyPoints(json: String): List<String> {
        val list = mutableListOf<String>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }

    fun parseMcqs(json: String): List<McqItem> {
        val list = mutableListOf<McqItem>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val optsArray = obj.optJSONArray("options")
                val opts = mutableListOf<String>()
                if (optsArray != null) {
                    for (j in 0 until optsArray.length()) {
                        opts.add(optsArray.getString(j))
                    }
                }
                list.add(
                    McqItem(
                        question = obj.optString("question", ""),
                        options = opts,
                        correctIndex = obj.optInt("correctIndex", 0),
                        explanation = obj.optString("explanation", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun parseShortQuestions(json: String): List<ShortQuestionItem> {
        val list = mutableListOf<ShortQuestionItem>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ShortQuestionItem(
                        question = obj.optString("question", ""),
                        answer = obj.optString("answer", ""),
                        explanation = obj.optString("explanation", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun parseFlashcards(json: String): List<FlashcardItem> {
        val list = mutableListOf<FlashcardItem>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FlashcardItem(
                        front = obj.optString("front", ""),
                        back = obj.optString("back", ""),
                        category = obj.optString("category", "General")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun parseMindMap(json: String): MindMapNode {
        if (json.isBlank()) return MindMapNode(title = "Study Topic")
        try {
            val obj = JSONObject(json)
            return parseNode(obj)
        } catch (_: Exception) {
            return MindMapNode(title = "Main Concept")
        }
    }

    private fun parseNode(obj: JSONObject): MindMapNode {
        val title = obj.optString("title", "Topic")
        val tag = obj.optString("tag", "")
        val description = obj.optString("description", "")
        val id = obj.optString("id", "")
        val children = mutableListOf<MindMapNode>()
        val childrenArray = obj.optJSONArray("children")
        if (childrenArray != null) {
            for (i in 0 until childrenArray.length()) {
                val childObj = childrenArray.getJSONObject(i)
                children.add(parseNode(childObj))
            }
        }
        return MindMapNode(title = title, children = children, tag = tag, description = description, id = id)
    }

    fun toJson(definitions: List<DefinitionItem>): String {
        val arr = JSONArray()
        definitions.forEach {
            val obj = JSONObject()
            obj.put("term", it.term)
            obj.put("definition", it.definition)
            obj.put("example", it.example)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun formulasToJson(items: List<FormulaItem>): String {
        val arr = JSONArray()
        items.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("formula", it.formula)
            obj.put("note", it.note)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun mcqsToJson(items: List<McqItem>): String {
        val arr = JSONArray()
        items.forEach {
            val obj = JSONObject()
            obj.put("question", it.question)
            val opts = JSONArray()
            it.options.forEach { opt -> opts.put(opt) }
            obj.put("options", opts)
            obj.put("correctIndex", it.correctIndex)
            obj.put("explanation", it.explanation)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun shortQuestionsToJson(items: List<ShortQuestionItem>): String {
        val arr = JSONArray()
        items.forEach {
            val obj = JSONObject()
            obj.put("question", it.question)
            obj.put("answer", it.answer)
            obj.put("explanation", it.explanation)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun flashcardsToJson(items: List<FlashcardItem>): String {
        val arr = JSONArray()
        items.forEach {
            val obj = JSONObject()
            obj.put("front", it.front)
            obj.put("back", it.back)
            obj.put("category", it.category)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun mindMapToJson(node: MindMapNode): String {
        return nodeToJson(node).toString()
    }

    private fun nodeToJson(node: MindMapNode): JSONObject {
        val obj = JSONObject()
        obj.put("title", node.title)
        if (node.tag.isNotBlank()) obj.put("tag", node.tag)
        if (node.description.isNotBlank()) obj.put("description", node.description)
        if (node.id.isNotBlank()) obj.put("id", node.id)
        val arr = JSONArray()
        node.children.forEach { child ->
            arr.put(nodeToJson(child))
        }
        obj.put("children", arr)
        return obj
    }
}
