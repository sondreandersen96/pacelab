package no.sondre.pacelabservice.infrastructure

import no.sondre.pacelabservice.application.CreateWorkout
import no.sondre.pacelabservice.application.CreateWorkoutService
import no.sondre.pacelabservice.application.WorkoutRepository
import no.sondre.pacelabservice.domain.Workout
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryWorkoutRepository : WorkoutRepository {
    private val workouts = CopyOnWriteArrayList<Workout>()

    override fun save(workout: Workout) {
        workouts += workout
    }
}

@Configuration
class WorkoutConfiguration {
    @Bean
    fun workoutRepository(): WorkoutRepository = InMemoryWorkoutRepository()

    @Bean
    fun createWorkout(workoutRepository: WorkoutRepository): CreateWorkout =
        CreateWorkoutService(workoutRepository)
}
