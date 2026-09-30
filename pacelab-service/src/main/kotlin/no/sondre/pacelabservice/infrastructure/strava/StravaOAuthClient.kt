package no.sondre.pacelabservice.infrastructure.strava

import com.fasterxml.jackson.annotation.JsonProperty
import no.sondre.pacelabservice.application.workout.port.input.ExternalWorkoutConnection
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant

class StravaOAuthClient(
    private val restClient: RestClient,
    private val properties: StravaProperties,
    private val connection: StravaConnection,
) : ExternalWorkoutConnection {
    override fun authorizationUrl(state: String): String {
        require(properties.clientId.isNotBlank() && properties.clientSecret.isNotBlank()) {
            "Strava client credentials are not configured"
        }
        return "https://www.strava.com/oauth/authorize?client_id=${encode(properties.clientId)}" +
            "&response_type=code&redirect_uri=${encode(properties.redirectUri)}" +
            "&approval_prompt=auto&scope=activity:read_all&state=${encode(state)}"
    }

    override fun completeAuthorization(code: String, grantedScope: String?) {
        val response = tokenRequest(
            linkedMapOf(
                "client_id" to properties.clientId,
                "client_secret" to properties.clientSecret,
                "code" to code,
                "grant_type" to "authorization_code",
            ),
        )
        require(grantedScope?.split(',')?.contains("activity:read_all") == true) {
            "Strava authorization must grant activity:read_all"
        }
        updateConnection(response)
    }

    override fun accessToken(): String {
        require(connection.isConnected) { "Connect Strava before importing activities" }
        if (connection.expiresAt!!.isAfter(Instant.now().plusSeconds(60))) return connection.accessToken!!

        val response = tokenRequest(
            linkedMapOf(
                "client_id" to properties.clientId,
                "client_secret" to properties.clientSecret,
                "refresh_token" to connection.refreshToken!!,
                "grant_type" to "refresh_token",
            ),
        )
        updateConnection(response)
        return connection.accessToken!!
    }

    override fun isConnected(): Boolean = connection.isConnected

    private fun tokenRequest(values: Map<String, String>): StravaTokenResponse {
        require(properties.clientId.isNotBlank() && properties.clientSecret.isNotBlank()) {
            "Strava client credentials are not configured"
        }
        val body = LinkedMultiValueMap<String, String>().apply { values.forEach(::add) }
        return restClient.post().uri("/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(StravaTokenResponse::class.java)!!
    }

    private fun updateConnection(response: StravaTokenResponse) {
        connection.accessToken = response.accessToken
        connection.refreshToken = response.refreshToken
        connection.expiresAt = Instant.ofEpochSecond(response.expiresAt)
    }

    private fun encode(value: String): String = URLEncoder.encode(value, UTF_8)
}

data class StravaTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("refresh_token")
    val refreshToken: String,
    @JsonProperty("expires_at")
    val expiresAt: Long,
)
