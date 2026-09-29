package no.sondre.pacelabservice.presentation

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class WorkoutControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `creates an endurance workout`() {
        mockMvc.perform(
            post("/workouts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "source": "MANUAL",
                      "workoutKind": "ENDURANCE",
                      "startedAt": "2026-09-29T07:00:00Z",
                      "durationSeconds": 1800,
                      "type": "RUNNING",
                      "distanceMeters": 5000
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.source").value("MANUAL"))
            .andExpect(jsonPath("$.workoutKind").value("ENDURANCE"))
            .andExpect(jsonPath("$.type").value("RUNNING"))
            .andExpect(jsonPath("$.averageSpeedMetersPerSecond").value(2.7777777777777777))
    }

    @Test
    fun `rejects endurance workout without distance`() {
        mockMvc.perform(
            post("/workouts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "source": "MANUAL",
                      "workoutKind": "ENDURANCE",
                      "startedAt": "2026-09-29T07:00:00Z",
                      "durationSeconds": 1800,
                      "type": "RUNNING"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Endurance workouts require a distance"))
    }
}
