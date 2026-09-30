package no.sondre.pacelabservice.infrastructure

import no.sondre.pacelabservice.application.workout.port.input.CreateWorkout
import no.sondre.pacelabservice.application.workout.port.input.FindWorkouts
import no.sondre.pacelabservice.application.workout.port.input.GetWorkout
import no.sondre.pacelabservice.application.workout.port.input.ImportExternalWorkouts
import no.sondre.pacelabservice.application.workout.port.input.SummarizeWorkoutHistory
import no.sondre.pacelabservice.application.workout.port.output.WorkoutRepository
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutImporter
import no.sondre.pacelabservice.application.workout.service.CreateWorkoutService
import no.sondre.pacelabservice.application.workout.service.FindWorkoutsService
import no.sondre.pacelabservice.application.workout.service.GetWorkoutService
import no.sondre.pacelabservice.application.workout.service.ImportExternalWorkoutsService
import no.sondre.pacelabservice.application.workout.service.SummarizeWorkoutHistoryService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WorkoutConfiguration {
    @Bean
    fun workoutRepository(): WorkoutRepository = InMemoryWorkoutRepository()

    @Bean
    fun createWorkout(workoutRepository: WorkoutRepository): CreateWorkout =
        CreateWorkoutService(workoutRepository)

    @Bean
    fun getWorkout(workoutRepository: WorkoutRepository): GetWorkout =
        GetWorkoutService(workoutRepository)

    @Bean
    fun findWorkouts(workoutRepository: WorkoutRepository): FindWorkouts =
        FindWorkoutsService(workoutRepository)

    @Bean
    fun summarizeWorkoutHistory(workoutRepository: WorkoutRepository): SummarizeWorkoutHistory =
        SummarizeWorkoutHistoryService(workoutRepository)

    @Bean
    fun importExternalWorkouts(
        externalWorkoutImporter: ExternalWorkoutImporter,
        workoutRepository: WorkoutRepository,
        createWorkout: CreateWorkout,
    ): ImportExternalWorkouts = ImportExternalWorkoutsService(externalWorkoutImporter, workoutRepository, createWorkout)
}
