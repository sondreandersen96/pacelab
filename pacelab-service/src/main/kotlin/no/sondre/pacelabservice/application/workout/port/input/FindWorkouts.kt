package no.sondre.pacelabservice.application.workout.port.input

import no.sondre.pacelabservice.application.workout.port.output.WorkoutCursor
import no.sondre.pacelabservice.application.workout.port.output.WorkoutPage
import no.sondre.pacelabservice.application.workout.port.output.WorkoutSearchCriteria

interface FindWorkouts {
    operator fun invoke(
        criteria: WorkoutSearchCriteria,
        cursor: WorkoutCursor?,
        limit: Int,
    ): WorkoutPage
}
