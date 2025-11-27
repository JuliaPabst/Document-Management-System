package org.workers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// Service for generating document summaries using OpenAI API
@Service
@Slf4j
public class OpenAIService {
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    @Value("${openai.api.key:}")
    private String apiKey;
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    @Value("${openai.model:gpt-4o-mini}")
    private String model;
    @Value("${openai.temperature:0.3}")
    private double temperature;
    @Value("${openai.max.tokens:300}")
    private int maxTokens;

    public OpenAIService() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    // Generate a summary from OCR extracted text using OpenAI
    public String generateSummary(String ocrText) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key is not configured, returning placeholder summary");
            return generatePlaceholderSummary(ocrText);
        }

        try {
            log.info("Generating summary using OpenAI for text of length: {} chars", ocrText.length());
            
            // Build messages array for OpenAI API
            ArrayNode messages = objectMapper.createArrayNode();
            
            // System message (to define the assistant's role)
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", 
                "You are a document summarization assistant. " +
                "Create a concise, informative summary of the provided document text. " +
                "Focus on the main topics, key points, and important information. " +
                "Keep the summary under 200 words.");
            messages.add(systemMsg);
            
            // User message: The OCR text to summarize
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            // Limit OCR text to avoid token limits (keep first 3000 chars)
            String truncatedText = ocrText.length() > 3000 
                ? ocrText.substring(0, 3000) + "..." 
                : ocrText;
            userMsg.put("content", "Summarize this document:\n\n" + truncatedText);
            messages.add(userMsg);
            
            // Build request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.set("messages", messages);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            
            // Call OpenAI API
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();
            
            log.debug("Calling OpenAI API: {} with model: {}", apiUrl, model);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                log.error("OpenAI API error: {} - {}", response.statusCode(), response.body());
                return generatePlaceholderSummary(ocrText);
            }
            
            // Parse response
            JsonNode responseJson = objectMapper.readTree(response.body());
            String summary = responseJson.get("choices").get(0).get("message").get("content").asText();
            
            log.info("Successfully generated summary using OpenAI (length: {} chars)", summary.length());
            return summary.trim();
            
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            return generatePlaceholderSummary(ocrText);
        }
    }

    // Placeholder summary when OpenAI is not available
    private String generatePlaceholderSummary(String ocrText) {
        int wordCount = ocrText.split("\\s+").length;
        int charCount = ocrText.length();
        String preview = ocrText.substring(0, Math.min(150, ocrText.length())).trim();
        if (ocrText.length() > 150) {
            preview += "...";
        }

        return String.format(
            "Document Summary (Generated without AI)\n\n" +
            "Statistics:\n" +
            "- Text Length: %d characters\n" +
            "- Estimated Words: %d\n\n" +
            "Preview:\n%s",
            charCount,
            wordCount,
            preview
        );
    }
}
