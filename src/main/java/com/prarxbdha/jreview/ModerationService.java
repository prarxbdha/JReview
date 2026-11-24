package com.prarxbdha.jreview;

import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Owns the moderation rules. The CLI knows nothing about Ollama,
 * and the client knows nothing about what the labels mean.
 */
@Service
public class ModerationService {

    private final OllamaClient ollama;

    public ModerationService(OllamaClient ollama) {
        this.ollama = ollama;
    }

    /** The rule order is written into the system prompt so the model applies it. */
    private static final String INSTRUCTIONS = """
            Classify this e-commerce review.
            Choose TOXIC first for harassment, hate, threats, or personal abuse.
            Then SPAM for promotions, affiliate links, giveaways, phishing, or fake content.
            Otherwise choose VERIFIED for a genuine first-hand opinion.
            A negative review is still VERIFIED if it is a real experience.
            Reply with only the JSON object {"verdict":"TOXIC|SPAM|VERIFIED"}.
            """;

    /** Each label has a short definition; Ollama is constrained to these keys. */
    private static final Map<String, String> CRITERIA = Map.of(
            "TOXIC", "Harassment, hate, threats, or personal abuse.",
            "SPAM", "Promotion, affiliate links, giveaways, phishing, or fake content.",
            "VERIFIED", "A genuine first-hand opinion. Positive or negative.");

    public Verdict moderate(String text) {
        String choice = ollama.choose(text, INSTRUCTIONS, CRITERIA);
        return Verdict.valueOf(choice.trim().toUpperCase(Locale.ROOT));
    }
}
