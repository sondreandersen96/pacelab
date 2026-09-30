package no.sondre.pacelabservice.application.workout.port.input

import no.sondre.pacelabservice.application.workout.port.output.WorkoutHistorySummary
import no.sondre.pacelabservice.application.workout.port.output.WorkoutSearchCriteria

interface SummarizeWorkoutHistory {
    operator fun invoke(criteria: WorkoutSearchCriteria): WorkoutHistorySummary
}
