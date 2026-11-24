package com.prarxbdha.jreview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class ModerationServiceTest {

    private final GeminiDecisionClient gemini = mock(GeminiDecisionClient.class);
    private final ModerationService service = new ModerationService(gemini);

    @Test
    void mapsToxicToToxic() {
        when(gemini.choose(anyString(), anyString(), any())).thenReturn("TOXIC");
        assertEquals(Verdict.TOXIC, service.moderate("You idiots deserve this."));
    }

    @Test
    void mapsPromotionToSpam() {
        when(gemini.choose(anyString(), anyString(), any())).thenReturn("SPAM");
        assertEquals(Verdict.SPAM, service.moderate("Click my affiliate link!"));
    }

    @Test
    void negativeButGenuineReviewIsVerified() {
        when(gemini.choose(anyString(), anyString(), any())).thenReturn("VERIFIED");
        assertEquals(Verdict.VERIFIED, service.moderate("This broke after two days. Very disappointed."));
    }

    @Test
    void unknownLabelFailsInsteadOfBecomingVerified() {
        when(gemini.choose(anyString(), anyString(), any())).thenReturn("BANANA");
        assertThrows(IllegalArgumentException.class, () -> service.moderate("Unknown result"));
    }
}
