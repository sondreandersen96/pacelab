package no.sondre.pacelabservice.presentation.ui

import no.sondre.pacelabservice.application.workout.port.input.ExternalWorkoutConnection
import no.sondre.pacelabservice.application.workout.port.input.ImportExternalWorkouts
import no.sondre.pacelabservice.application.workout.port.input.WorkoutImportResult
import no.sondre.pacelabservice.application.workout.port.input.FindWorkouts
import no.sondre.pacelabservice.application.workout.port.output.WorkoutSearchCriteria
import no.sondre.pacelabservice.domain.EnduranceWorkout
import no.sondre.pacelabservice.domain.Workout
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import jakarta.servlet.http.HttpSession
import java.security.SecureRandom
import java.util.Base64

@Controller
class StravaUiController(
    private val externalWorkoutConnection: ExternalWorkoutConnection,
    private val importExternalWorkouts: ImportExternalWorkouts,
    private val findWorkouts: FindWorkouts,
) {
    @GetMapping("/")
    fun index(model: Model): String {
        model.addAttribute("connected", externalWorkoutConnection.isConnected())
        model.addAttribute(
            "workouts",
            findWorkouts(WorkoutSearchCriteria(), null, 50).workouts.map(::toView),
        )
        return "index"
    }

    @GetMapping("/strava/connect")
    fun connect(session: HttpSession): String {
        val state = ByteArray(32).also(SecureRandom()::nextBytes)
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
        session.setAttribute(OAUTH_STATE, state)
        return "redirect:${externalWorkoutConnection.authorizationUrl(state)}"
    }

    @GetMapping("/strava/callback")
    fun callback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(required = false) scope: String?,
        @RequestParam(required = false) error: String?,
        session: HttpSession,
        redirectAttributes: RedirectAttributes,
    ): String {
        val expectedState = session.getAttribute(OAUTH_STATE) as? String
        session.removeAttribute(OAUTH_STATE)
        if (error != null || code == null || state == null || state != expectedState) {
            redirectAttributes.addFlashAttribute("error", "Strava authorization was cancelled or could not be verified")
            return "redirect:/"
        }

        return runCatching { externalWorkoutConnection.completeAuthorization(code, scope) }
            .fold(
                onSuccess = {
                    redirectAttributes.addFlashAttribute("message", "Strava connected. Import activities when ready.")
                    "redirect:/"
                },
                onFailure = {
                    redirectAttributes.addFlashAttribute("error", it.message ?: "Could not connect to Strava")
                    "redirect:/"
                },
            )
    }

    @PostMapping("/strava/import")
    fun importWorkouts(redirectAttributes: RedirectAttributes): String =
        runCatching { importExternalWorkouts() }
            .fold(
                onSuccess = { result ->
                    redirectAttributes.addFlashAttribute("message", result.toMessage())
                    "redirect:/"
                },
                onFailure = {
                    redirectAttributes.addFlashAttribute("error", it.message ?: "Could not import Strava activities")
                    "redirect:/"
                },
            )

    private fun WorkoutImportResult.toMessage() =
        "Imported $imported activities; $alreadyImported already imported; $skipped skipped."

    private fun toView(workout: Workout) = WorkoutView(
        source = workout.source.name,
        startedAt = workout.startedAt.toString(),
        type = (workout as? EnduranceWorkout)?.type?.name ?: "STRENGTH",
        durationMinutes = workout.duration.toMinutes(),
        distanceKilometers = (workout as? EnduranceWorkout)?.distance?.value?.div(1_000),
    )

    companion object {
        private const val OAUTH_STATE = "strava.oauth.state"
    }
}

data class WorkoutView(
    val source: String,
    val startedAt: String,
    val type: String,
    val durationMinutes: Long,
    val distanceKilometers: Double?,
)
