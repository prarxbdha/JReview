package com.prarxbdha.jreview;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;

/**
 * Serves the tiny browser page. It reuses ModerationService, so the page
 * and the REST API follow exactly the same moderation rules.
 */
@Controller
public class FrontendController {

    private final ReviewInputParser parser;
    private final ModerationService service;

    public FrontendController(ReviewInputParser parser, ModerationService service) {
        this.parser = parser;
        this.service = service;
    }

    /** Show the empty page. */
    @GetMapping("/")
    public String page(Model model) {
        model.addAttribute("mode", "text");
        return "index";
    }

    /** Accept either form, then show the verdict or a friendly error. */
    @PostMapping("/moderate")
    public String moderate(
            @RequestParam String mode,
            @RequestParam(required = false) String text,
            @RequestParam(required = false, name = "json") String jsonInput,
            Model model) {

        // Keep the form values so the page does not forget what the user typed.
        model.addAttribute("mode", mode);
        model.addAttribute("text", text);
        model.addAttribute("json", jsonInput);

        String review;
        try {
            review = parser.extract(mode, text, jsonInput);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "index";
        }

        model.addAttribute("review", review);
        try {
            model.addAttribute("verdict", service.moderate(review));
        } catch (RestClientException | IllegalArgumentException e) {
            model.addAttribute("error", "Gemini moderation is unavailable.");
        }
        return "index";
    }
}
