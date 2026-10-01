package no.sondre.pacelabservice.presentation

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class CoachControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `returns the current coach history`() {
        mockMvc.perform(get("/coach/messages"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `rejects a blank coach message`() {
        mockMvc.perform(
            post("/coach/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"message":" "}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("message must not be blank"))
    }

    @Test
    fun `reports an unavailable coach when no API key is configured`() {
        mockMvc.perform(
            post("/coach/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"message":"How was my training?"}"""),
        )
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.message").value("Coach is currently unavailable"))
    }
}
