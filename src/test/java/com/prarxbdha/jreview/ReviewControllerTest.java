package com.prarxbdha.jreview;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ModerationService service;

    @Test
    void rejectsBlankText() throws Exception {
        mvc.perform(post("/api/v1/reviews/classify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsVerdict() throws Exception {
        when(service.moderate("Broke after two days.")).thenReturn(Verdict.VERIFIED);

        mvc.perform(post("/api/v1/reviews/classify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Broke after two days.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("VERIFIED"));
    }
}
