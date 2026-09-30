package no.sondre.pacelabservice.infrastructure

import no.sondre.pacelabservice.application.workout.port.output.WorkoutCursor
import no.sondre.pacelabservice.application.workout.port.output.WorkoutHistorySummary
import no.sondre.pacelabservice.application.workout.port.output.WorkoutPage
import no.sondre.pacelabservice.application.workout.port.output.WorkoutRepository
import no.sondre.pacelabservice.application.workout.port.output.WorkoutSearchCriteria
import no.sondre.pacelabservice.domain.EnduranceWorkout
import no.sondre.pacelabservice.domain.Workout
import no.sondre.pacelabservice.domain.WorkoutId
import no.sondre.pacelabservice.domain.WorkoutSource
import no.sondre.pacelabservice.domain.SourceActivityId
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryWorkoutRepository : WorkoutRepository {
    private val workouts = CopyOnWriteArrayList<Workout>()

    override fun save(workout: Workout) {
        workouts += workout
    }

    override fun findById(id: WorkoutId): Workout? = workouts.find { it.id == id }

    override fun findBySourceActivityId(source: WorkoutSource, sourceActivityId: SourceActivityId): Workout? =
        workouts.find { it.source == source && it.sourceActivityId == sourceActivityId }

    override fun find(
        criteria: WorkoutSearchCriteria,
        cursor: WorkoutCursor?,
        limit: Int,
    ): WorkoutPage {
        val matchingWorkouts = matchingWorkouts(criteria)
            .sortedWith(compareByDescending<Workout> { it.startedAt }.thenByDescending { it.id.value })
            .filter { workout ->
                cursor == null ||
                    workout.startedAt < cursor.startedAt ||
                    (workout.startedAt == cursor.startedAt && workout.id.value < cursor.id.value)
            }
            .take(limit + 1)
            .toList()

        val pageWorkouts = matchingWorkouts.take(limit)
        val nextCursor = pageWorkouts.lastOrNull()
            ?.takeIf { matchingWorkouts.size > limit }
            ?.let { WorkoutCursor(it.startedAt, it.id) }

        return WorkoutPage(pageWorkouts, nextCursor)
    }

    override fun summarize(criteria: WorkoutSearchCriteria): WorkoutHistorySummary =
        matchingWorkouts(criteria)
            .fold(WorkoutHistorySummary(0, Duration.ZERO, 0.0)) { summary, workout ->
                WorkoutHistorySummary(
                    activityCount = summary.activityCount + 1,
                    totalDuration = summary.totalDuration.plus(workout.duration),
                    totalDistanceMeters = summary.totalDistanceMeters + ((workout as? EnduranceWorkout)?.distance?.value ?: 0.0),
                )
            }

    private fun matchingWorkouts(criteria: WorkoutSearchCriteria): Sequence<Workout> = workouts
        .asSequence()
        .filter { workout -> criteria.startedAtFrom == null || workout.startedAt >= criteria.startedAtFrom }
        .filter { workout -> criteria.startedAtTo == null || workout.startedAt < criteria.startedAtTo }
        .filter { workout -> criteria.kind == null || workout.kind == criteria.kind }
        .filter { workout ->
            criteria.enduranceType == null ||
                (workout is EnduranceWorkout && workout.type == criteria.enduranceType)
        }
}
