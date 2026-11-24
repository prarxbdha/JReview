package com.prarxbdha.jreview;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The only class that talks to Ollama. It asks a local model for one label and
 * returns that label as a plain string. No API key and no network egress.
 */
@Component
public class OllamaClient {

    private final RestClient http;
    private final ObjectMapper json;
    private final String model;
    private final double temperature;
    private final int numCtx;

    public OllamaClient(
            RestClient.Builder builder,
            ObjectMapper json,
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String model,
            @Value("${ollama.temperature:0}") double temperature,
            @Value("${ollama.num-ctx:1024}") int numCtx) {
        this.http = builder.baseUrl(baseUrl).build();
        this.json = json;
        this.model = model;
        this.temperature = temperature;
        this.numCtx = numCtx;
    }

    /** One chat request with a fixed three-choice JSON answer. */
    public String choose(String input, String instructions, Map<String, String> criteria) {
        Map<String, Object> format = Map.of(
                "type", "object",
                "properties", Map.of("verdict", Map.of(
                        "type", "string",
                        "enum", List.of("VERIFIED", "SPAM", "TOXIC"))),
                "required", List.of("verdict"));

        Map<String, Object> options = Map.of(
                "temperature", temperature,
                "num_ctx", numCtx);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", instructions + rules(criteria)),
                        Map.of("role", "user", "content", input)),
                "stream", false,
                "think", false,
                "format", format,
                "options", options);

        JsonNode response = http.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalArgumentException("Empty Ollama response");
        }

        String content = response.path("message").path("content").asText();
        if (content.isBlank()) {
            throw new IllegalArgumentException("Ollama returned an empty answer");
        }

        try {
            String verdict = json.readTree(content).path("verdict").asText();
            if (verdict.isBlank()) {
                throw new IllegalArgumentException("Ollama answer has no verdict");
            }
            return verdict;
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Ollama returned a malformed answer", e);
        }
    }

    private String rules(Map<String, String> criteria) {
        StringBuilder text = new StringBuilder("\n\nRules:\n");
        criteria.forEach((label, description) ->
                text.append("- ").append(label).append(": ").append(description).append('\n'));
        return text.toString();
    }
}
