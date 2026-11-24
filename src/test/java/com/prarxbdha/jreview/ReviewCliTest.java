package com.prarxbdha.jreview;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

class ReviewCliTest {

    private final ModerationService service = mock(ModerationService.class);

    @Test
    void customInputShowsTheVerdict() {
        when(service.moderate("Broke after two days.")).thenReturn(Verdict.VERIFIED);

        String output = run("1\nBroke after two days.\n0\n");

        assertTrue(output.contains("Review:  Broke after two days."));
        assertTrue(output.contains("Verdict: VERIFIED"));
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    void jsonInputShowsResultsAndSummary() {
        when(service.moderate("Great product")).thenReturn(Verdict.VERIFIED);
        when(service.moderate("Click my link!")).thenReturn(Verdict.SPAM);

        String output = run("2\n[{\"text\":\"Great product\"},{\"text\":\"Click my link!\"}]\n\n0\n");

        assertTrue(output.contains("VERDICT"));
        assertTrue(output.contains("REVIEW"));
        assertTrue(output.contains("VERIFIED"));
        assertTrue(output.contains("SPAM"));
        assertTrue(output.contains("Summary: 2 review(s) — VERIFIED 1, SPAM 1, TOXIC 0"));
    }

    @Test
    void blankCustomInputShowsAFriendlyError() {
        String output = run("1\n   \n0\n");

        assertTrue(output.contains("Error: Review text must not be blank."));
    }

    @Test
    void invalidJsonShowsAFriendlyError() {
        String output = run("2\n{bad\n\n0\n");

        assertTrue(output.contains("Error: That is not valid JSON."));
    }

    @Test
    void ollamaFailureShowsAFriendlyError() {
        when(service.moderate("Something")).thenThrow(new RestClientException("boom"));

        String output = run("1\nSomething\n0\n");

        assertTrue(output.contains("Ollama is unavailable."));
    }

    private String run(String input) {
        ReviewInputParser parser = new ReviewInputParser(new ObjectMapper());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        new ReviewCli(service, parser, new BufferedReader(new StringReader(input)), out).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
