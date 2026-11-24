package com.prarxbdha.jreview;

import tools.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * The only class that talks to Gemini. It sends one typed decision request
 * and returns the chosen label as a plain string.
 */
@Component
public class GeminiDecisionClient {

    private final RestClient http;
    private final String model;

    public GeminiDecisionClient(
            RestClient.Builder builder,
            @Value("${gemini.api.base-url}") String baseUrl,
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.api.model}") String model) {
        this.model = model;
        // The key is a header, never part of the URL or the JSON body.
        this.http = builder
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    public String choose(String input, String instructions, Map<String, String> criteria) {
        // A decision request is: shared input + one question with fixed options.
        Map<String, Object> body = Map.of(
                "model", model,
                "input", input,
                "questions", Map.of(
                        "verdict", Map.of(
                                "type", "choice",
                                "instructions", instructions,
                                "criteria", criteria)));

        JsonNode response = http.post()
                .uri("/v1beta/decisions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalArgumentException("Empty Gemini response");
        }

        // Gemini Decision 1 returns a label here, not generated text.
        return response.path("answers").path("verdict").path("choice").asText();
    }
}
