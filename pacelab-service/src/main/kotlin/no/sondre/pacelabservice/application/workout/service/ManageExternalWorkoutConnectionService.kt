package no.sondre.pacelabservice.application.workout.service

import no.sondre.pacelabservice.application.workout.port.input.ManageExternalWorkoutConnection
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutConnection

class ManageExternalWorkoutConnectionService(
    private val externalWorkoutConnection: ExternalWorkoutConnection,
) : ManageExternalWorkoutConnection {
    override fun authorizationUrl(state: String): String = externalWorkoutConnection.authorizationUrl(state)

    override fun completeAuthorization(code: String, grantedScope: String?) {
        externalWorkoutConnection.completeAuthorization(code, grantedScope)
    }

    override fun isConnected(): Boolean = externalWorkoutConnection.isConnected()
}
