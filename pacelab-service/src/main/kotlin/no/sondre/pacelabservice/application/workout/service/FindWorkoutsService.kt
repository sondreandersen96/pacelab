package no.sondre.pacelabservice.application.workout.service

import no.sondre.pacelabservice.application.workout.port.input.FindWorkouts
import no.sondre.pacelabservice.application.workout.port.output.WorkoutCursor
import no.sondre.pacelabservice.application.workout.port.output.WorkoutPage
import no.sondre.pacelabservice.application.workout.port.output.WorkoutRepository
import no.sondre.pacelabservice.application.workout.port.output.WorkoutSearchCriteria

class FindWorkoutsService(
    private val workoutRepository: WorkoutRepository,
) : FindWorkouts {
    override fun invoke(
        criteria: WorkoutSearchCriteria,
        cursor: WorkoutCursor?,
        limit: Int,
    ): WorkoutPage = workoutRepository.find(criteria, cursor, limit)
}
