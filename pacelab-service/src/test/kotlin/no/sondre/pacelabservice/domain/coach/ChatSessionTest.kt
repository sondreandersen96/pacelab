package no.sondre.pacelabservice.domain.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.time.Instant

class ChatSessionTest {
    @Test
    fun `records ordered user and assistant messages`() {
        val session = ChatSession.create(Instant.EPOCH)

        session.recordUserMessage("How was my training?", Instant.EPOCH.plusSeconds(1))
        session.recordAssistantMessage("I will look it up.", Instant.EPOCH.plusSeconds(2))

        assertEquals(listOf(ChatMessageRole.USER, ChatMessageRole.ASSISTANT), session.messages().map(ChatMessage::role))
        assertEquals("How was my training?", session.messages().first().content)
    }

    @Test
    fun `rejects blank messages`() {
        val session = ChatSession.create()

        assertFailsWith<IllegalArgumentException> { session.recordUserMessage("  ") }
    }
}
