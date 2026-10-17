package com.example.memory

import android.content.Context
import android.net.Uri
import com.example.data.local.entities.KnowledgeChunkEntity
import com.example.data.local.entities.KnowledgeDocumentEntity
import com.example.data.repository.MishaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class KnowledgeQueryResult(
    val hasMatch: Boolean,
    val query: String,
    val matchedChunks: List<KnowledgeChunkEntity>,
    val concatenatedContext: String,
    val sourceCitations: List<String>
)

class KnowledgeVaultManager(
    private val context: Context,
    private val repository: MishaRepository
) {

    /**
     * Ingests a new document into Knowledge Vault with chunking and token indexing.
     */
    suspend fun ingestDocument(
        title: String,
        content: String,
        fileName: String = "$title.txt",
        mimeType: String = "text/plain",
        category: String = "Notes"
    ): Long = withContext(Dispatchers.IO) {
        val cleanContent = content.trim()
        val chunks = createChunks(cleanContent, title)
        val doc = KnowledgeDocumentEntity(
            title = title,
            fileName = fileName,
            mimeType = mimeType,
            content = cleanContent,
            summary = generateQuickSummary(cleanContent),
            chunksCount = chunks.size,
            fileSize = cleanContent.toByteArray().size.toLong(),
            indexedAt = System.currentTimeMillis(),
            category = category
        )
        repository.addDocumentWithChunks(doc, chunks)
    }

    /**
     * Ingests content from a file URI selected via Storage Access Framework.
     */
    suspend fun ingestFromUri(uri: Uri, title: String, category: String = "Imported"): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
            val text = stringBuilder.toString().trim()
            if (text.isBlank()) {
                return@withContext Result.failure(Exception("Document content was empty."))
            }
            val docId = ingestDocument(title = title, content = text, fileName = uri.lastPathSegment ?: "$title.txt", category = category)
            Result.success(docId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves relevant context from indexed knowledge chunks based on user query tokens.
     */
    suspend fun searchAndRetrieve(query: String): KnowledgeQueryResult = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        val keywords = extractKeywords(q)

        val matchedChunks = mutableListOf<KnowledgeChunkEntity>()
        val seenChunkIds = mutableSetOf<Long>()

        for (kw in keywords) {
            if (kw.length < 3) continue
            val found = repository.searchKnowledge(kw)
            for (chunk in found) {
                if (seenChunkIds.add(chunk.id)) {
                    matchedChunks.add(chunk)
                }
            }
        }

        if (matchedChunks.isEmpty()) {
            return@withContext KnowledgeQueryResult(
                hasMatch = false,
                query = query,
                matchedChunks = emptyList(),
                concatenatedContext = "",
                sourceCitations = emptyList()
            )
        }

        // Rank by keyword frequency
        val ranked = matchedChunks.sortedByDescending { chunk ->
            keywords.count { kw -> chunk.textSnippet.contains(kw, ignoreCase = true) }
        }.take(4)

        val citations = ranked.map { "Document: \"${it.documentTitle}\" (Section #${it.chunkIndex + 1})" }.distinct()
        val contextText = ranked.joinToString("\n\n---\n") { chunk ->
            "[Source: ${chunk.documentTitle}]\n${chunk.textSnippet}"
        }

        KnowledgeQueryResult(
            hasMatch = true,
            query = query,
            matchedChunks = ranked,
            concatenatedContext = contextText,
            sourceCitations = citations
        )
    }

    private fun createChunks(content: String, docTitle: String): List<KnowledgeChunkEntity> {
        val chunks = mutableListOf<KnowledgeChunkEntity>()
        // Split by double newline (paragraphs) or fixed size windows (approx 350 chars with overlap)
        val paragraphs = content.split(Regex("\n\n+")).filter { it.isNotBlank() }

        if (paragraphs.isNotEmpty() && paragraphs.all { it.length < 500 }) {
            paragraphs.forEachIndexed { index, p ->
                val keywords = extractKeywords(p).joinToString(",")
                chunks.add(
                    KnowledgeChunkEntity(
                        id = 0,
                        documentId = 0,
                        documentTitle = docTitle,
                        chunkIndex = index,
                        textSnippet = p.trim(),
                        keywords = keywords
                    )
                )
            }
        } else {
            // Windowing
            val chunkSize = 350
            val overlap = 50
            var start = 0
            var chunkIndex = 0
            while (start < content.length) {
                val end = (start + chunkSize).coerceAtMost(content.length)
                val snippet = content.substring(start, end).trim()
                if (snippet.isNotBlank()) {
                    val keywords = extractKeywords(snippet).joinToString(",")
                    chunks.add(
                        KnowledgeChunkEntity(
                            id = 0,
                            documentId = 0,
                            documentTitle = docTitle,
                            chunkIndex = chunkIndex++,
                            textSnippet = snippet,
                            keywords = keywords
                        )
                    )
                }
                start += (chunkSize - overlap)
            }
        }
        return chunks
    }

    private fun extractKeywords(text: String): List<String> {
        val stopWords = setOf(
            "the", "is", "at", "which", "on", "and", "a", "an", "in", "to", "for", "with",
            "misha", "kya", "hai", "batao", "meri", "mera", "mein", "mein", "ka", "ki", "ke",
            "what", "how", "when", "where", "who", "why", "formula", "note", "notes"
        )
        return text.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.length > 2 && it !in stopWords }
            .distinct()
    }

    private fun generateQuickSummary(text: String): String {
        val firstSentence = text.lines().firstOrNull { it.isNotBlank() } ?: ""
        return if (firstSentence.length > 90) firstSentence.take(87) + "..." else firstSentence
    }
}
