package no.sondre.pacelabservice.application.workout.port.output

interface ExternalWorkoutConnection {
    fun authorizationUrl(state: String): String

    fun completeAuthorization(code: String, grantedScope: String?)

    fun isConnected(): Boolean

    fun accessToken(): String
}
