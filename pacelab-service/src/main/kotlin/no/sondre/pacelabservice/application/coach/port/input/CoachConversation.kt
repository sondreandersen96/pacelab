package no.sondre.pacelabservice.application.coach.port.input

import no.sondre.pacelabservice.domain.coach.ChatMessage

interface GetCoachHistory {
    operator fun invoke(): List<ChatMessage>
}

interface SendCoachMessage {
    operator fun invoke(message: String): ChatMessage
}
