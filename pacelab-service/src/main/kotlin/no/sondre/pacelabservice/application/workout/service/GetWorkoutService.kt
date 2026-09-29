package no.sondre.pacelabservice.application.workout.service

import no.sondre.pacelabservice.application.workout.port.input.GetWorkout
import no.sondre.pacelabservice.application.workout.port.output.WorkoutRepository
import no.sondre.pacelabservice.domain.Workout
import no.sondre.pacelabservice.domain.WorkoutId

class GetWorkoutService(
    private val workoutRepository: WorkoutRepository,
) : GetWorkout {
    override fun invoke(id: WorkoutId): Workout? = workoutRepository.findById(id)
}
