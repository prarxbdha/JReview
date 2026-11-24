package com.prarxbdha.jreview;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

/**
 * Turns expected failures into short HTTP responses without stack traces.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> badRequest() {
        return ResponseEntity.badRequest().body("Request text must not be blank");
    }

    // Covers network errors, bad keys, rate limits, and malformed model labels.
    @ExceptionHandler({RestClientException.class, IllegalArgumentException.class})
    public ResponseEntity<String> badGateway() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("Gemini moderation is unavailable");
    }
}
