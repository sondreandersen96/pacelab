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

@JvmInline
value class SourceActivityId(val value: String) {
    init {
        require(value.isNotBlank()) { "Source activity id must not be blank" }
    }
}

enum class EnduranceWorkoutType {
    CYCLING,
    INDOOR_CYCLING,
    RUNNING,
    TREADMILL_RUNNING,
}

enum class WorkoutKind {
    ENDURANCE,
    STRENGTH,
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
    abstract val sourceActivityId: SourceActivityId?
    abstract val startedAt: Instant
    abstract val duration: Duration

    val kind: WorkoutKind
        get() = when (this) {
            is EnduranceWorkout -> WorkoutKind.ENDURANCE
            is StrengthWorkout -> WorkoutKind.STRENGTH
        }
}

data class EnduranceWorkout(
    override val id: WorkoutId,
    override val source: WorkoutSource,
    override val sourceActivityId: SourceActivityId? = null,
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
    override val sourceActivityId: SourceActivityId? = null,
    override val startedAt: Instant,
    override val duration: Duration,
) : Workout() {
    init {
        require(!duration.isZero && !duration.isNegative) { "Duration must be greater than zero" }
    }
}
