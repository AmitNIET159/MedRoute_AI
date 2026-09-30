package com.medroute.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class HuggingFaceClient {

    private static final Logger logger = LoggerFactory.getLogger(HuggingFaceClient.class);

    private final String apiToken;
    private final String model;
    private final String apiUrl;
    private final int timeoutMs;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HuggingFaceClient(
            Dotenv dotenv,
            ObjectMapper objectMapper) {
        String token = dotenv.get("HF_TOKEN");
        this.apiToken = (token != null) ? token : "";
        
        String modelStr = dotenv.get("HF_MODEL");
        this.model = (modelStr != null && !modelStr.trim().isEmpty()) ? modelStr : "mistralai/Mistral-7B-Instruct-v0.3";
        
        String apiUrlStr = dotenv.get("HF_API_URL");
        String url = (apiUrlStr != null && !apiUrlStr.trim().isEmpty()) ? apiUrlStr : "https://api-inference.huggingface.co/models/";
        this.apiUrl = url.endsWith("/") ? url : url + "/";
        
        String timeoutStr = dotenv.get("HF_TIMEOUT_MS");
        this.timeoutMs = (timeoutStr != null && !timeoutStr.trim().isEmpty()) ? Integer.parseInt(timeoutStr) : 15000;
        
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.timeoutMs))
                .build();
    }

    public String getModelName() {
        return model;
    }

    public HFResponse generateInsight(String prompt) {
        if (apiToken == null || apiToken.trim().isEmpty()) {
            logger.warn("HF_TOKEN is not configured. Falling back to deterministic mode.");
            return new HFResponse(null, "FALLBACK", "Token not configured", 0, 0, 0);
        }

        long startTime = System.currentTimeMillis();
        try {
            String fullUrl = apiUrl + model;
            
            // Format for Mistral Instruct
            String formattedPrompt = "<s>[INST] " + prompt + " [/INST]";

            String requestBody = objectMapper.createObjectNode()
                    .put("inputs", formattedPrompt)
                    .set("parameters", objectMapper.createObjectNode()
                            .put("max_new_tokens", 250)
                            .put("temperature", 0.3)
                            .put("return_full_text", false))
                    .toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 429) {
                logger.warn("Hugging Face API rate limited (429)");
                return new HFResponse(null, "TIMEOUT", "Rate limited", (int) duration, prompt.length() / 4, 0);
            }
            if (response.statusCode() >= 400) {
                logger.error("Hugging Face API error {}: {}", response.statusCode(), response.body());
                return new HFResponse(null, "FAILED", "API Error: " + response.statusCode(), (int) duration, prompt.length() / 4, 0);
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            String generatedText = null;

            if (rootNode.isArray() && rootNode.size() > 0 && rootNode.get(0).has("generated_text")) {
                generatedText = rootNode.get(0).get("generated_text").asText();
            }

            if (generatedText == null || generatedText.trim().isEmpty()) {
                logger.warn("Empty response from Hugging Face API");
                return new HFResponse(null, "FALLBACK", "Empty response", (int) duration, prompt.length() / 4, 0);
            }

            return new HFResponse(generatedText.trim(), "SUCCESS", null, (int) duration, prompt.length() / 4, generatedText.length() / 4);

        } catch (java.net.http.HttpTimeoutException e) {
            logger.warn("Hugging Face API timeout");
            return new HFResponse(null, "TIMEOUT", "Request timed out", timeoutMs, prompt.length() / 4, 0);
        } catch (Exception e) {
            logger.error("Exception calling Hugging Face API", e);
            return new HFResponse(null, "FAILED", e.getMessage(), (int) (System.currentTimeMillis() - startTime), prompt.length() / 4, 0);
        }
    }

    public static class HFResponse {
        public final String text;
        public final String status;
        public final String errorReason;
        public final int durationMs;
        public final int inputTokens;
        public final int outputTokens;

        public HFResponse(String text, String status, String errorReason, int durationMs, int inputTokens, int outputTokens) {
            this.text = text;
            this.status = status;
            this.errorReason = errorReason;
            this.durationMs = durationMs;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
        }
    }
}
