package no.sondre.pacelabservice.application.workout.port.input

import no.sondre.pacelabservice.domain.Workout
import no.sondre.pacelabservice.domain.WorkoutId

interface GetWorkout {
    operator fun invoke(id: WorkoutId): Workout?
}
