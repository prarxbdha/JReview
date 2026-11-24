package com.prarxbdha.jreview;

import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Owns the moderation rules. The HTTP layer knows nothing about Gemini,
 * and the client knows nothing about what the labels mean.
 */
@Service
public class ModerationService {

    private final GeminiDecisionClient gemini;

    public ModerationService(GeminiDecisionClient gemini) {
        this.gemini = gemini;
    }

    /** The rule order is written into the question so the model applies it. */
    private static final String INSTRUCTIONS = """
            Classify this e-commerce review.
            Choose TOXIC first for harassment, hate, threats, or personal abuse.
            Then SPAM for promotions, affiliate links, giveaways, phishing, or fake content.
            Otherwise choose VERIFIED for a genuine first-hand opinion.
            A negative review is still VERIFIED if it is a real experience.
            """;

    /** Each label has a short definition; Gemini returns one of these keys. */
    private static final Map<String, String> CRITERIA = Map.of(
            "TOXIC", "Harassment, hate, threats, or personal abuse.",
            "SPAM", "Promotion, affiliate links, giveaways, phishing, or fake content.",
            "VERIFIED", "A genuine first-hand opinion. Positive or negative.");

    public Verdict moderate(String text) {
        // Gemini returns the chosen key, e.g. "TOXIC".
        String choice = gemini.choose(text, INSTRUCTIONS, CRITERIA);

        // An unexpected label becomes a 502, not a silent VERIFIED.
        return Verdict.valueOf(choice.trim().toUpperCase());
    }
}
