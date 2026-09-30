package no.sondre.pacelabservice.application.workout.service

import no.sondre.pacelabservice.application.workout.port.input.SummarizeWorkoutHistory
import no.sondre.pacelabservice.application.workout.port.output.WorkoutHistorySummary
import no.sondre.pacelabservice.application.workout.port.output.WorkoutRepository
import no.sondre.pacelabservice.application.workout.port.output.WorkoutSearchCriteria

class SummarizeWorkoutHistoryService(
    private val workoutRepository: WorkoutRepository,
) : SummarizeWorkoutHistory {
    override fun invoke(criteria: WorkoutSearchCriteria): WorkoutHistorySummary =
        workoutRepository.summarize(criteria)
}
