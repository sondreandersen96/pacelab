package no.sondre.pacelabservice.infrastructure.coach

import no.sondre.pacelabservice.application.coach.port.input.GetCoachHistory
import no.sondre.pacelabservice.application.coach.port.input.SendCoachMessage
import no.sondre.pacelabservice.application.coach.port.output.ChatSessionRepository
import no.sondre.pacelabservice.application.coach.port.output.CoachModel
import no.sondre.pacelabservice.application.coach.port.output.CoachTool
import no.sondre.pacelabservice.application.coach.service.GetCoachHistoryService
import no.sondre.pacelabservice.application.coach.service.SendCoachMessageService
import no.sondre.pacelabservice.application.coach.tool.FindWorkoutsTool
import no.sondre.pacelabservice.application.coach.tool.GetWorkoutTool
import no.sondre.pacelabservice.application.coach.tool.WorkoutHistorySummaryTool
import no.sondre.pacelabservice.application.workout.port.input.FindWorkouts
import no.sondre.pacelabservice.application.workout.port.input.GetWorkout
import no.sondre.pacelabservice.application.workout.port.input.SummarizeWorkoutHistory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoachConfiguration {
    @Bean
    fun chatSessionRepository(): ChatSessionRepository = InMemoryChatSessionRepository()

    @Bean
    fun workoutHistorySummaryTool(summarizeWorkoutHistory: SummarizeWorkoutHistory): CoachTool =
        WorkoutHistorySummaryTool(summarizeWorkoutHistory)

    @Bean
    fun findWorkoutsTool(findWorkouts: FindWorkouts): CoachTool = FindWorkoutsTool(findWorkouts)

    @Bean
    fun getWorkoutTool(getWorkout: GetWorkout): CoachTool = GetWorkoutTool(getWorkout)

    @Bean
    fun getCoachHistory(chatSessionRepository: ChatSessionRepository): GetCoachHistory =
        GetCoachHistoryService(chatSessionRepository)

    @Bean
    fun sendCoachMessage(
        chatSessionRepository: ChatSessionRepository,
        coachModel: CoachModel,
        tools: List<CoachTool>,
    ): SendCoachMessage = SendCoachMessageService(chatSessionRepository, coachModel, tools)
}
