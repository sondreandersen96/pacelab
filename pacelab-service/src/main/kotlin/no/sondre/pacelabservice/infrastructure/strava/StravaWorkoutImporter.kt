package no.sondre.pacelabservice.infrastructure.strava

import com.fasterxml.jackson.annotation.JsonProperty
import no.sondre.pacelabservice.application.workout.port.output.ExternalEnduranceWorkout
import no.sondre.pacelabservice.application.workout.port.output.ExternalStrengthWorkout
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkout
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutConnection
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutConnectionUnavailableException
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutImportBatch
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutImporter
import no.sondre.pacelabservice.domain.Calories
import no.sondre.pacelabservice.domain.DistanceMeters
import no.sondre.pacelabservice.domain.ElevationMeters
import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import no.sondre.pacelabservice.domain.PowerWatts
import no.sondre.pacelabservice.domain.SourceActivityId
import no.sondre.pacelabservice.domain.SpeedMetersPerSecond
import no.sondre.pacelabservice.domain.StravaMap
import no.sondre.pacelabservice.domain.StravaWorkoutDetails
import no.sondre.pacelabservice.domain.WorkoutLocation
import no.sondre.pacelabservice.domain.WorkoutSample
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
        logger.info("Fetching up to $IMPORT_LIMIT recent Strava workouts")
        val activities = restClient.get().uri { builder ->
            builder.path("/api/v3/athlete/activities").queryParam("per_page", IMPORT_LIMIT).build()
        }.header("Authorization", "Bearer $accessToken").retrieve()
            .body(Array<StravaActivity>::class.java)?.toList().orEmpty()

        var skipped = 0
        val workouts = activities.mapNotNull { activity ->
            if (!activity.isSupported()) {
                skipped++
                null
            } else {
                val detail = getActivity(activity.id, accessToken)
                val streams = getStreams(activity.id, accessToken)
                detail.toWorkout(streams)?.also { return@mapNotNull it } ?: run { skipped++; null }
            }
        }
        return ExternalWorkoutImportBatch(workouts, skipped)
    }

    private fun getActivity(id: Long, accessToken: String): StravaDetailedActivity =
        restClient.get().uri("/api/v3/activities/{id}", id)
            .header("Authorization", "Bearer $accessToken").retrieve()
            .body(StravaDetailedActivity::class.java)!!

    private fun getStreams(id: Long, accessToken: String): StravaStreams =
        restClient.get().uri { builder ->
            builder.path("/api/v3/activities/{id}/streams")
                .queryParam("keys", STREAM_KEYS)
                .queryParam("key_by_type", true)
                .build(id)
        }.header("Authorization", "Bearer $accessToken").retrieve()
            .body(StravaStreams::class.java) ?: StravaStreams()

    private companion object {
        const val IMPORT_LIMIT = 50
        const val STREAM_KEYS = "time,distance,latlng,altitude,velocity_smooth,heartrate,cadence,watts,temp,moving,grade_smooth"
    }
}

data class StravaActivity(
    val id: Long,
    @JsonProperty("sport_type") val sportType: String,
    val distance: Double? = null,
) {
    fun isSupported(): Boolean = sportType in supportedSportTypes && (sportType == "WeightTraining" || distance != null && distance > 0)
    private companion object {
        val supportedSportTypes = setOf("Run", "TrailRun", "VirtualRun", "Treadmill", "Ride", "MountainBikeRide", "GravelRide", "EBikeRide", "EMountainBikeRide", "Velomobile", "VirtualRide", "IndoorCycling", "WeightTraining")
    }
}

data class StravaDetailedActivity(
    val id: Long,
    @JsonProperty("sport_type") val sportType: String,
    @JsonProperty("start_date") val startDate: Instant,
    @JsonProperty("elapsed_time") val elapsedTime: Long,
    @JsonProperty("moving_time") val movingTime: Long? = null,
    val distance: Double? = null,
    @JsonProperty("average_watts") val averageWatts: Double? = null,
    @JsonProperty("total_elevation_gain") val totalElevationGained: Double? = null,
    val name: String? = null,
    val trainer: Boolean = false,
    val manual: Boolean = false,
    @JsonProperty("gear_id") val gearId: String? = null,
    @JsonProperty("average_speed") val averageSpeed: Double? = null,
    @JsonProperty("max_speed") val maxSpeed: Double? = null,
    @JsonProperty("device_watts") val deviceWatts: Boolean = false,
    @JsonProperty("has_heartrate") val hasHeartrate: Boolean = false,
    @JsonProperty("pr_count") val prCount: Int = 0,
    @JsonProperty("total_photo_count") val totalPhotoCount: Int = 0,
    val description: String? = null,
    val calories: Double? = null,
    val map: StravaActivityMap? = null,
)

data class StravaActivityMap(val id: String? = null, val polyline: String? = null)

data class StravaStreams(
    val time: StravaStream? = null,
    val distance: StravaStream? = null,
    val latlng: StravaStream? = null,
    val altitude: StravaStream? = null,
    @JsonProperty("velocity_smooth") val velocitySmooth: StravaStream? = null,
    val heartrate: StravaStream? = null,
    val cadence: StravaStream? = null,
    val watts: StravaStream? = null,
    val temp: StravaStream? = null,
    val moving: StravaStream? = null,
    @JsonProperty("grade_smooth") val gradeSmooth: StravaStream? = null,
)

data class StravaStream(val data: List<Any?> = emptyList())

