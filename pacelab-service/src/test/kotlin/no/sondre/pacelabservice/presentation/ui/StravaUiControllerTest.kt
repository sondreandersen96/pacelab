package no.sondre.pacelabservice.presentation.ui

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class StravaUiControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `serves the Strava connection page`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Connect Strava")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Ask your coach")))
    }

    @Test
    fun `returns coach validation feedback to the page`() {
        mockMvc.perform(post("/coach/ask").param("message", " "))
            .andExpect(status().is3xxRedirection)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("coachError", "Ask the coach a question first."))
    }
}
