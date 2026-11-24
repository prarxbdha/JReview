package com.prarxbdha.jreview;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class OllamaClientTest {

    @Test
    void sendsChatRequestAndReadsTheVerdict() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://ollama.test/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"model\":\"qwen3.5:4b\"")))
                .andExpect(content().string(containsString("\"stream\":false")))
                .andExpect(content().string(containsString("\"think\":false")))
                .andExpect(content().string(containsString(
                        "\"enum\":[\"VERIFIED\",\"SPAM\",\"TOXIC\"]")))
                .andRespond(withSuccess(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"verdict\\\":\\\"SPAM\\\"}\"},\"done\":true}",
                        MediaType.APPLICATION_JSON));

        OllamaClient client = new OllamaClient(
                builder, new ObjectMapper(), "http://ollama.test", "qwen3.5:4b", 0, 1024);

        assertEquals("SPAM", client.choose("Buy now!", "Classify", Map.of("SPAM", "Promotion")));
        server.verify();
    }

    @Test
    void malformedAnswerIsRejected() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://ollama.test/api/chat"))
                .andRespond(withSuccess(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\"not json\"},\"done\":true}",
                        MediaType.APPLICATION_JSON));

        OllamaClient client = new OllamaClient(
                builder, new ObjectMapper(), "http://ollama.test", "qwen3.5:4b", 0, 1024);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> client.choose("Hello", "Classify", Map.of("VERIFIED", "Genuine")));
        assertTrue(error.getMessage().contains("malformed"));
        server.verify();
    }
}
