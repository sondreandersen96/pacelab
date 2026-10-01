package no.sondre.pacelabservice.application.coach.tool

import no.sondre.pacelabservice.application.coach.port.output.CoachTool
import no.sondre.pacelabservice.application.coach.port.output.CoachToolDefinition
import no.sondre.pacelabservice.application.workout.port.input.FindWorkouts
import no.sondre.pacelabservice.application.workout.port.input.GetWorkout
import no.sondre.pacelabservice.application.workout.port.input.SummarizeWorkoutHistory
import no.sondre.pacelabservice.application.workout.port.output.WorkoutSearchCriteria
import no.sondre.pacelabservice.domain.EnduranceWorkout
import no.sondre.pacelabservice.domain.EnduranceWorkoutType
import no.sondre.pacelabservice.domain.Workout
import no.sondre.pacelabservice.domain.WorkoutId
import no.sondre.pacelabservice.domain.WorkoutKind
import java.time.Instant
import java.util.UUID

class WorkoutHistorySummaryTool(
    private val summarizeWorkoutHistory: SummarizeWorkoutHistory,
) : CoachTool {
    override val definition = CoachToolDefinition(
        name = "summarize_workout_history",
        description = "Summarize workout count, duration, and distance for an optional timeframe and workout type.",
        parameters = searchParameters,
    )

    override fun execute(arguments: Map<String, String?>): Map<String, Any?> = runCatching {
        val summary = summarizeWorkoutHistory(arguments.toSearchCriteria())
        mapOf(
            "activityCount" to summary.activityCount,
            "totalDurationSeconds" to summary.totalDuration.seconds,
            "totalDistanceMeters" to summary.totalDistanceMeters,
        )
    }.getOrElse(::toolError)
}

class FindWorkoutsTool(
    private val findWorkouts: FindWorkouts,
) : CoachTool {
    override val definition = CoachToolDefinition(
        name = "find_workouts",
        description = "List up to 20 workouts matching an optional timeframe and workout type.",
        parameters = searchParameters,
    )

    override fun execute(arguments: Map<String, String?>): Map<String, Any?> = runCatching {
        val workouts = findWorkouts(arguments.toSearchCriteria(), null, MAX_RESULTS).workouts
        mapOf("workouts" to workouts.map(Workout::toToolResult))
    }.getOrElse(::toolError)

    private companion object {
        const val MAX_RESULTS = 20
    }
}

class GetWorkoutTool(
    private val getWorkout: GetWorkout,
) : CoachTool {
    override val definition = CoachToolDefinition(
        name = "get_workout",
        description = "Get one workout by its UUID.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf("id" to mapOf("type" to "string", "description" to "Workout UUID")),
            "required" to listOf("id"),
            "additionalProperties" to false,
        )
    )

    override fun execute(arguments: Map<String, String?>): Map<String, Any?> = runCatching {
        val id = requireNotNull(arguments["id"]) { "id is required" }
        getWorkout(WorkoutId(UUID.fromString(id)))?.toToolResult() ?: mapOf("error" to "Workout not found")
    }.getOrElse(::toolError)
}

private val searchParameters = mapOf(
    "type" to "object",
    "properties" to mapOf(
        "from" to mapOf("type" to "string", "description" to "Inclusive ISO-8601 instant. Supply with to."),
        "to" to mapOf("type" to "string", "description" to "Exclusive ISO-8601 instant. Supply with from."),
        "workoutKind" to mapOf("type" to "string", "enum" to WorkoutKind.entries.map(WorkoutKind::name)),
        "enduranceType" to mapOf("type" to "string", "enum" to EnduranceWorkoutType.entries.map(EnduranceWorkoutType::name)),
    ),
    "additionalProperties" to false,
)

private fun Map<String, String?>.toSearchCriteria(): WorkoutSearchCriteria {
    val from = this["from"]?.let(Instant::parse)
    val to = this["to"]?.let(Instant::parse)
    require((from == null) == (to == null)) { "Both from and to must be supplied together" }
    require(from == null || from < to) { "from must be before to" }

    val kind = this["workoutKind"]?.let(WorkoutKind::valueOf)
    val enduranceType = this["enduranceType"]?.let(EnduranceWorkoutType::valueOf)
    require(kind != WorkoutKind.STRENGTH || enduranceType == null) {
        "Strength workouts cannot have an endurance type filter"
    }
    return WorkoutSearchCriteria(from, to, kind, enduranceType)
}

private fun Workout.toToolResult(): Map<String, Any?> = mapOf(
    "id" to id.value.toString(),
    "source" to source.name,
    "workoutKind" to kind.name,
    "startedAt" to startedAt.toString(),
    "durationSeconds" to duration.seconds,
    "type" to (this as? EnduranceWorkout)?.type?.name,
    "distanceMeters" to (this as? EnduranceWorkout)?.distance?.value,
    "averagePowerWatts" to (this as? EnduranceWorkout)?.averagePower?.value,
)

private fun toolError(error: Throwable): Map<String, Any?> = mapOf("error" to (error.message ?: "Invalid tool arguments"))
