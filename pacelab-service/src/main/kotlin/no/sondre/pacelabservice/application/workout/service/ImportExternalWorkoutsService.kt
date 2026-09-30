package no.sondre.pacelabservice.application.workout.service

import no.sondre.pacelabservice.application.workout.port.input.CreateEnduranceWorkoutCommand
import no.sondre.pacelabservice.application.workout.port.input.CreateStrengthWorkoutCommand
import no.sondre.pacelabservice.application.workout.port.input.CreateWorkout
import no.sondre.pacelabservice.application.workout.port.input.ImportExternalWorkouts
import no.sondre.pacelabservice.application.workout.port.input.WorkoutImportResult
import no.sondre.pacelabservice.application.workout.port.output.ExternalEnduranceWorkout
import no.sondre.pacelabservice.application.workout.port.output.ExternalStrengthWorkout
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutImporter
import no.sondre.pacelabservice.application.workout.port.output.WorkoutRepository

class ImportExternalWorkoutsService(
    private val externalWorkoutImporter: ExternalWorkoutImporter,
    private val workoutRepository: WorkoutRepository,
    private val createWorkout: CreateWorkout,
) : ImportExternalWorkouts {
    override fun invoke(): WorkoutImportResult {
        val batch = externalWorkoutImporter.fetchWorkouts()
        var imported = 0
        var alreadyImported = 0

        batch.workouts.forEach { workout ->
            if (workoutRepository.findBySourceActivityId(workout.source, workout.sourceActivityId) != null) {
                alreadyImported++
                return@forEach
            }

            when (workout) {
                is ExternalEnduranceWorkout -> createWorkout(
                    CreateEnduranceWorkoutCommand(
                        source = workout.source,
                        sourceActivityId = workout.sourceActivityId,
                        startedAt = workout.startedAt,
                        duration = workout.duration,
                        type = workout.type,
                        distance = workout.distance,
                        averagePower = workout.averagePower,
                    ),
                )
                is ExternalStrengthWorkout -> createWorkout(
                    CreateStrengthWorkoutCommand(
                        source = workout.source,
                        sourceActivityId = workout.sourceActivityId,
                        startedAt = workout.startedAt,
                        duration = workout.duration,
                    ),
                )
            }
            imported++
        }

        return WorkoutImportResult(imported, alreadyImported, batch.skipped)
    }
}
