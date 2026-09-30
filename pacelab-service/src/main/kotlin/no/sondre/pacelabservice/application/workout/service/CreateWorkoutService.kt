package no.sondre.pacelabservice.application.workout.service

import no.sondre.pacelabservice.application.workout.port.input.CreateEnduranceWorkoutCommand
import no.sondre.pacelabservice.application.workout.port.input.CreateStrengthWorkoutCommand
import no.sondre.pacelabservice.application.workout.port.input.CreateWorkout
import no.sondre.pacelabservice.application.workout.port.input.CreateWorkoutCommand
import no.sondre.pacelabservice.application.workout.port.output.WorkoutRepository
import no.sondre.pacelabservice.domain.EnduranceWorkout
import no.sondre.pacelabservice.domain.StrengthWorkout
import no.sondre.pacelabservice.domain.Workout
import no.sondre.pacelabservice.domain.WorkoutId

class CreateWorkoutService(
    private val workoutRepository: WorkoutRepository,
) : CreateWorkout {
    override fun invoke(command: CreateWorkoutCommand): Workout {
        val workout = when (command) {
            is CreateEnduranceWorkoutCommand -> EnduranceWorkout(
                id = WorkoutId.generate(),
                source = command.source,
                sourceActivityId = command.sourceActivityId,
                startedAt = command.startedAt,
                duration = command.duration,
                type = command.type,
                distance = command.distance,
                averagePower = command.averagePower,
            )
            is CreateStrengthWorkoutCommand -> StrengthWorkout(
                id = WorkoutId.generate(),
                source = command.source,
                sourceActivityId = command.sourceActivityId,
                startedAt = command.startedAt,
                duration = command.duration,
            )
        }

        workoutRepository.save(workout)
        return workout
    }
}
