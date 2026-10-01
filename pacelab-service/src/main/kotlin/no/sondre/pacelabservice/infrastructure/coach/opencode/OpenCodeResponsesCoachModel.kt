package no.sondre.pacelabservice.infrastructure.coach.opencode

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.ObjectMapper
import no.sondre.pacelabservice.application.coach.port.output.CoachModel
import no.sondre.pacelabservice.application.coach.port.output.CoachModelMessage
import no.sondre.pacelabservice.application.coach.port.output.CoachModelResponse
import no.sondre.pacelabservice.application.coach.port.output.CoachModelUnavailableException
import no.sondre.pacelabservice.application.coach.port.output.CoachModelUpstreamException
import no.sondre.pacelabservice.application.coach.port.output.CoachToolCall
import no.sondre.pacelabservice.application.coach.port.output.CoachToolDefinition
import no.sondre.pacelabservice.application.coach.port.output.CoachToolResult
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

class OpenCodeResponsesCoachModel(
    private val restClient: RestClient,
    private val properties: OpenCodeProperties,
    private val objectMapper: ObjectMapper = ObjectMapper(),
) : CoachModel {
    override fun respond(messages: List<CoachModelMessage>, tools: List<CoachToolDefinition>): CoachModelResponse {
        requireConfigured()
        return send(
            OpenCodeResponseRequest(
                model = properties.model,
                input = listOf(OpenCodeInputMessage("system", SYSTEM_INSTRUCTION)) + messages.map(OpenCodeInputMessage::from),
                tools = tools.map(OpenCodeFunctionTool::from),
            ),
        )
    }

    override fun respondToToolResults(responseId: String, toolResults: List<CoachToolResult>): CoachModelResponse {
        requireConfigured()
        return send(
            OpenCodeResponseRequest(
                model = properties.model,
                previousResponseId = responseId,
                input = toolResults.map { result ->
                    OpenCodeFunctionCallOutput(
                        callId = result.callId,
                        output = objectMapper.writeValueAsString(result.output),
                    )
                },
            ),
        )
    }

    private fun send(request: OpenCodeResponseRequest): CoachModelResponse {
        val response = try {
            restClient.post()
                .uri("/responses")
                .header("Authorization", "Bearer ${properties.apiKey}")
                .body(request)
                .retrieve()
                .body(OpenCodeResponse::class.java)
                ?: throw CoachModelUpstreamException("OpenCode returned an empty response")
        } catch (exception: RestClientResponseException) {
            throw CoachModelUpstreamException("OpenCode returned status ${exception.statusCode.value()}")
        } catch (_: ResourceAccessException) {
            throw CoachModelUnavailableException("OpenCode is unavailable")
        }

        val calls = response.output.filter { it.type == "function_call" }.map { output ->
            CoachToolCall(
                callId = requireNotNull(output.callId) { "OpenCode function call is missing call_id" },
                name = requireNotNull(output.name) { "OpenCode function call is missing name" },
                arguments = parseArguments(output.arguments),
            )
        }
        if (calls.isNotEmpty()) return CoachModelResponse.ToolCalls(response.id, calls)

        val content = response.outputText
            ?: response.output.firstNotNullOfOrNull { output ->
                output.content?.firstOrNull { it.type == "output_text" }?.text
            }
            ?: throw CoachModelUpstreamException("OpenCode response did not contain text or tool calls")
        return CoachModelResponse.FinalAnswer(content)
    }

    private fun parseArguments(arguments: String?): Map<String, String?> {
        if (arguments.isNullOrBlank()) return emptyMap()
        return try {
            objectMapper.readValue(arguments, Map::class.java).entries.associate { (key, value) -> key.toString() to value?.toString() }
        } catch (_: Exception) {
            mapOf("_invalid" to "Invalid tool arguments")
        }
    }

    private fun requireConfigured() {
        if (properties.apiKey.isBlank()) throw CoachModelUnavailableException("OpenCode API key is not configured")
    }

    private companion object {
        const val SYSTEM_INSTRUCTION = """
            You are Pacelab's workout coach. Answer using the available workout tools when data is needed.
            Never invent workout data. Explain clearly when the available data is insufficient.
            Keep answers concise and practical.
        """
    }
}

private data class OpenCodeResponseRequest(
    val model: String,
    val input: List<Any>,
    val tools: List<OpenCodeFunctionTool>? = null,
    @JsonProperty("previous_response_id")
    val previousResponseId: String? = null,
)

private data class OpenCodeInputMessage(
    val role: String,
    val content: String,
) {
    companion object {
        fun from(message: CoachModelMessage) = when (message) {
            is CoachModelMessage.User -> OpenCodeInputMessage("user", message.content)
            is CoachModelMessage.Assistant -> OpenCodeInputMessage("assistant", message.content)
        }
    }
}

private data class OpenCodeFunctionTool(
    val type: String = "function",
    val name: String,
    val description: String,
    val parameters: Map<String, Any>,
) {
    companion object {
        fun from(definition: CoachToolDefinition) = OpenCodeFunctionTool(
            name = definition.name,
            description = definition.description,
            parameters = definition.parameters,
        )
    }
}

private data class OpenCodeFunctionCallOutput(
    val type: String = "function_call_output",
    @JsonProperty("call_id")
    val callId: String,
    val output: String,
)

private data class OpenCodeResponse(
    val id: String,
    val output: List<OpenCodeOutput> = emptyList(),
    @JsonProperty("output_text")
    val outputText: String? = null,
)

private data class OpenCodeOutput(
    val type: String,
    @JsonProperty("call_id")
    val callId: String? = null,
    val name: String? = null,
    val arguments: String? = null,
    val content: List<OpenCodeContent>? = null,
)

private data class OpenCodeContent(
    val type: String,
    val text: String? = null,
)
