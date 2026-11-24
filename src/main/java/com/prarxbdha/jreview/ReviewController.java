package com.prarxbdha.jreview;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP layer. It only reads JSON, validates it, and returns the result.
 */
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ModerationService service;

    public ReviewController(ModerationService service) {
        this.service = service;
    }

    /** The client sends only the text to classify. */
    public record ReviewRequest(@NotBlank String text) {}

    /** The client receives only the final decision. */
    public record ReviewResponse(Verdict verdict) {}

    @PostMapping("/classify")
    public ReviewResponse classify(@Valid @RequestBody ReviewRequest request) {
        // @Valid rejects blank text before it ever reaches Gemini.
        return new ReviewResponse(service.moderate(request.text()));
    }
}
