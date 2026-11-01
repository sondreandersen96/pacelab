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
    fun `maps a detailed treadmill activity with Strava details and samples`() {
        val workout = activity("Treadmill", distance = 5_000.0, averageWatts = 210.0).toWorkout(streams())

        val enduranceWorkout = assertIs<ExternalEnduranceWorkout>(workout)
        assertEquals(EnduranceWorkoutType.TREADMILL_RUNNING, enduranceWorkout.type)
        assertEquals(210.0, enduranceWorkout.averagePower?.value)
        assertEquals("Morning run", enduranceWorkout.name)
        assertEquals(1_500, enduranceWorkout.movingTime?.seconds)
        assertEquals(35.0, enduranceWorkout.totalElevationGained?.value)
        assertEquals(2.8, enduranceWorkout.averageSpeed?.value)
        assertEquals(4.5, enduranceWorkout.maxSpeed?.value)
        assertEquals(450.0, enduranceWorkout.calories?.value)
        assertEquals("Easy effort", enduranceWorkout.description)
        assertEquals("map-1", enduranceWorkout.stravaDetails?.map?.id)
        assertEquals("detailed-polyline", enduranceWorkout.stravaDetails?.map?.polyline)
        assertEquals(3, enduranceWorkout.stravaDetails?.stravaPrCount)
        assertEquals(true, enduranceWorkout.stravaDetails?.trainer)
        assertEquals("g1", enduranceWorkout.stravaDetails?.gearId)
        assertEquals(true, enduranceWorkout.stravaDetails?.deviceWatts)
        assertEquals(true, enduranceWorkout.stravaDetails?.hasHeartrate)
        assertEquals(true, enduranceWorkout.stravaDetails?.hasPhotos)
        assertEquals(2, enduranceWorkout.samples.size)
        assertEquals(142, enduranceWorkout.samples[1].heartrate)
        assertEquals(59.912, enduranceWorkout.samples[1].location?.latitude)
    }

    @Test
    fun `maps weight training to a strength workout`() {
        assertIs<ExternalStrengthWorkout>(activity("WeightTraining").toWorkout(StravaStreams()))
    }

    @Test
    fun `skips unsupported and zero-distance activities`() {
        assertNull(activity("Hike", distance = 5_000.0).toWorkout(StravaStreams()))
        assertNull(activity("Run", distance = 0.0).toWorkout(StravaStreams()))
    }

    private fun activity(sportType: String, distance: Double? = null, averageWatts: Double? = null) = StravaDetailedActivity(
        id = 123,
        sportType = sportType,
        startDate = Instant.parse("2026-09-29T07:00:00Z"),
        elapsedTime = 1_800,
        movingTime = 1_500,
        distance = distance,
        averageWatts = averageWatts,
        totalElevationGained = 35.0,
        name = "Morning run",
        trainer = true,
        manual = false,
        gearId = "g1",
        averageSpeed = 2.8,
        maxSpeed = 4.5,
        deviceWatts = true,
        hasHeartrate = true,
        prCount = 3,
        totalPhotoCount = 1,
        description = "Easy effort",
        calories = 450.0,
        map = StravaActivityMap("map-1", "detailed-polyline"),
    )

    private fun streams() = StravaStreams(
        time = StravaStream(listOf(0, 60)),
        distance = StravaStream(listOf(0.0, 200.0)),
        latlng = StravaStream(listOf(listOf(59.911, 10.751), listOf(59.912, 10.752))),
        altitude = StravaStream(listOf(10.0, 12.0)),
        velocitySmooth = StravaStream(listOf(2.0, 3.0)),
        heartrate = StravaStream(listOf(140, 142)),
        cadence = StravaStream(listOf(80.0, 82.0)),
        watts = StravaStream(listOf(200, 210)),
        temp = StravaStream(listOf(12, 13)),
        moving = StravaStream(listOf(true, true)),
        gradeSmooth = StravaStream(listOf(1.0, 2.0)),
    )
}
