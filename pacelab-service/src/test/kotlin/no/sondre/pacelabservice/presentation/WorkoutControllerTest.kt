package no.sondre.pacelabservice.presentation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
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
    fun `serves generated OpenAPI documentation`() {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.paths['/workouts']").exists())
            .andExpect(jsonPath("$.paths['/workouts/{id}']").exists())
            .andExpect(jsonPath("$.paths['/workouts/summary']").exists())

        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection)
    }

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

    @Test
    fun `summarizes cycling workouts in a timeframe`() {
        createWorkout("ENDURANCE", "2026-01-01T00:00:00Z", "CYCLING", 1_800, 5_000.0)
        createWorkout("ENDURANCE", "2026-01-31T00:00:00Z", "CYCLING", 3_600, 10_000.0)
        createWorkout("ENDURANCE", "2026-01-15T00:00:00Z", "RUNNING", 1_800, 5_000.0)
        createWorkout("ENDURANCE", "2026-02-01T00:00:00Z", "CYCLING", 1_800, 5_000.0)

        mockMvc.perform(
            get("/workouts/summary")
                .param("from", "2026-01-01T00:00:00Z")
                .param("to", "2026-02-01T00:00:00Z")
                .param("workoutKind", "ENDURANCE")
                .param("enduranceType", "CYCLING"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activityCount").value(2))
            .andExpect(jsonPath("$.totalDurationSeconds").value(5_400))
            .andExpect(jsonPath("$.totalDistanceMeters").value(15_000.0))
    }

    @Test
    fun `summarizes strength workouts with zero distance`() {
        createWorkout("STRENGTH", "2026-03-01T00:00:00Z", durationSeconds = 2_700)

        mockMvc.perform(
            get("/workouts/summary")
                .param("from", "2026-03-01T00:00:00Z")
                .param("to", "2026-03-02T00:00:00Z")
                .param("workoutKind", "STRENGTH"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activityCount").value(1))
            .andExpect(jsonPath("$.totalDurationSeconds").value(2_700))
            .andExpect(jsonPath("$.totalDistanceMeters").value(0.0))
    }

    @Test
    fun `summarizes all workout history`() {
        val before = summary()
        createWorkout("STRENGTH", "2026-04-01T00:00:00Z")
        val after = summary()

        assertEquals(before.path("activityCount").asLong() + 1, after.path("activityCount").asLong())
        assertEquals(
            before.path("totalDurationSeconds").asLong() + 1_800,
            after.path("totalDurationSeconds").asLong(),
        )
        assertEquals(before.path("totalDistanceMeters").asDouble(), after.path("totalDistanceMeters").asDouble())
    }

    @Test
    fun `returns zero totals for an empty summary`() {
        mockMvc.perform(
            get("/workouts/summary")
                .param("from", "2030-01-01T00:00:00Z")
                .param("to", "2030-02-01T00:00:00Z"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activityCount").value(0))
            .andExpect(jsonPath("$.totalDurationSeconds").value(0))
            .andExpect(jsonPath("$.totalDistanceMeters").value(0.0))
    }

    @Test
    fun `rejects invalid summary timeframe`() {
        mockMvc.perform(
            get("/workouts/summary")
                .param("from", "2026-02-01T00:00:00Z")
                .param("to", "2026-01-01T00:00:00Z"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("from must be before to"))
    }

    private fun createWorkout(
        workoutKind: String,
        startedAt: String,
        type: String? = null,
        durationSeconds: Long = 1_800,
        distanceMeters: Double = 5_000.0,
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
                          "durationSeconds": $durationSeconds
                        }
                        """.trimIndent()
                    } else {
                        """
                        {
                          "source": "MANUAL",
                          "workoutKind": "ENDURANCE",
                          "startedAt": "$startedAt",
                          "durationSeconds": $durationSeconds,
                          "type": "$type",
                          "distanceMeters": $distanceMeters
                        }
                        """.trimIndent()
                    },
                ),
        ).andExpect(status().isCreated).andReturn().response.contentAsString,
    )

    private fun summary() = objectMapper.readTree(
        mockMvc.perform(get("/workouts/summary"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString,
    )
}
