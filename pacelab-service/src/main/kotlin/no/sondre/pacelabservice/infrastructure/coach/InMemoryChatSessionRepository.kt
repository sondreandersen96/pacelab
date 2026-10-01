package no.sondre.pacelabservice.infrastructure.coach

import no.sondre.pacelabservice.application.coach.port.output.ChatSessionRepository
import no.sondre.pacelabservice.domain.coach.ChatSession

class InMemoryChatSessionRepository : ChatSessionRepository {
    private val session = ChatSession.create()

    override fun get(): ChatSession = session

    override fun save(session: ChatSession) {
        require(session === this.session) { "Only the application chat session can be saved" }
    }
}
