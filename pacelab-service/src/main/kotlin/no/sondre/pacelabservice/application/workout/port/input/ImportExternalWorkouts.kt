package no.sondre.pacelabservice.application.workout.port.input

data class WorkoutImportResult(
    val imported: Int,
    val alreadyImported: Int,
    val skipped: Int,
)

interface ImportExternalWorkouts {
    operator fun invoke(): WorkoutImportResult
}
