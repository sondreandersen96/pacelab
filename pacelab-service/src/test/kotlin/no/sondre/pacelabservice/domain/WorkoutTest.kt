package no.sondre.pacelabservice.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.time.Duration
import java.time.Instant

class WorkoutTest {
    @Test
    fun `calculates average speed for endurance workout`() {
        val workout = EnduranceWorkout(
            id = WorkoutId.generate(),
            source = WorkoutSource.MANUAL,
            startedAt = Instant.parse("2026-09-29T07:00:00Z"),
            duration = Duration.ofMinutes(30),
            type = EnduranceWorkoutType.RUNNING,
            distance = DistanceMeters(5_000.0),
        )

        assertEquals(5_000.0 / 1_800, workout.averageSpeedMetersPerSecond)
    }

    @Test
    fun `rejects non-positive workout duration`() {
        assertFailsWith<IllegalArgumentException> {
            StrengthWorkout(
                id = WorkoutId.generate(),
                source = WorkoutSource.MANUAL,
                startedAt = Instant.parse("2026-09-29T07:00:00Z"),
                duration = Duration.ZERO,
            )
        }
    }
}
