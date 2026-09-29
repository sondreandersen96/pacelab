package no.sondre.pacelabservice.application.workout.port.input

import no.sondre.pacelabservice.domain.DistanceMeters
import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import no.sondre.pacelabservice.domain.PowerWatts
import no.sondre.pacelabservice.domain.Workout
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
