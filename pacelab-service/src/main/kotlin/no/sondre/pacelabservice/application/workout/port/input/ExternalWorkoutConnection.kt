package no.sondre.pacelabservice.application.workout.port.input

interface ExternalWorkoutConnection {
    fun authorizationUrl(state: String): String

    fun completeAuthorization(code: String, grantedScope: String?)

    fun isConnected(): Boolean

    fun accessToken(): String
}
