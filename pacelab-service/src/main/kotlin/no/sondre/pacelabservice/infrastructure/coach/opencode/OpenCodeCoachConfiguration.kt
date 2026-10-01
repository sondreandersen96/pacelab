package no.sondre.pacelabservice.infrastructure.coach.opencode

import no.sondre.pacelabservice.application.coach.port.output.CoachModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

data class OpenCodeProperties(
    val apiKey: String,
    val model: String,
    val baseUrl: String,
)

@Configuration
class OpenCodeCoachConfiguration {
    @Bean
    fun openCodeProperties(
        @Value("\${opencode.api-key:}") apiKey: String,
        @Value("\${opencode.model:gpt-5.6-luna}") model: String,
        @Value("\${opencode.base-url:https://opencode.ai/zen/v1}") baseUrl: String,
    ) = OpenCodeProperties(apiKey, model, baseUrl)

    @Bean
    fun coachModel(openCodeProperties: OpenCodeProperties): CoachModel =
        OpenCodeResponsesCoachModel(
            RestClient.builder().baseUrl(openCodeProperties.baseUrl).build(),
            openCodeProperties,
        )
}
