package com.prarxbdha.jreview;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Turns either input box into one plain review string.
 * This is the only class that knows the page has two modes.
 */
@Component
public class ReviewInputParser {

    private final ObjectMapper json;

    public ReviewInputParser(ObjectMapper json) {
        this.json = json;
    }

    /** Text mode uses the typed text; JSON mode extracts its "text" field. */
    public String extract(String mode, String text, String jsonInput) {
        if ("json".equals(mode)) {
            return fromJson(jsonInput);
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Review text must not be blank.");
        }
        return text.trim();
    }

    private String fromJson(String jsonInput) {
        if (jsonInput == null || jsonInput.isBlank()) {
            throw new IllegalArgumentException("Paste JSON like {\"text\":\"Your review\"}.");
        }

        JsonNode node;
        try {
            node = json.readTree(jsonInput);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("That is not valid JSON.", e);
        }

        JsonNode value = node.path("text");
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("JSON must contain a non-blank \"text\" field.");
        }
        return value.asText().trim();
    }
}
