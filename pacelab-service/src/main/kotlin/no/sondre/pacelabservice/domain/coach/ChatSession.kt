package no.sondre.pacelabservice.domain.coach

import java.time.Instant
import java.util.UUID

@JvmInline
value class ChatSessionId(val value: UUID) {
    companion object {
        fun generate() = ChatSessionId(UUID.randomUUID())
    }
}

@JvmInline
value class ChatMessageId(val value: UUID) {
    companion object {
        fun generate() = ChatMessageId(UUID.randomUUID())
    }
}

enum class ChatMessageRole {
    USER,
    ASSISTANT,
}

data class ChatMessage(
    val id: ChatMessageId,
    val role: ChatMessageRole,
    val content: String,
    val createdAt: Instant,
)

class ChatSession(
    val id: ChatSessionId,
    val createdAt: Instant,
) {
    private val messages = mutableListOf<ChatMessage>()

    fun recordUserMessage(content: String, createdAt: Instant = Instant.now()): ChatMessage =
        record(ChatMessageRole.USER, content, createdAt)

    fun recordAssistantMessage(content: String, createdAt: Instant = Instant.now()): ChatMessage =
        record(ChatMessageRole.ASSISTANT, content, createdAt)

    fun messages(): List<ChatMessage> = messages.toList()

    private fun record(role: ChatMessageRole, content: String, createdAt: Instant): ChatMessage {
        require(content.isNotBlank()) { "Chat message must not be blank" }
        return ChatMessage(ChatMessageId.generate(), role, content.trim(), createdAt).also(messages::add)
    }

    companion object {
        fun create(createdAt: Instant = Instant.now()) = ChatSession(ChatSessionId.generate(), createdAt)
    }
}
