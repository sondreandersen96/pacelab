package no.sondre.pacelabservice.presentation

import com.fasterxml.jackson.annotation.JsonInclude
import no.sondre.pacelabservice.application.workout.port.input.CreateEnduranceWorkoutCommand
import no.sondre.pacelabservice.application.workout.port.input.CreateStrengthWorkoutCommand
import no.sondre.pacelabservice.application.workout.port.input.CreateWorkout
import no.sondre.pacelabservice.application.workout.port.input.CreateWorkoutCommand
import no.sondre.pacelabservice.application.workout.port.input.FindWorkouts
import no.sondre.pacelabservice.application.workout.port.input.GetWorkout
import no.sondre.pacelabservice.application.workout.port.output.WorkoutCursor
import no.sondre.pacelabservice.application.workout.port.output.WorkoutSearchCriteria
import no.sondre.pacelabservice.domain.DistanceMeters
import no.sondre.pacelabservice.domain.EnduranceWorkout
import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import no.sondre.pacelabservice.domain.PowerWatts
import no.sondre.pacelabservice.domain.StrengthWorkout
import no.sondre.pacelabservice.domain.Workout
import no.sondre.pacelabservice.domain.WorkoutId
import no.sondre.pacelabservice.domain.WorkoutKind
import no.sondre.pacelabservice.domain.WorkoutSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Duration
import java.time.Instant
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import java.util.UUID

data class CreateWorkoutRequest(
    val source: WorkoutSource,
    val workoutKind: WorkoutKind,
    val startedAt: Instant,
    val durationSeconds: Long,
    val type: EnduranceWorkoutType? = null,
    val distanceMeters: Double? = null,
    val averagePowerWatts: Double? = null,
)

data class WorkoutResponse(
    val id: String,
    val source: WorkoutSource,
    val workoutKind: WorkoutKind,
    val startedAt: Instant,
    val durationSeconds: Long,
    val type: EnduranceWorkoutType? = null,
    val distanceMeters: Double? = null,
    val averagePowerWatts: Double? = null,
    val averageSpeedMetersPerSecond: Double? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WorkoutPageResponse(
    val items: List<WorkoutResponse>,
    val nextCursor: String?,
)

@RestController
@RequestMapping("/workouts")
class WorkoutController(
    private val createWorkout: CreateWorkout,
    private val getWorkout: GetWorkout,
    private val findWorkouts: FindWorkouts,
) {
    @PostMapping
    fun create(@RequestBody request: CreateWorkoutRequest): ResponseEntity<WorkoutResponse> {
        val workout = createWorkout(request.toCommand())
        return ResponseEntity.status(HttpStatus.CREATED).body(workout.toResponse())
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): WorkoutResponse =
        getWorkout(WorkoutId(UUID.fromString(id)))?.toResponse() ?: throw WorkoutNotFoundException

    @GetMapping
    fun find(
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
        @RequestParam(required = false) workoutKind: WorkoutKind?,
        @RequestParam(required = false) enduranceType: EnduranceWorkoutType?,
        @RequestParam(required = false, defaultValue = "50") limit: Int,
        @RequestParam(required = false) cursor: String?,
    ): WorkoutPageResponse {
        require((from == null) == (to == null)) { "Both from and to must be supplied together" }
        require(from == null || from < to) { "from must be before to" }
        require(limit in 1..100) { "limit must be between 1 and 100" }
        require(workoutKind != WorkoutKind.STRENGTH || enduranceType == null) {
            "Strength workouts cannot have an endurance type filter"
        }

        val page = findWorkouts(
            WorkoutSearchCriteria(
                startedAtFrom = from,
                startedAtTo = to,
                kind = workoutKind,
                enduranceType = enduranceType,
            ),
            cursor?.toWorkoutCursor(),
            limit,
        )
        return WorkoutPageResponse(page.workouts.map { it.toResponse() }, page.nextCursor?.encode())
    }
}

private fun CreateWorkoutRequest.toCommand(): CreateWorkoutCommand = when (workoutKind) {
    WorkoutKind.ENDURANCE -> CreateEnduranceWorkoutCommand(
        source = source,
        startedAt = startedAt,
        duration = Duration.ofSeconds(durationSeconds),
        type = requireNotNull(type) { "Endurance workouts require a type" },
        distance = DistanceMeters(requireNotNull(distanceMeters) { "Endurance workouts require a distance" }),
        averagePower = averagePowerWatts?.let(::PowerWatts),
    )
    WorkoutKind.STRENGTH -> {
        require(type == null) { "Strength workouts cannot have an endurance type" }
        require(distanceMeters == null) { "Strength workouts cannot have a distance" }
        require(averagePowerWatts == null) { "Strength workouts cannot have average power" }
        CreateStrengthWorkoutCommand(source, startedAt, Duration.ofSeconds(durationSeconds))
    }
}

private fun Workout.toResponse(): WorkoutResponse = when (this) {
    is EnduranceWorkout -> WorkoutResponse(
        id = id.value.toString(),
        source = source,
        workoutKind = WorkoutKind.ENDURANCE,
        startedAt = startedAt,
        durationSeconds = duration.seconds,
        type = type,
        distanceMeters = distance.value,
        averagePowerWatts = averagePower?.value,
        averageSpeedMetersPerSecond = averageSpeedMetersPerSecond,
    )
    is StrengthWorkout -> WorkoutResponse(
        id = id.value.toString(),
        source = source,
        workoutKind = WorkoutKind.STRENGTH,
        startedAt = startedAt,
        durationSeconds = duration.seconds,
    )
}

private fun WorkoutCursor.encode(): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString("$startedAt|${id.value}".toByteArray(UTF_8))

private fun String.toWorkoutCursor(): WorkoutCursor {
    val parts = runCatching { String(Base64.getUrlDecoder().decode(this), UTF_8).split("|") }
        .getOrElse { throw IllegalArgumentException("Invalid cursor") }
    require(parts.size == 2) { "Invalid cursor" }

    return try {
        WorkoutCursor(Instant.parse(parts[0]), WorkoutId(UUID.fromString(parts[1])))
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid cursor")
    }
}

data class ApiError(val message: String)

data object WorkoutNotFoundException : RuntimeException()

@RestControllerAdvice
class WorkoutExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    fun invalidWorkout(exception: IllegalArgumentException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(ApiError(exception.message ?: "Invalid workout"))

    @ExceptionHandler(WorkoutNotFoundException::class)
    fun workoutNotFound(): ResponseEntity<Void> = ResponseEntity.notFound().build()
}
