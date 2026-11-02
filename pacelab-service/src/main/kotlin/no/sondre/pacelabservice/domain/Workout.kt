package no.sondre.pacelabservice.domain

import java.time.Duration
import java.time.Instant
import java.util.UUID

@JvmInline
value class WorkoutId(val value: UUID) {
    companion object {
        fun generate() = WorkoutId(UUID.randomUUID())
    }
}

enum class WorkoutSource { MANUAL, STRAVA }

@JvmInline
value class SourceActivityId(val value: String) {
    init { require(value.isNotBlank()) { "Source activity id must not be blank" } }
}

enum class EnduranceWorkoutType { CYCLING, INDOOR_CYCLING, RUNNING, TREADMILL_RUNNING }

enum class WorkoutKind { ENDURANCE, STRENGTH }

@JvmInline
value class DistanceMeters(val value: Double) {
    init { require(value.isFinite() && value > 0) { "Distance must be greater than zero" } }
}

@JvmInline
value class PowerWatts(val value: Double) {
    init { require(value.isFinite() && value >= 0) { "Power must be non-negative" } }
}

@JvmInline
value class SpeedMetersPerSecond(val value: Double) {
    init { require(value.isFinite() && value >= 0) { "Speed must be non-negative" } }
}

@JvmInline
value class ElevationMeters(val value: Double) {
    init { require(value.isFinite() && value >= 0) { "Elevation must be non-negative" } }
}

@JvmInline
value class Calories(val value: Double) {
    init { require(value.isFinite() && value >= 0) { "Calories must be non-negative" } }
}

data class StravaMap(
    val id: String?,
    val polyline: String?,
) {
    init { require(id != null || polyline != null) { "Strava map must have an id or polyline" } }
}

sealed interface SourceWorkoutDetails

data class StravaDetails(
    val trainer: Boolean,
    val manual: Boolean,
    val gearId: String?,
    val deviceWatts: Boolean,
    val hasHeartrate: Boolean,
    val hasPhotos: Boolean,
    val stravaPrCount: Int,
    val map: StravaMap?,
) : SourceWorkoutDetails {
    init { require(stravaPrCount >= 0) { "Strava PR count must be non-negative" } }
}

data class WorkoutLocation(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }
}

data class WorkoutSample(
    val elapsedTime: Duration,
    val distanceMeters: Double? = null,
    val location: WorkoutLocation? = null,
    val altitudeMeters: Double? = null,
    val speed: SpeedMetersPerSecond? = null,
    val heartrate: Int? = null,
    val watts: PowerWatts? = null,
    val cadence: Double? = null,
    val temperatureCelsius: Double? = null,
    val moving: Boolean? = null,
    val grade: Double? = null,
) {
    init {
        require(!elapsedTime.isNegative) { "Sample elapsed time must not be negative" }
        require(distanceMeters == null || distanceMeters.isFinite() && distanceMeters >= 0) { "Sample distance must be non-negative" }
        require(altitudeMeters == null || altitudeMeters.isFinite()) { "Sample altitude must be finite" }
        require(heartrate == null || heartrate >= 0) { "Sample heartrate must be non-negative" }
        require(cadence == null || cadence.isFinite() && cadence >= 0) { "Sample cadence must be non-negative" }
        require(temperatureCelsius == null || temperatureCelsius.isFinite()) { "Sample temperature must be finite" }
        require(grade == null || grade.isFinite()) { "Sample grade must be finite" }
    }
}

sealed class Workout {
    abstract val id: WorkoutId
    abstract val source: WorkoutSource
    abstract val sourceActivityId: SourceActivityId?
    abstract val startedAt: Instant
    abstract val elapsedTime: Duration
    abstract val name: String?
    abstract val description: String?
    abstract val sourceDetails: SourceWorkoutDetails?
    abstract val samples: List<WorkoutSample>

    val kind: WorkoutKind get() = when (this) {
        is EnduranceWorkout -> WorkoutKind.ENDURANCE
        is StrengthWorkout -> WorkoutKind.STRENGTH
    }

    protected fun validateCommonFields() {
        require(!elapsedTime.isZero && !elapsedTime.isNegative) { "Elapsed time must be greater than zero" }
        require(name?.isNotBlank() != false) { "Workout name must not be blank" }
        require(description?.isNotBlank() != false) { "Workout description must not be blank" }
        require(sourceDetails !is StravaDetails || source == WorkoutSource.STRAVA) {
            "Strava details require a Strava workout"
        }
        require(samples.zipWithNext().all { (first, second) -> first.elapsedTime < second.elapsedTime }) {
            "Workout samples must be ordered by elapsed time"
        }
        require(samples.all { it.elapsedTime <= elapsedTime }) { "Workout samples must not exceed elapsed time" }
    }
}

data class EnduranceWorkout(
    override val id: WorkoutId,
    override val source: WorkoutSource,
    override val sourceActivityId: SourceActivityId? = null,
    override val startedAt: Instant,
    override val elapsedTime: Duration,
    val type: EnduranceWorkoutType,
    val distance: DistanceMeters,
    val averagePower: PowerWatts? = null,
    val movingTime: Duration? = null,
    val totalElevationGained: ElevationMeters? = null,
    val averageSpeed: SpeedMetersPerSecond? = null,
    val maxSpeed: SpeedMetersPerSecond? = null,
    val calories: Calories? = null,
    override val name: String? = null,
    override val description: String? = null,
    override val sourceDetails: SourceWorkoutDetails? = null,
    override val samples: List<WorkoutSample> = emptyList(),
) : Workout() {
    init {
        validateCommonFields()
        require(movingTime == null || !movingTime.isNegative && !movingTime.isZero && movingTime <= elapsedTime) {
            "Moving time must be greater than zero and no greater than elapsed time"
        }
    }

    val averageSpeedMetersPerSecond: Double get() = averageSpeed?.value ?: distance.value / elapsedTime.toMillis() * 1_000
}

data class StrengthWorkout(
    override val id: WorkoutId,
    override val source: WorkoutSource,
    override val sourceActivityId: SourceActivityId? = null,
    override val startedAt: Instant,
    override val elapsedTime: Duration,
    override val name: String? = null,
    override val description: String? = null,
    override val sourceDetails: SourceWorkoutDetails? = null,
    override val samples: List<WorkoutSample> = emptyList(),
) : Workout() {
    init { validateCommonFields() }
}
