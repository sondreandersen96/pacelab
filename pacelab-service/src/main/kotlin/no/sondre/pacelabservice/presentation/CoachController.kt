package no.sondre.pacelabservice.presentation

import no.sondre.pacelabservice.application.coach.port.input.GetCoachHistory
import no.sondre.pacelabservice.application.coach.port.input.SendCoachMessage
import no.sondre.pacelabservice.application.coach.port.output.CoachModelUnavailableException
import no.sondre.pacelabservice.application.coach.port.output.CoachModelUpstreamException
import no.sondre.pacelabservice.domain.coach.ChatMessage
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class SendCoachMessageRequest(val message: String)

data class CoachMessageResponse(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: Instant,
)

@RestController
@RequestMapping("/coach/messages")
class CoachController(
    private val getCoachHistory: GetCoachHistory,
    private val sendCoachMessage: SendCoachMessage,
) {
    @GetMapping
    fun history(): List<CoachMessageResponse> = getCoachHistory().map(ChatMessage::toResponse)

    @PostMapping
    fun send(@RequestBody request: SendCoachMessageRequest): ResponseEntity<CoachMessageResponse> {
        require(request.message.isNotBlank()) { "message must not be blank" }
        return ResponseEntity.status(HttpStatus.CREATED).body(sendCoachMessage(request.message).toResponse())
    }
}

private fun ChatMessage.toResponse() = CoachMessageResponse(
    id = id.value.toString(),
    role = role.name,
    content = content,
    createdAt = createdAt,
)

@RestControllerAdvice
class CoachExceptionHandler {
    @ExceptionHandler(CoachModelUnavailableException::class)
    fun unavailable(): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiError("Coach is currently unavailable"))

    @ExceptionHandler(CoachModelUpstreamException::class)
    fun upstreamFailure(): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiError("Coach provider request failed"))
}
