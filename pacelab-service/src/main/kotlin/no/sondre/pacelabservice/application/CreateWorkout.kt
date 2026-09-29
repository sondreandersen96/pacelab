package no.sondre.pacelabservice.application

import no.sondre.pacelabservice.domain.DistanceMeters
import no.sondre.pacelabservice.domain.EnduranceWorkout
import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import no.sondre.pacelabservice.domain.PowerWatts
import no.sondre.pacelabservice.domain.StrengthWorkout
import no.sondre.pacelabservice.domain.Workout
import no.sondre.pacelabservice.domain.WorkoutId
import no.sondre.pacelabservice.domain.WorkoutSource
import java.time.Duration
import java.time.Instant

interface CreateWorkout {
    operator fun invoke(command: CreateWorkoutCommand): Workout
}

sealed interface CreateWorkoutCommand {
    val source: WorkoutSource
    val startedAt: Instant
    val duration: Duration
}

data class CreateEnduranceWorkoutCommand(
    override val source: WorkoutSource,
    override val startedAt: Instant,
    override val duration: Duration,
    val type: EnduranceWorkoutType,
    val distance: DistanceMeters,
    val averagePower: PowerWatts? = null,
) : CreateWorkoutCommand

data class CreateStrengthWorkoutCommand(
    override val source: WorkoutSource,
    override val startedAt: Instant,
    override val duration: Duration,
) : CreateWorkoutCommand

interface WorkoutRepository {
    fun save(workout: Workout)
}

class CreateWorkoutService(
    private val workoutRepository: WorkoutRepository,
) : CreateWorkout {
    override fun invoke(command: CreateWorkoutCommand): Workout {
        val workout = when (command) {
            is CreateEnduranceWorkoutCommand -> EnduranceWorkout(
                id = WorkoutId.generate(),
                source = command.source,
                startedAt = command.startedAt,
                duration = command.duration,
                type = command.type,
                distance = command.distance,
                averagePower = command.averagePower,
            )
            is CreateStrengthWorkoutCommand -> StrengthWorkout(
                id = WorkoutId.generate(),
                source = command.source,
                startedAt = command.startedAt,
                duration = command.duration,
            )
        }

        workoutRepository.save(workout)
        return workout
    }
}
