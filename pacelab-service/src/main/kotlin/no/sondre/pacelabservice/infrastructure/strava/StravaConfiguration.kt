package no.sondre.pacelabservice.infrastructure.strava

import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutImporter
import no.sondre.pacelabservice.application.workout.port.output.ExternalWorkoutConnection
import no.sondre.pacelabservice.application.workout.port.input.ManageExternalWorkoutConnection
import no.sondre.pacelabservice.application.workout.service.ManageExternalWorkoutConnectionService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

data class StravaProperties(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
)

@Configuration
class StravaConfiguration {
    @Bean
    fun stravaProperties(
        @Value("\${strava.client-id:}") clientId: String,
        @Value("\${strava.client-secret:}") clientSecret: String,
        @Value("\${strava.redirect-uri:http://localhost:8080/strava/callback}") redirectUri: String,
    ) = StravaProperties(clientId, clientSecret, redirectUri)

    @Bean
    fun stravaRestClient(): RestClient =
        RestClient.builder().baseUrl("https://www.strava.com").build()

    @Bean
    fun externalWorkoutConnection(
        stravaRestClient: RestClient,
        stravaProperties: StravaProperties,
        stravaConnection: StravaConnection,
    ): ExternalWorkoutConnection = StravaOAuthClient(stravaRestClient, stravaProperties, stravaConnection)

    @Bean
    fun manageExternalWorkoutConnection(
        externalWorkoutConnection: ExternalWorkoutConnection,
    ): ManageExternalWorkoutConnection = ManageExternalWorkoutConnectionService(externalWorkoutConnection)

    @Bean
    fun externalWorkoutImporter(
        stravaRestClient: RestClient,
        externalWorkoutConnection: ExternalWorkoutConnection,
    ): ExternalWorkoutImporter = StravaWorkoutImporter(stravaRestClient, externalWorkoutConnection)
}
