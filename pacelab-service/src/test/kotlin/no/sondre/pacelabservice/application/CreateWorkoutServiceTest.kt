package no.sondre.pacelabservice.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import no.sondre.pacelabservice.domain.DistanceMeters
import no.sondre.pacelabservice.domain.EnduranceWorkout
import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import no.sondre.pacelabservice.domain.Workout
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
    }
}
