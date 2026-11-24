package io.github.rasagyavatsal.homeoremedica.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors the web app's server contracts (src/server/contracts.ts).

@Serializable
enum class Role {
    @SerialName("user") User,
    @SerialName("assistant") Assistant,
}

@Serializable
data class ChatTurn(val role: Role, val content: String)

@Serializable
data class Citation(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val author: String? = null,
    /** Null for chunks about principles rather than a remedy, such as the Organon's. */
    val remedyName: String? = null,
    val sectionTitle: String,
    val passageIndexes: List<Int> = emptyList(),
    val text: String,
)

@Serializable
data class ChatRequest(
    val message: String,
    val history: List<ChatTurn>,
    /** What the server condensed the chat's earlier turns into. */
    val summary: String? = null,
    /** Empty when `/api/kind` found the question describes no patient, so the repertories are not searched for it. */
    val symptoms: List<String>? = null,
    /** Set when `/api/kind` found the question off-topic, so the books are not searched for it. */
    val offTopic: Boolean? = null,
)

/**
 * Whether a question describes a patient, asks about anything else the books
 * could answer, or has nothing to do with homoeopathy, medicine or a patient.
 */
@Serializable
enum class QuestionKind {
    @SerialName("case") Case,
    @SerialName("question") Question,
    @SerialName("off-topic") OffTopic,
}

/** `POST /api/kind`: decided before a question is answered, so only a case is searched for in the repertories. */
@Serializable
data class KindResponse(val kind: QuestionKind)

/** `POST /api/chat`: the answer plus the trimmed history and the summary to send with the next question. */
@Serializable
data class ChatResponse(
    val answer: String,
    val corpusVersion: String,
    val model: String,
    val sources: List<Citation>,
    val history: List<ChatTurn>,
    /** What the chat's earlier turns were condensed into, now or before. */
    val summary: String? = null,
)
