package no.sondre.pacelabservice.application.workout.port.output

import no.sondre.pacelabservice.domain.DistanceMeters
import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import no.sondre.pacelabservice.domain.PowerWatts
import no.sondre.pacelabservice.domain.SourceActivityId
import no.sondre.pacelabservice.domain.WorkoutSource
import java.time.Duration
import java.time.Instant

interface ExternalWorkoutImporter {
    fun fetchWorkouts(): ExternalWorkoutImportBatch
}

data class ExternalWorkoutImportBatch(
    val workouts: List<ExternalWorkout>,
    val skipped: Int,
)

sealed interface ExternalWorkout {
    val source: WorkoutSource
    val sourceActivityId: SourceActivityId
    val startedAt: Instant
    val duration: Duration
}

data class ExternalEnduranceWorkout(
    override val source: WorkoutSource,
    override val sourceActivityId: SourceActivityId,
    override val startedAt: Instant,
    override val duration: Duration,
    val type: EnduranceWorkoutType,
    val distance: DistanceMeters,
    val averagePower: PowerWatts?,
) : ExternalWorkout

data class ExternalStrengthWorkout(
    override val source: WorkoutSource,
    override val sourceActivityId: SourceActivityId,
    override val startedAt: Instant,
    override val duration: Duration,
) : ExternalWorkout

class ExternalWorkoutConnectionUnavailableException : RuntimeException()
