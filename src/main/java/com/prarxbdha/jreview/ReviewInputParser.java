package com.prarxbdha.jreview;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Turns console input into plain review strings. This is the only class that
 * knows the application has two input modes.
 */
@Component
public class ReviewInputParser {

    private static final int MAX_REVIEWS = 100;

    private final ObjectMapper json;

    public ReviewInputParser(ObjectMapper json) {
        this.json = json;
    }

    /** Custom input: one typed review, trimmed and checked. */
    public String extract(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Review text must not be blank.");
        }
        return text.trim();
    }

    /**
     * JSON multiple inputs: a JSON array of objects such as
     * [{"text":"Great product"},{"text":"Click my link!"}].
     */
    public List<String> extractMany(String jsonInput) {
        if (jsonInput == null || jsonInput.isBlank()) {
            throw new IllegalArgumentException("Paste JSON like [{\"text\":\"Your review\"}].");
        }

        JsonNode node;
        try {
            node = json.readTree(jsonInput);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("That is not valid JSON.", e);
        }

        if (!node.isArray() || node.size() == 0) {
            throw new IllegalArgumentException(
                    "JSON must be a non-empty array of objects with a \"text\" field.");
        }
        if (node.size() > MAX_REVIEWS) {
            throw new IllegalArgumentException(
                    "Please enter at most " + MAX_REVIEWS + " reviews at a time.");
        }

        List<String> reviews = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            JsonNode item = node.get(i);
            JsonNode value = item.path("text");
            if (!item.isObject() || !value.isTextual() || value.asText().isBlank()) {
                throw new IllegalArgumentException(
                        "Item " + (i + 1) + " must be an object with a non-blank \"text\" field.");
            }
            reviews.add(value.asText().trim());
        }
        return reviews;
    }
}
