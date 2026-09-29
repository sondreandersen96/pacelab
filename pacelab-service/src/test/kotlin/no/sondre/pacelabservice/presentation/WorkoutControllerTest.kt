package no.sondre.pacelabservice.presentation

import org.junit.jupiter.api.Test
import kotlin.test.assertNotEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
class WorkoutControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
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

    @Test
    fun `retrieves a workout by id`() {
        val workout = createWorkout("STRENGTH", "2024-01-15T07:00:00Z")

        mockMvc.perform(get("/workouts/${workout.path("id").asString()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.workoutKind").value("STRENGTH"))
            .andExpect(jsonPath("$.startedAt").value("2024-01-15T07:00:00Z"))
    }

    @Test
    fun `returns not found for an unknown workout`() {
        mockMvc.perform(get("/workouts/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `filters and paginates workouts with the same start time`() {
        val startedAt = "2024-02-01T07:00:00Z"
        createWorkout("ENDURANCE", startedAt, "RUNNING")
        createWorkout("ENDURANCE", startedAt, "RUNNING")
        createWorkout("ENDURANCE", startedAt, "CYCLING")

        val firstPage = mockMvc.perform(
            get("/workouts")
                .param("from", "2024-02-01T00:00:00Z")
                .param("to", "2024-02-02T00:00:00Z")
                .param("enduranceType", "RUNNING")
                .param("limit", "1"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].type").value("RUNNING"))
            .andExpect(jsonPath("$.nextCursor").isNotEmpty)
            .andReturn()

        val cursor = objectMapper.readTree(firstPage.response.contentAsString).path("nextCursor").asString()
        val secondPage = mockMvc.perform(
            get("/workouts")
                .param("from", "2024-02-01T00:00:00Z")
                .param("to", "2024-02-02T00:00:00Z")
                .param("enduranceType", "RUNNING")
                .param("limit", "1")
                .param("cursor", cursor),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].type").value("RUNNING"))
            .andExpect(jsonPath("$.nextCursor").doesNotExist())
            .andReturn()

        val firstId = objectMapper.readTree(firstPage.response.contentAsString).path("items")[0].path("id").asString()
        val secondId = objectMapper.readTree(secondPage.response.contentAsString).path("items")[0].path("id").asString()
        assertNotEquals(firstId, secondId)
    }

    @Test
    fun `includes from boundary and excludes to boundary`() {
        createWorkout("STRENGTH", "2024-03-01T00:00:00Z")
        createWorkout("STRENGTH", "2024-03-02T00:00:00Z")

        mockMvc.perform(
            get("/workouts")
                .param("from", "2024-03-01T00:00:00Z")
                .param("to", "2024-03-02T00:00:00Z")
                .param("workoutKind", "STRENGTH"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].startedAt").value("2024-03-01T00:00:00Z"))
    }

    @Test
    fun `rejects invalid timeframe`() {
        mockMvc.perform(
            get("/workouts")
                .param("from", "2024-02-02T00:00:00Z")
                .param("to", "2024-02-01T00:00:00Z"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("from must be before to"))
    }

    private fun createWorkout(
        workoutKind: String,
        startedAt: String,
        type: String? = null,
    ) = objectMapper.readTree(
        mockMvc.perform(
            post("/workouts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    if (workoutKind == "STRENGTH") {
                        """
                        {
                          "source": "MANUAL",
                          "workoutKind": "STRENGTH",
                          "startedAt": "$startedAt",
                          "durationSeconds": 1800
                        }
                        """.trimIndent()
                    } else {
                        """
                        {
                          "source": "MANUAL",
                          "workoutKind": "ENDURANCE",
                          "startedAt": "$startedAt",
                          "durationSeconds": 1800,
                          "type": "$type",
                          "distanceMeters": 5000
                        }
                        """.trimIndent()
                    },
                ),
        ).andExpect(status().isCreated).andReturn().response.contentAsString,
    )
}