internal fun StravaDetailedActivity.toWorkout(streams: StravaStreams): ExternalWorkout? {
    val validElapsedTime = runCatching { Duration.ofSeconds(elapsedTime).also { require(!it.isZero && !it.isNegative) } }.getOrNull() ?: return null
    val sourceActivityId = SourceActivityId(id.toString())
    val details = StravaWorkoutDetails(
        trainer = trainer,
        manual = manual,
        gearId = gearId,
        deviceWatts = deviceWatts,
        hasHeartrate = hasHeartrate,
        hasPhotos = totalPhotoCount > 0,
        stravaPrCount = prCount.coerceAtLeast(0),
        map = map?.let { candidate -> candidate.takeIf { it.id != null || it.polyline != null }?.let { StravaMap(it.id, it.polyline) } },
    )
    val samples = streams.toSamples(validElapsedTime)
    val normalizedName = name?.takeIf(String::isNotBlank)
    val normalizedDescription = description?.takeIf(String::isNotBlank)

    return when (sportType) {
        "Run", "TrailRun", "VirtualRun" -> enduranceWorkout(EnduranceWorkoutType.RUNNING, sourceActivityId, validElapsedTime, details, samples, normalizedName, normalizedDescription)
        "Treadmill" -> enduranceWorkout(EnduranceWorkoutType.TREADMILL_RUNNING, sourceActivityId, validElapsedTime, details, samples, normalizedName, normalizedDescription)
        "Ride", "MountainBikeRide", "GravelRide", "EBikeRide", "EMountainBikeRide", "Velomobile" -> enduranceWorkout(EnduranceWorkoutType.CYCLING, sourceActivityId, validElapsedTime, details, samples, normalizedName, normalizedDescription)
        "VirtualRide", "IndoorCycling" -> enduranceWorkout(EnduranceWorkoutType.INDOOR_CYCLING, sourceActivityId, validElapsedTime, details, samples, normalizedName, normalizedDescription)
        "WeightTraining" -> ExternalStrengthWorkout(WorkoutSource.STRAVA, sourceActivityId, startDate, validElapsedTime, normalizedName, normalizedDescription, details, samples)
        else -> null
    }
}

private fun StravaDetailedActivity.enduranceWorkout(
    type: EnduranceWorkoutType,
    sourceActivityId: SourceActivityId,
    elapsedTime: Duration,
    details: StravaWorkoutDetails,
    samples: List<WorkoutSample>,
    name: String?,
    description: String?,
): ExternalEnduranceWorkout? {
    val validDistance = distance?.let { runCatching { DistanceMeters(it) }.getOrNull() } ?: return null
    return ExternalEnduranceWorkout(
        WorkoutSource.STRAVA, sourceActivityId, startDate, elapsedTime, type, validDistance,
        averageWatts?.let { runCatching { PowerWatts(it) }.getOrNull() },
        movingTime?.let { runCatching { Duration.ofSeconds(it).also { value -> require(!value.isZero && !value.isNegative && value <= elapsedTime) } }.getOrNull() },
        totalElevationGained?.let { runCatching { ElevationMeters(it) }.getOrNull() },
        averageSpeed?.let { runCatching { SpeedMetersPerSecond(it) }.getOrNull() },
        maxSpeed?.let { runCatching { SpeedMetersPerSecond(it) }.getOrNull() },
        calories?.let { runCatching { Calories(it) }.getOrNull() },
        name, description, details, samples,
    )
}

private fun StravaStreams.toSamples(elapsedTime: Duration): List<WorkoutSample> =
    time?.data.orEmpty().mapIndexedNotNull { index, rawTime ->
        val sampleTime = rawTime.asLong()?.let(Duration::ofSeconds) ?: return@mapIndexedNotNull null
        if (sampleTime.isNegative || sampleTime > elapsedTime) return@mapIndexedNotNull null
        runCatching {
            WorkoutSample(
                elapsedTime = sampleTime,
                distanceMeters = distance?.data?.getOrNull(index).asDouble(),
                location = latlng?.data?.getOrNull(index).asLocation(),
                altitudeMeters = altitude?.data?.getOrNull(index).asDouble(),
                speed = velocitySmooth?.data?.getOrNull(index).asDouble()?.let(::SpeedMetersPerSecond),
                heartrate = heartrate?.data?.getOrNull(index).asLong()?.toInt(),
                watts = watts?.data?.getOrNull(index).asDouble()?.let(::PowerWatts),
                cadence = cadence?.data?.getOrNull(index).asDouble(),
                temperatureCelsius = temp?.data?.getOrNull(index).asDouble(),
                moving = moving?.data?.getOrNull(index) as? Boolean,
                grade = gradeSmooth?.data?.getOrNull(index).asDouble(),
            )
        }.getOrNull()
    }.fold(mutableListOf()) { samples, sample ->
        if (samples.lastOrNull()?.elapsedTime == null || samples.last().elapsedTime < sample.elapsedTime) samples += sample
        samples
    }

private fun Any?.asDouble(): Double? = (this as? Number)?.toDouble()?.takeIf(Double::isFinite)
private fun Any?.asLong(): Long? = (this as? Number)?.toLong()
private fun Any?.asLocation(): WorkoutLocation? = (this as? List<*>)?.takeIf { it.size == 2 }?.let { values ->
    runCatching { WorkoutLocation(values[0].asDouble() ?: return null, values[1].asDouble() ?: return null) }.getOrNull()
}
