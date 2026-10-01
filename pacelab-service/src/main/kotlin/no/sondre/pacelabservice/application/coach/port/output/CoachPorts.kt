package no.sondre.pacelabservice.application.coach.port.output

import no.sondre.pacelabservice.domain.coach.ChatMessage
import no.sondre.pacelabservice.domain.coach.ChatSession

interface ChatSessionRepository {
    fun get(): ChatSession

    fun save(session: ChatSession)
}

interface CoachModel {
    fun respond(
        messages: List<CoachModelMessage>,
        tools: List<CoachToolDefinition>,
    ): CoachModelResponse

    fun respondToToolResults(
        responseId: String,
        toolResults: List<CoachToolResult>,
    ): CoachModelResponse
}

sealed interface CoachModelMessage {
    data class User(val content: String) : CoachModelMessage
    data class Assistant(val content: String) : CoachModelMessage
}

data class CoachToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>,
)

data class CoachToolCall(
    val callId: String,
    val name: String,
    val arguments: Map<String, String?>,
)

data class CoachToolResult(
    val callId: String,
    val output: Map<String, Any?>,
)

sealed interface CoachModelResponse {
    data class FinalAnswer(val content: String) : CoachModelResponse
    data class ToolCalls(val responseId: String, val calls: List<CoachToolCall>) : CoachModelResponse
}

interface CoachTool {
    val definition: CoachToolDefinition

    fun execute(arguments: Map<String, String?>): Map<String, Any?>
}

class CoachModelUnavailableException(message: String) : RuntimeException(message)

class CoachModelUpstreamException(message: String) : RuntimeException(message)

fun ChatMessage.toCoachModelMessage(): CoachModelMessage = when (role) {
    no.sondre.pacelabservice.domain.coach.ChatMessageRole.USER -> CoachModelMessage.User(content)
    no.sondre.pacelabservice.domain.coach.ChatMessageRole.ASSISTANT -> CoachModelMessage.Assistant(content)
}
