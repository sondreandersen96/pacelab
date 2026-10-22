package no.sondre.pacelabservice.application.workout.port.input

interface ManageExternalWorkoutConnection {
    fun authorizationUrl(state: String): String

    fun completeAuthorization(code: String, grantedScope: String?)

    fun isConnected(): Boolean
}
