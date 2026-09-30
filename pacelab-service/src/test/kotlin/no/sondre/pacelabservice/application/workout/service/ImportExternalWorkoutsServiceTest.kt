package no.sondre.pacelabservice.application.workout.service

import kotlin.test.Test
import kotlin.test.assertEquals
import no.sondre.pacelabservice.application.workout.port.input.CreateWorkout
import no.sondre.pacelabservice.application.workout.port.input.CreateWorkoutCommand
import no.sondre.pacelabservice.application.workout.port.output.ExternalEnduranceWorkout
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutImportBatch
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutImporter
import no.sondre.pacelabservice.application.workout.port.output.WorkoutCursor
import no.sondre.pacelabservice.application.workout.port.output.WorkoutHistorySummary
import no.sondre.pacelabservice.application.workout.port.output.WorkoutPage
import no.sondre.pacelabservice.application.workout.port.output.WorkoutRepository
import no.sondre.pacelabservice.application.workout.port.output.WorkoutSearchCriteria
import no.sondre.pacelabservice.domain.DistanceMeters
import no.sondre.pacelabservice.domain.EnduranceWorkout
import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import no.sondre.pacelabservice.domain.SourceActivityId
import no.sondre.pacelabservice.domain.Workout
import no.sondre.pacelabservice.domain.WorkoutId
import no.sondre.pacelabservice.domain.WorkoutSource
import java.time.Duration
import java.time.Instant

class ImportExternalWorkoutsServiceTest {
    @Test
    fun `imports new workouts and ignores previously imported activities`() {
        val sourceActivityId = SourceActivityId("123")
        val repository = InMemoryRepository().apply {
            save(EnduranceWorkout(WorkoutId.generate(), WorkoutSource.STRAVA, sourceActivityId, Instant.EPOCH, Duration.ofMinutes(30), EnduranceWorkoutType.RUNNING, DistanceMeters(5_000.0)))
        }
        val createWorkout = CreateWorkoutService(repository)
        val importer = object : ExternalWorkoutImporter {
            override fun fetchWorkouts() = ExternalWorkoutImportBatch(
                listOf(
                    ExternalEnduranceWorkout(WorkoutSource.STRAVA, sourceActivityId, Instant.EPOCH, Duration.ofMinutes(30), EnduranceWorkoutType.RUNNING, DistanceMeters(5_000.0), null),
                    ExternalEnduranceWorkout(WorkoutSource.STRAVA, SourceActivityId("124"), Instant.EPOCH, Duration.ofMinutes(45), EnduranceWorkoutType.CYCLING, DistanceMeters(20_000.0), null),
                ),
                skipped = 3,
            )
        }

        val result = ImportExternalWorkoutsService(importer, repository, createWorkout)()

        assertEquals(1, result.imported)
        assertEquals(1, result.alreadyImported)
        assertEquals(3, result.skipped)
        assertEquals(2, repository.workouts.size)
    }

    private class InMemoryRepository : WorkoutRepository {
        val workouts = mutableListOf<Workout>()

        override fun save(workout: Workout) { workouts += workout }
        override fun findById(id: WorkoutId): Workout? = workouts.find { it.id == id }
        override fun findBySourceActivityId(source: WorkoutSource, sourceActivityId: SourceActivityId): Workout? =
            workouts.find { it.source == source && it.sourceActivityId == sourceActivityId }
        override fun find(criteria: WorkoutSearchCriteria, cursor: WorkoutCursor?, limit: Int): WorkoutPage = WorkoutPage(emptyList(), null)
        override fun summarize(criteria: WorkoutSearchCriteria): WorkoutHistorySummary = WorkoutHistorySummary(0, Duration.ZERO, 0.0)
    }
}
