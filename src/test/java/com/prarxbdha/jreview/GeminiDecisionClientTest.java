package com.prarxbdha.jreview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiDecisionClientTest {

    @Test
    void sendsDecisionRequestAndReadsChosenLabel() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("http://gemini.test/v1beta/decisions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"gemini-decision-1\"")))
                .andRespond(withSuccess("""
                        {"answers":{"verdict":{"type":"choice","choice":"SPAM"}}}
                        """, MediaType.APPLICATION_JSON));

        GeminiDecisionClient client = new GeminiDecisionClient(
                builder, "http://gemini.test", "test-key", "gemini-decision-1");

        String choice = client.choose("Buy now!", "Classify", Map.of("SPAM", "Promotion"));

        assertEquals("SPAM", choice);
        server.verify();
    }
}
