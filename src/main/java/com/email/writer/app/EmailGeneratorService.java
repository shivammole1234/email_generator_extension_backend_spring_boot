package com.email.writer.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EmailGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(EmailGeneratorService.class);
    private final WebClient webClient;


    // Hardcoded URL and API Key
    @Value("${url}")
    private String geminiApiUrl;
    @Value("${api-key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @PostConstruct
    private void validateConfiguration() {
        if (geminiApiUrl == null || geminiApiKey == null || geminiApiKey.isEmpty()) {
            throw new IllegalStateException("Gemini API URL or API Key is not properly configured.");
        }
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        logger.info("Generating email reply for request...");

        String prompt = buildPrompt(emailRequest);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        try {
            logger.debug("Sending request to Gemini API: {}", geminiApiUrl);

            Mono<String> responseMono = webClient.post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class);

            String response = responseMono.block();
            if (response == null) {
                logger.error("Received null response from API.");
                return "Error: Received empty response from API.";
            }

            return extractResponseContent(response);

        } catch (WebClientResponseException e) {
            logger.error("API request failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "Error: API request failed with status " + e.getStatusCode();
        } catch (Exception e) {
            logger.error("Unexpected error during email generation: {}", e.getMessage(), e);
            return "Error: Unable to generate email reply.";
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        logger.debug("Building email generation prompt...");

        StringBuilder prompt = new StringBuilder(
                "Generate an email reply for the following email content. Do not generate a subject line. "
        );

        Optional.ofNullable(emailRequest.getTone())
                .filter(tone -> !tone.isEmpty())
                .ifPresent(tone -> prompt.append("Use a ").append(tone).append(" tone. "));

        prompt.append("Original email:\n").append(emailRequest.getEmailContent());

        logger.debug("Generated prompt: {}", prompt);
        return prompt.toString();
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response);

            JsonNode textNode = Optional.ofNullable(jsonNode)
                    .map(node -> node.path("candidates"))
                    .filter(JsonNode::isArray)
                    .filter(array -> array.size() > 0)
                    .map(array -> array.get(0).path("content").path("parts"))
                    .filter(JsonNode::isArray)
                    .filter(parts -> parts.size() > 0)
                    .map(parts -> parts.get(0).path("text"))
                    .orElse(null);

            if (textNode == null || textNode.isMissingNode()) {
                logger.error("No valid response text found in API response.");
                return "Error: Unable to extract content from API response.";
            }

            return textNode.asText();
        } catch (Exception e) {
            logger.error("Error processing API response: {}", e.getMessage(), e);
            return "Error: Unable to process API response.";
        }
    }
}
