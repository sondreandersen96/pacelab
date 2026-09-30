package no.sondre.pacelabservice.infrastructure.strava

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import no.sondre.pacelabservice.application.workout.port.output.ExternalEnduranceWorkout
import no.sondre.pacelabservice.application.workout.port.output.ExternalStrengthWorkout
import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import java.time.Instant

class StravaWorkoutImporterTest {
    @Test
    fun `maps a treadmill activity to an endurance workout`() {
        val workout = activity("Treadmill", distance = 5_000.0, averageWatts = 210.0).toWorkout()

        val enduranceWorkout = assertIs<ExternalEnduranceWorkout>(workout)
        assertEquals(EnduranceWorkoutType.TREADMILL_RUNNING, enduranceWorkout.type)
        assertEquals(210.0, enduranceWorkout.averagePower?.value)
    }

    @Test
    fun `maps weight training to a strength workout`() {
        assertIs<ExternalStrengthWorkout>(activity("WeightTraining").toWorkout())
    }

    @Test
    fun `skips unsupported and zero-distance activities`() {
        assertNull(activity("Hike", distance = 5_000.0).toWorkout())
        assertNull(activity("Run", distance = 0.0).toWorkout())
    }

    private fun activity(sportType: String, distance: Double? = null, averageWatts: Double? = null) = StravaActivity(
        id = 123,
        sportType = sportType,
        startDate = Instant.parse("2026-09-29T07:00:00Z"),
        elapsedTime = 1_800,
        distance = distance,
        averageWatts = averageWatts,
    )
}
