package no.sondre.pacelabservice.infrastructure.coach.opencode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
import no.sondre.pacelabservice.application.coach.port.output.CoachModelMessage
import no.sondre.pacelabservice.application.coach.port.output.CoachModelResponse
import no.sondre.pacelabservice.application.coach.port.output.CoachToolDefinition
import no.sondre.pacelabservice.application.coach.port.output.CoachModelUpstreamException
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod.POST
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import org.springframework.web.client.RestClient

class OpenCodeResponsesCoachModelTest {
    @Test
    fun `maps an OpenCode function call to a coach tool call`() {
        val builder = RestClient.builder().baseUrl("https://opencode.ai/inference/openai/v1")
        val server = MockRestServiceServer.bindTo(builder).build()
        val model = OpenCodeResponsesCoachModel(
            builder.build(),
            OpenCodeProperties("test-key", "gpt-5.6-luna", "https://opencode.ai/inference/openai/v1"),
        )
        server.expect(requestTo("https://opencode.ai/inference/openai/v1/responses"))
            .andExpect(method(POST))
            .andExpect(header(AUTHORIZATION, "Bearer test-key"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "id": "response-1",
                      "output": [{
                        "type": "function_call",
                        "call_id": "call-1",
                        "name": "summarize_workout_history",
                        "arguments": "{\"workoutKind\":\"ENDURANCE\"}"
                      }]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val response = model.respond(
            listOf(CoachModelMessage.User("How much did I train?")),
            listOf(CoachToolDefinition("summarize_workout_history", "summary", emptyMap())),
        )

        val toolCalls = assertIs<CoachModelResponse.ToolCalls>(response)
        assertEquals("response-1", toolCalls.responseId)
        assertEquals("ENDURANCE", toolCalls.calls.single().arguments["workoutKind"])
        server.verify()
    }

    @Test
    fun `preserves a provider error as the cause of an upstream exception`() {
        val builder = RestClient.builder().baseUrl("https://opencode.ai/inference/openai/v1")
        val server = MockRestServiceServer.bindTo(builder).build()
        val model = OpenCodeResponsesCoachModel(
            builder.build(),
            OpenCodeProperties("test-key", "gpt-5.6-luna", "https://opencode.ai/inference/openai/v1"),
        )
        server.expect(requestTo("https://opencode.ai/inference/openai/v1/responses"))
            .andRespond(withBadRequest().body("{\"error\":{\"message\":\"Invalid request\"}}"))

        val exception = assertFailsWith<CoachModelUpstreamException> {
            model.respond(listOf(CoachModelMessage.User("How was my workout?")), emptyList())
        }

        assertEquals("OpenCode returned status 400", exception.message)
        assertIs<org.springframework.web.client.HttpClientErrorException.BadRequest>(exception.cause)
        server.verify()
    }
}
