package no.sondre.pacelabservice.infrastructure.strava

import com.fasterxml.jackson.annotation.JsonProperty
import no.sondre.pacelabservice.application.workout.port.output.ExternalEnduranceWorkout
import no.sondre.pacelabservice.application.workout.port.output.ExternalStrengthWorkout
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkout
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutConnectionUnavailableException
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutImportBatch
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutImporter
import no.sondre.pacelabservice.application.workout.port.input.ExternalWorkoutConnection
import no.sondre.pacelabservice.domain.DistanceMeters
import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import no.sondre.pacelabservice.domain.PowerWatts
import no.sondre.pacelabservice.domain.SourceActivityId
import no.sondre.pacelabservice.domain.WorkoutSource
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.Instant

class StravaWorkoutImporter(
    private val restClient: RestClient,
    private val externalWorkoutConnection: ExternalWorkoutConnection,
) : ExternalWorkoutImporter {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun fetchWorkouts(): ExternalWorkoutImportBatch {
        val accessToken = try {
            externalWorkoutConnection.accessToken()
        } catch (_: IllegalArgumentException) {
            throw ExternalWorkoutConnectionUnavailableException()
        }
        val workouts = mutableListOf<ExternalWorkout>()
        var skipped = 0
        var page = 1

        while (true) {
            logger.info("Fetching workouts, page $page")
            val activities = restClient.get().uri { builder ->
                builder.path("/api/v3/athlete/activities")
                    .queryParam("page", page)
                    .queryParam("per_page", 200)
                    .build()
            }
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body(Array<StravaActivity>::class.java)
                ?.toList()
                .orEmpty()

            activities.forEach { activity ->
                activity.toWorkout()?.let(workouts::add) ?: run { skipped++ }
            }
            if (activities.size < 200) break
            page++
        }
        return ExternalWorkoutImportBatch(workouts, skipped)
    }
}

data class StravaActivity(
    val id: Long,
    @JsonProperty("sport_type")
    val sportType: String,
    @JsonProperty("start_date")
    val startDate: Instant,
    @JsonProperty("elapsed_time")
    val elapsedTime: Long,
    val distance: Double? = null,
    @JsonProperty("average_watts")
    val averageWatts: Double? = null,
)

internal fun StravaActivity.toWorkout(): ExternalWorkout? {
    val duration = runCatching { Duration.ofSeconds(elapsedTime).also { require(!it.isZero && !it.isNegative) } }.getOrNull() ?: return null
    val sourceActivityId = SourceActivityId(id.toString())
    return when (sportType) {
        "Run", "TrailRun", "VirtualRun" -> enduranceWorkout(EnduranceWorkoutType.RUNNING, sourceActivityId, duration)
        "Treadmill" -> enduranceWorkout(EnduranceWorkoutType.TREADMILL_RUNNING, sourceActivityId, duration)
        "Ride", "MountainBikeRide", "GravelRide", "EBikeRide", "EMountainBikeRide", "Velomobile" ->
            enduranceWorkout(EnduranceWorkoutType.CYCLING, sourceActivityId, duration)
        "VirtualRide", "IndoorCycling" -> enduranceWorkout(EnduranceWorkoutType.INDOOR_CYCLING, sourceActivityId, duration)
        "WeightTraining" -> ExternalStrengthWorkout(WorkoutSource.STRAVA, sourceActivityId, startDate, duration)
        else -> null
    }
}

private fun StravaActivity.enduranceWorkout(
    type: EnduranceWorkoutType,
    sourceActivityId: SourceActivityId,
    duration: Duration,
): ExternalEnduranceWorkout? {
    val validDistance = distance?.let { runCatching { DistanceMeters(it) }.getOrNull() } ?: return null
    val validPower = averageWatts?.let { runCatching { PowerWatts(it) }.getOrNull() }
    return ExternalEnduranceWorkout(WorkoutSource.STRAVA, sourceActivityId, startDate, duration, type, validDistance, validPower)
}
