package com.prarxbdha.jreview;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FrontendControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ModerationService service;

    @Test
    void showsBothInputChoices() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Type text")))
                .andExpect(content().string(containsString("Paste JSON")));
    }

    @Test
    void textModeReturnsVerdict() throws Exception {
        when(service.moderate("Broke after two days.")).thenReturn(Verdict.VERIFIED);

        mvc.perform(post("/moderate")
                        .param("mode", "text")
                        .param("text", "Broke after two days."))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("verdict", Verdict.VERIFIED));
    }

    @Test
    void jsonModeReturnsVerdict() throws Exception {
        when(service.moderate("Click my link!")).thenReturn(Verdict.SPAM);

        mvc.perform(post("/moderate")
                        .param("mode", "json")
                        .param("json", "{\"text\":\"Click my link!\"}"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("verdict", Verdict.SPAM));
    }

    @Test
    void invalidJsonShowsErrorOnThePage() throws Exception {
        mvc.perform(post("/moderate")
                        .param("mode", "json")
                        .param("json", "{bad"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("error"))
                .andExpect(content().string(containsString("not valid JSON")));
    }
}
