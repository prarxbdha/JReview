package com.prarxbdha.jreview;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * The whole user interface: a terminal menu with two input modes. It owns no
 * moderation logic — that stays in ModerationService.
 */
@Component
public class ReviewCli {

    private final ModerationService service;
    private final ReviewInputParser parser;
    private final BufferedReader in;
    private final PrintStream out;

    @Autowired
    public ReviewCli(ModerationService service, ReviewInputParser parser) {
        this(service, parser, new BufferedReader(new InputStreamReader(System.in)), System.out);
    }

    /** Package-private seam so the menu can be tested without a real terminal. */
    ReviewCli(ModerationService service, ReviewInputParser parser, BufferedReader in, PrintStream out) {
        this.service = service;
        this.parser = parser;
        this.in = in;
        this.out = out;
    }

    public void run() {
        out.println("JReview — classify e-commerce reviews");
        out.println("Rules: TOXIC first, SPAM second, VERIFIED otherwise.");
        out.println("A negative but genuine review is still VERIFIED.");

        while (true) {
            out.println();
            out.println("1) Custom input");
            out.println("2) JSON multiple inputs");
            out.println("0) Exit");
            out.print("Choose an option: ");
            out.flush();

            String choice = readLine();
            if (choice == null) {
                out.println("Goodbye!");
                return;
            }

            switch (choice.trim().toLowerCase()) {
                case "1" -> classifyOne();
                case "2" -> classifyMany();
                case "0", "q", "quit", "exit" -> {
                    out.println("Goodbye!");
                    return;
                }
                default -> out.println("Please enter 1, 2, or 0.");
            }
        }
    }

    /** Mode 1: one review typed at the prompt. */
    private void classifyOne() {
        out.println();
        out.print("Review text: ");
        out.flush();
        String line = readLine();
        if (line == null) {
            return;
        }

        String review;
        try {
            review = parser.extract(line);
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
            return;
        }

        try {
            Verdict verdict = service.moderate(review);
            out.println();
            out.println("Review:  " + review);
            out.println("Verdict: " + verdict + " — " + meaning(verdict));
        } catch (RestClientException | IllegalArgumentException e) {
            reportUnavailable();
        }
    }

    /** Mode 2: a JSON array of {"text": ...} objects, one per review. */
    private void classifyMany() {
        out.println();
        out.println("Paste a JSON array of objects with a text field, for example:");
        out.println("[{\"text\":\"Great product\"},{\"text\":\"Click my link!\"}]");
        out.println("Finish with an empty line.");

        StringBuilder buffer = new StringBuilder();
        while (true) {
            String line = readLine();
            if (line == null) {
                return;
            }
            if (line.isBlank()) {
                break;
            }
            buffer.append(line).append('\n');
        }

        List<String> reviews;
        try {
            reviews = parser.extractMany(buffer.toString());
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
            return;
        }

        try {
            List<Verdict> verdicts = reviews.stream().map(service::moderate).toList();
            printResults(reviews, verdicts);
        } catch (RestClientException | IllegalArgumentException e) {
            reportUnavailable();
        }
    }

    private void printResults(List<String> reviews, List<Verdict> verdicts) {
        out.println();
        out.printf("%-4s %-10s %s%n", "#", "VERDICT", "REVIEW");
        for (int i = 0; i < reviews.size(); i++) {
            out.printf("%-4d %-10s %s%n", i + 1, verdicts.get(i), shorten(reviews.get(i), 60));
        }

        long verified = count(verdicts, Verdict.VERIFIED);
        long spam = count(verdicts, Verdict.SPAM);
        long toxic = count(verdicts, Verdict.TOXIC);
        out.println();
        out.printf("Summary: %d review(s) — VERIFIED %d, SPAM %d, TOXIC %d%n",
                verdicts.size(), verified, spam, toxic);
    }

    private long count(List<Verdict> verdicts, Verdict target) {
        return verdicts.stream().filter(verdict -> verdict == target).count();
    }

    private String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private String meaning(Verdict verdict) {
        return switch (verdict) {
            case VERIFIED -> "genuine first-hand opinion (positive or negative)";
            case SPAM -> "promotion, affiliate link, giveaway, phishing, or fake content";
            case TOXIC -> "harassment, hate, threats, or personal abuse";
        };
    }

    private void reportUnavailable() {
        out.println("Ollama is unavailable. Is 'ollama serve' running with the model pulled?");
    }

    private String readLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            out.println("Unable to read input: " + e.getMessage());
            return null;
        }
    }
}
