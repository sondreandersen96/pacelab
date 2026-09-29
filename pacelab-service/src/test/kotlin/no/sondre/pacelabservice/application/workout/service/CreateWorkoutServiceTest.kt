package no.sondre.pacelabservice.application.workout.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import no.sondre.pacelabservice.application.workout.port.input.CreateEnduranceWorkoutCommand
import no.sondre.pacelabservice.application.workout.port.output.WorkoutCursor
import no.sondre.pacelabservice.application.workout.port.output.WorkoutPage
import no.sondre.pacelabservice.application.workout.port.output.WorkoutRepository
import no.sondre.pacelabservice.application.workout.port.output.WorkoutSearchCriteria
import no.sondre.pacelabservice.domain.DistanceMeters
import no.sondre.pacelabservice.domain.EnduranceWorkout
import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import no.sondre.pacelabservice.domain.Workout
import no.sondre.pacelabservice.domain.WorkoutId
import no.sondre.pacelabservice.domain.WorkoutSource
import java.time.Duration
import java.time.Instant

class CreateWorkoutServiceTest {
    @Test
    fun `saves the created workout`() {
        val repository = RecordingWorkoutRepository()
        val service = CreateWorkoutService(repository)

        val created = service(
            CreateEnduranceWorkoutCommand(
                source = WorkoutSource.STRAVA,
                startedAt = Instant.parse("2026-09-29T07:00:00Z"),
                duration = Duration.ofMinutes(30),
                type = EnduranceWorkoutType.CYCLING,
                distance = DistanceMeters(15_000.0),
            ),
        )

        assertSame(created, repository.saved)
        assertEquals(WorkoutSource.STRAVA, created.source)
        assertEquals(EnduranceWorkoutType.CYCLING, (created as EnduranceWorkout).type)
    }

    private class RecordingWorkoutRepository : WorkoutRepository {
        var saved: Workout? = null

        override fun save(workout: Workout) {
            saved = workout
        }

        override fun findById(id: WorkoutId): Workout? = saved?.takeIf { it.id == id }

        override fun find(
            criteria: WorkoutSearchCriteria,
            cursor: WorkoutCursor?,
            limit: Int,
        ): WorkoutPage = WorkoutPage(emptyList(), null)
    }
}
