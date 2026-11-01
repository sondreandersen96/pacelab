package no.sondre.pacelabservice.application.workout.port.input

import no.sondre.pacelabservice.domain.DistanceMeters
import no.sondre.pacelabservice.domain.Calories
import no.sondre.pacelabservice.domain.ElevationMeters
import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import no.sondre.pacelabservice.domain.PowerWatts
import no.sondre.pacelabservice.domain.SourceActivityId
import no.sondre.pacelabservice.domain.SpeedMetersPerSecond
import no.sondre.pacelabservice.domain.SourceWorkoutDetails
import no.sondre.pacelabservice.domain.Workout
import no.sondre.pacelabservice.domain.WorkoutSource
import no.sondre.pacelabservice.domain.WorkoutSample
import java.time.Duration
import java.time.Instant

interface CreateWorkout {
    operator fun invoke(command: CreateWorkoutCommand): Workout
}

sealed interface CreateWorkoutCommand {
    val source: WorkoutSource
    val sourceActivityId: SourceActivityId?
    val startedAt: Instant
    val elapsedTime: Duration
    val name: String?
    val description: String?
    val sourceDetails: SourceWorkoutDetails?
    val samples: List<WorkoutSample>
}

data class CreateEnduranceWorkoutCommand(
    override val source: WorkoutSource,
    override val sourceActivityId: SourceActivityId? = null,
    override val startedAt: Instant,
    override val elapsedTime: Duration,
    val type: EnduranceWorkoutType,
    val distance: DistanceMeters,
    val averagePower: PowerWatts? = null,
    val movingTime: Duration? = null,
    val totalElevationGained: ElevationMeters? = null,
    val averageSpeed: SpeedMetersPerSecond? = null,
    val maxSpeed: SpeedMetersPerSecond? = null,
    val calories: Calories? = null,
    override val name: String? = null,
    override val description: String? = null,
    override val sourceDetails: SourceWorkoutDetails? = null,
    override val samples: List<WorkoutSample> = emptyList(),
) : CreateWorkoutCommand

data class CreateStrengthWorkoutCommand(
    override val source: WorkoutSource,
    override val sourceActivityId: SourceActivityId? = null,
    override val startedAt: Instant,
    override val elapsedTime: Duration,
    override val name: String? = null,
    override val description: String? = null,
    override val sourceDetails: SourceWorkoutDetails? = null,
    override val samples: List<WorkoutSample> = emptyList(),
) : CreateWorkoutCommand
