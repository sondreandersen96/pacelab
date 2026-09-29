package no.sondre.pacelabservice.presentation

import no.sondre.pacelabservice.application.CreateEnduranceWorkoutCommand
import no.sondre.pacelabservice.application.CreateStrengthWorkoutCommand
import no.sondre.pacelabservice.application.CreateWorkout
import no.sondre.pacelabservice.application.CreateWorkoutCommand
import no.sondre.pacelabservice.domain.DistanceMeters
import no.sondre.pacelabservice.domain.EnduranceWorkout
import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import no.sondre.pacelabservice.domain.PowerWatts
import no.sondre.pacelabservice.domain.StrengthWorkout
import no.sondre.pacelabservice.domain.Workout
import no.sondre.pacelabservice.domain.WorkoutSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Duration
import java.time.Instant

enum class WorkoutKind {
    ENDURANCE,
    STRENGTH,
}

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

@RestController
@RequestMapping("/workouts")
class WorkoutController(
    private val createWorkout: CreateWorkout,
) {
    @PostMapping
    fun create(@RequestBody request: CreateWorkoutRequest): ResponseEntity<WorkoutResponse> {
        val workout = createWorkout(request.toCommand())
        return ResponseEntity.status(HttpStatus.CREATED).body(workout.toResponse())
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

data class ApiError(val message: String)

@RestControllerAdvice
class WorkoutExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    fun invalidWorkout(exception: IllegalArgumentException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(ApiError(exception.message ?: "Invalid workout"))
}
