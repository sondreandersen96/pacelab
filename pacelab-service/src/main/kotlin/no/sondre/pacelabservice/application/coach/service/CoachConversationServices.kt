package no.sondre.pacelabservice.application.coach.service

import no.sondre.pacelabservice.application.coach.port.input.GetCoachHistory
import no.sondre.pacelabservice.application.coach.port.input.SendCoachMessage
import no.sondre.pacelabservice.application.coach.port.output.ChatSessionRepository
import no.sondre.pacelabservice.application.coach.port.output.CoachModel
import no.sondre.pacelabservice.application.coach.port.output.CoachModelResponse
import no.sondre.pacelabservice.application.coach.port.output.CoachTool
import no.sondre.pacelabservice.application.coach.port.output.CoachToolResult
import no.sondre.pacelabservice.application.coach.port.output.toCoachModelMessage
import no.sondre.pacelabservice.domain.coach.ChatMessage
import no.sondre.pacelabservice.domain.coach.ChatSession

class GetCoachHistoryService(
    private val chatSessionRepository: ChatSessionRepository,
) : GetCoachHistory {
    override fun invoke(): List<ChatMessage> = chatSessionRepository.get().messages()
}

class SendCoachMessageService(
    private val chatSessionRepository: ChatSessionRepository,
    private val coachModel: CoachModel,
    private val tools: List<CoachTool>,
) : SendCoachMessage {
    @Synchronized
    override fun invoke(message: String): ChatMessage {
        val session = chatSessionRepository.get()
        session.recordUserMessage(message)
        chatSessionRepository.save(session)

        var response = coachModel.respond(session.messages().map { it.toCoachModelMessage() }, tools.map { it.definition })
        repeat(MAX_TOOL_ROUNDS) {
            if (response is CoachModelResponse.FinalAnswer) {
                return recordAssistantMessage(session, response.content)
            }

            val toolCalls = response as CoachModelResponse.ToolCalls
            val results = toolCalls.calls.map { call ->
                val tool = tools.find { it.definition.name == call.name }
                CoachToolResult(
                    callId = call.callId,
                    output = tool?.execute(call.arguments)
                        ?: mapOf("error" to "Unknown tool: ${call.name}"),
                )
            }
            response = coachModel.respondToToolResults(toolCalls.responseId, results)
        }

        return recordAssistantMessage(session, "I could not complete the workout lookup. Please try again.")
    }

    private fun recordAssistantMessage(
        session: ChatSession,
        content: String,
    ): ChatMessage {
        val message = session.recordAssistantMessage(content)
        chatSessionRepository.save(session)
        return message
    }

    private companion object {
        const val MAX_TOOL_ROUNDS = 3
    }
}
