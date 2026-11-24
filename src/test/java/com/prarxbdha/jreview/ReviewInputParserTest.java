package com.prarxbdha.jreview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ReviewInputParserTest {

    private final ReviewInputParser parser = new ReviewInputParser(new ObjectMapper());

    @Test
    void customTextIsTrimmedAndUsedDirectly() {
        assertEquals("Broke after two days.", parser.extract("  Broke after two days.  "));
    }

    @Test
    void blankCustomTextGetsAFriendlyError() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.extract("   "));
        assertTrue(error.getMessage().contains("blank"));
    }

    @Test
    void jsonArrayIsExtractedInOrder() {
        String json = """
                [
                  {"text": " Great product "},
                  {"text": "Click my link!"}
                ]
                """;

        assertEquals(List.of("Great product", "Click my link!"), parser.extractMany(json));
    }

    @Test
    void invalidJsonGetsAFriendlyError() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.extractMany("{not json}"));
        assertTrue(error.getMessage().contains("valid JSON"));
    }

    @Test
    void nonArrayJsonGetsAFriendlyError() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.extractMany("{\"text\":\"One review\"}"));
        assertTrue(error.getMessage().contains("array"));
    }

    @Test
    void emptyArrayGetsAFriendlyError() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.extractMany("[]"));
        assertTrue(error.getMessage().contains("non-empty"));
    }

    @Test
    void missingTextFieldGetsAFriendlyError() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.extractMany("[{\"product\":\"phone\"}]"));
        assertTrue(error.getMessage().contains("Item 1"));
    }

    @Test
    void blankTextFieldGetsAFriendlyError() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.extractMany("[{\"text\":\"  \"}]"));
        assertTrue(error.getMessage().contains("Item 1"));
    }

    @Test
    void tooManyReviewsGetsAFriendlyError() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < 101; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"text\":\"Review ").append(i).append("\"}");
        }
        json.append(']');

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.extractMany(json.toString()));
        assertTrue(error.getMessage().contains("100"));
    }
}
