package no.sondre.pacelabservice.infrastructure.strava

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.SessionScope
import java.time.Instant

@Component
@SessionScope
class StravaConnection {
    var accessToken: String? = null
    var refreshToken: String? = null
    var expiresAt: Instant? = null

    val isConnected: Boolean
        get() = accessToken != null && refreshToken != null && expiresAt != null

    fun clear() {
        accessToken = null
        refreshToken = null
        expiresAt = null
    }
}
