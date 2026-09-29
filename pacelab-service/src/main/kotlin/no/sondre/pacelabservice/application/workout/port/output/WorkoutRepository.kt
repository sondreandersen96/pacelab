package no.sondre.pacelabservice.application.workout.port.output

import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import no.sondre.pacelabservice.domain.Workout
import no.sondre.pacelabservice.domain.WorkoutId
import no.sondre.pacelabservice.domain.WorkoutKind
import java.time.Instant

interface WorkoutRepository {
    fun save(workout: Workout)

    fun findById(id: WorkoutId): Workout?

    fun find(
        criteria: WorkoutSearchCriteria,
        cursor: WorkoutCursor?,
        limit: Int,
    ): WorkoutPage
}

data class WorkoutSearchCriteria(
    val startedAtFrom: Instant? = null,
    val startedAtTo: Instant? = null,
    val kind: WorkoutKind? = null,
    val enduranceType: EnduranceWorkoutType? = null,
)

data class WorkoutCursor(
    val startedAt: Instant,
    val id: WorkoutId,
)

data class WorkoutPage(
    val workouts: List<Workout>,
    val nextCursor: WorkoutCursor?,
)
