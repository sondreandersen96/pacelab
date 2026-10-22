package no.sondre.pacelabservice.application.workout.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutConnection

class ManageExternalWorkoutConnectionServiceTest {
    @Test
    fun `delegates the browser connection flow to the external connection`() {
        val connection = RecordingConnection()
        val service = ManageExternalWorkoutConnectionService(connection)

        assertEquals("https://example.test/authorize?state=state", service.authorizationUrl("state"))
        service.completeAuthorization("code", "activity:read_all")

        assertEquals("code", connection.code)
        assertEquals("activity:read_all", connection.scope)
        assertTrue(service.isConnected())
    }

    private class RecordingConnection : ExternalWorkoutConnection {
        var code: String? = null
        var scope: String? = null

        override fun authorizationUrl(state: String) = "https://example.test/authorize?state=$state"

        override fun completeAuthorization(code: String, grantedScope: String?) {
            this.code = code
            scope = grantedScope
        }

        override fun isConnected() = true

        override fun accessToken() = "token"
    }
}
