package no.sondre.pacelabservice.application.coach.service

import kotlin.test.Test
import kotlin.test.assertEquals
import no.sondre.pacelabservice.application.coach.port.output.ChatSessionRepository
import no.sondre.pacelabservice.application.coach.port.output.CoachModel
import no.sondre.pacelabservice.application.coach.port.output.CoachModelMessage
import no.sondre.pacelabservice.application.coach.port.output.CoachModelResponse
import no.sondre.pacelabservice.application.coach.port.output.CoachTool
import no.sondre.pacelabservice.application.coach.port.output.CoachToolCall
import no.sondre.pacelabservice.application.coach.port.output.CoachToolDefinition
import no.sondre.pacelabservice.application.coach.port.output.CoachToolResult
import no.sondre.pacelabservice.domain.coach.ChatMessageRole
import no.sondre.pacelabservice.domain.coach.ChatSession

class SendCoachMessageServiceTest {
    @Test
    fun `records the user message and final assistant response`() {
        val repository = SessionRepository()
        val service = SendCoachMessageService(repository, FinalAnswerModel("You trained for 90 minutes."), emptyList())

        val reply = service("How did I train?")

        assertEquals("You trained for 90 minutes.", reply.content)
        assertEquals(
            listOf(ChatMessageRole.USER, ChatMessageRole.ASSISTANT),
            repository.session.messages().map { it.role },
        )
    }

    @Test
    fun `executes requested tool and continues the model turn`() {
        val repository = SessionRepository()
        val model = ToolThenAnswerModel()
        val tool = object : CoachTool {
            override val definition = CoachToolDefinition("workout_summary", "summary", emptyMap())
            override fun execute(arguments: Map<String, String?>) = mapOf("activityCount" to 2)
        }

        val reply = SendCoachMessageService(repository, model, listOf(tool))("How many workouts?")

        assertEquals("You completed 2 workouts.", reply.content)
        assertEquals(2, model.toolResults.single().output["activityCount"])
    }

    private class SessionRepository : ChatSessionRepository {
        val session = ChatSession.create()
        override fun get() = session
        override fun save(session: ChatSession) = Unit
    }

    private class FinalAnswerModel(private val answer: String) : CoachModel {
        override fun respond(messages: List<CoachModelMessage>, tools: List<CoachToolDefinition>) =
            CoachModelResponse.FinalAnswer(answer)

        override fun respondToToolResults(responseId: String, toolResults: List<CoachToolResult>) =
            error("No tool call expected")
    }

    private class ToolThenAnswerModel : CoachModel {
        lateinit var toolResults: List<CoachToolResult>

        override fun respond(messages: List<CoachModelMessage>, tools: List<CoachToolDefinition>) =
            CoachModelResponse.ToolCalls("response-1", listOf(CoachToolCall("call-1", "workout_summary", emptyMap())))

        override fun respondToToolResults(responseId: String, toolResults: List<CoachToolResult>): CoachModelResponse {
            this.toolResults = toolResults
            return CoachModelResponse.FinalAnswer("You completed 2 workouts.")
        }
    }
}
