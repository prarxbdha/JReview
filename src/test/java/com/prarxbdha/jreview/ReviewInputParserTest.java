package com.prarxbdha.jreview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ReviewInputParserTest {

    private final ReviewInputParser parser = new ReviewInputParser(new ObjectMapper());

    @Test
    void plainTextIsTrimmedAndUsedDirectly() {
        assertEquals("Broke after two days.",
                parser.extract("text", "  Broke after two days.  ", null));
    }

    @Test
    void jsonTextFieldIsExtracted() {
        assertEquals("Broke after two days.",
                parser.extract("json", null, "{\"text\":\"Broke after two days.\"}"));
    }

    @Test
    void invalidJsonGetsAFriendlyError() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.extract("json", null, "{not json}"));
        assertTrue(error.getMessage().contains("valid JSON"));
    }

    @Test
    void missingTextFieldGetsAFriendlyError() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.extract("json", null, "{\"product\":\"phone\"}"));
        assertTrue(error.getMessage().contains("text"));
    }

    @Test
    void blankTextGetsAFriendlyError() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.extract("text", "   ", null));
        assertTrue(error.getMessage().contains("blank"));
    }
}
