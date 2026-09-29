package no.sondre.pacelabservice.infrastructure

import no.sondre.pacelabservice.application.workout.port.input.CreateWorkout
import no.sondre.pacelabservice.application.workout.port.input.FindWorkouts
import no.sondre.pacelabservice.application.workout.port.input.GetWorkout
import no.sondre.pacelabservice.application.workout.port.output.WorkoutCursor
import no.sondre.pacelabservice.application.workout.port.output.WorkoutPage
import no.sondre.pacelabservice.application.workout.port.output.WorkoutRepository
import no.sondre.pacelabservice.application.workout.port.output.WorkoutSearchCriteria
import no.sondre.pacelabservice.application.workout.service.CreateWorkoutService
import no.sondre.pacelabservice.application.workout.service.FindWorkoutsService
import no.sondre.pacelabservice.application.workout.service.GetWorkoutService
import no.sondre.pacelabservice.domain.EnduranceWorkout
import no.sondre.pacelabservice.domain.Workout
import no.sondre.pacelabservice.domain.WorkoutId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryWorkoutRepository : WorkoutRepository {
    private val workouts = CopyOnWriteArrayList<Workout>()

    override fun save(workout: Workout) {
        workouts += workout
    }

    override fun findById(id: WorkoutId): Workout? = workouts.find { it.id == id }

    override fun find(
        criteria: WorkoutSearchCriteria,
        cursor: WorkoutCursor?,
        limit: Int,
    ): WorkoutPage {
        val matchingWorkouts = workouts
            .asSequence()
            .filter { workout -> criteria.startedAtFrom == null || workout.startedAt >= criteria.startedAtFrom }
            .filter { workout -> criteria.startedAtTo == null || workout.startedAt < criteria.startedAtTo }
            .filter { workout -> criteria.kind == null || workout.kind == criteria.kind }
            .filter { workout ->
                criteria.enduranceType == null ||
                    (workout is EnduranceWorkout && workout.type == criteria.enduranceType)
            }
            .sortedWith(compareByDescending<Workout> { it.startedAt }.thenByDescending { it.id.value })
            .filter { workout ->
                cursor == null ||
                    workout.startedAt < cursor.startedAt ||
                    (workout.startedAt == cursor.startedAt && workout.id.value < cursor.id.value)
            }
            .take(limit + 1)
            .toList()

        val pageWorkouts = matchingWorkouts.take(limit)
        val nextCursor = pageWorkouts.lastOrNull()
            ?.takeIf { matchingWorkouts.size > limit }
            ?.let { WorkoutCursor(it.startedAt, it.id) }

        return WorkoutPage(pageWorkouts, nextCursor)
    }
}

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
}
