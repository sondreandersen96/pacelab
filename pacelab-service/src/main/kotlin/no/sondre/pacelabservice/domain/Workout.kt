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

enum class WorkoutSource {
    MANUAL,
    STRAVA,
}

enum class EnduranceWorkoutType {
    CYCLING,
    INDOOR_CYCLING,
    RUNNING,
    TREADMILL_RUNNING,
}

@JvmInline
value class DistanceMeters(val value: Double) {
    init {
        require(value.isFinite() && value > 0) { "Distance must be greater than zero" }
    }
}

@JvmInline
value class PowerWatts(val value: Double) {
    init {
        require(value.isFinite() && value >= 0) { "Average power must be non-negative" }
    }
}

sealed class Workout {
    abstract val id: WorkoutId
    abstract val source: WorkoutSource
    abstract val startedAt: Instant
    abstract val duration: Duration
}

data class EnduranceWorkout(
    override val id: WorkoutId,
    override val source: WorkoutSource,
    override val startedAt: Instant,
    override val duration: Duration,
    val type: EnduranceWorkoutType,
    val distance: DistanceMeters,
    val averagePower: PowerWatts? = null,
) : Workout() {
    init {
        require(!duration.isZero && !duration.isNegative) { "Duration must be greater than zero" }
    }

    val averageSpeedMetersPerSecond: Double
        get() = distance.value / duration.toMillis() * 1_000
}

data class StrengthWorkout(
    override val id: WorkoutId,
    override val source: WorkoutSource,
    override val startedAt: Instant,
    override val duration: Duration,
) : Workout() {
    init {
        require(!duration.isZero && !duration.isNegative) { "Duration must be greater than zero" }
    }
}
