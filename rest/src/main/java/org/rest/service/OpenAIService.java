package org.rest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.rest.dto.ChatCompletionRequestDto;
import org.rest.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for handling OpenAI API interactions
 */
@Service
public class OpenAIService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final FileMetadataService fileMetadataService;
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${openai.model:gpt-4.1-mini}")
    private String model;
    
    @Value("${openai.temperature:0.3}")
    private double temperature;
    
    @Value("${openai.max.tokens:500}")
    private int maxTokens;

    public OpenAIService(FileMetadataService fileMetadataService, ObjectMapper objectMapper) {
        this.fileMetadataService = fileMetadataService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Generate a chat completion using OpenAI API with file metadata context
     */
    public String generateChatCompletion(ChatCompletionRequestDto request) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        try {
            // Build system message with file metadata context
            String systemMessage = buildSystemMessageWithContext();
            
            // Build messages array for OpenAI API
            ArrayNode messages = objectMapper.createArrayNode();
            
            // Add system message
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemMessage);
            messages.add(systemMsg);
            
            // Add conversation history
            if (request.getConversationHistory() != null) {
                for (ChatCompletionRequestDto.ConversationMessage msg : request.getConversationHistory()) {
                    ObjectNode historyMsg = objectMapper.createObjectNode();
                    historyMsg.put("role", msg.getRole());
                    historyMsg.put("content", msg.getContent());
                    messages.add(historyMsg);
                }
            }
            
            // Add user message
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", request.getMessage());
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
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                logger.error("OpenAI API error: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("Failed to get response from OpenAI API: " + response.statusCode());
            }
            
            // Parse response
            JsonNode responseJson = objectMapper.readTree(response.body());
            String assistantMessage = responseJson.get("choices").get(0).get("message").get("content").asText();
            
            return assistantMessage;
            
        } catch (Exception e) {
            logger.error("Error calling OpenAI API", e);
            throw new RuntimeException("An error occurred while processing your request: " + e.getMessage(), e);
        }
    }

    /**
     * Build system message with file metadata context
     */
    private String buildSystemMessageWithContext() {
        StringBuilder systemMessage = new StringBuilder();
        systemMessage.append("You are a helpful assistant for a document management system called Paperless. ");
        systemMessage.append("You help users find information about their documents, answer questions about file metadata, ");
        systemMessage.append("and provide assistance with document management tasks.");
        
        try {
            // Fetch all file metadata
            List<FileMetadata> files = fileMetadataService.getAllFileMetadata();
            
            if (!files.isEmpty()) {
                // Calculate statistics
                int totalFiles = files.size();
                Set<String> uniqueAuthors = files.stream()
                        .map(FileMetadata::getAuthor)
                        .collect(Collectors.toSet());
                
                long totalSize = files.stream()
                        .mapToLong(FileMetadata::getSize)
                        .sum();
                
                // Build context
                systemMessage.append("\n\n=== DOCUMENT DATABASE INFORMATION ===\n");
                systemMessage.append("This information represents the COMPLETE and ACCURATE state of the document database. ");
                systemMessage.append("You MUST use ONLY this data to answer questions. DO NOT make up or hallucinate any information.\n\n");
                
                systemMessage.append("STATISTICS:\n");
                systemMessage.append("- Total Documents: ").append(totalFiles).append("\n");
                systemMessage.append("- Total Authors: ").append(uniqueAuthors.size()).append("\n");
                systemMessage.append("- Total Storage Used: ").append(String.format("%.2f", totalSize / (1024.0 * 1024.0))).append(" MB\n\n");
                
                systemMessage.append("AUTHORS LIST (Complete):\n");
                int authorIndex = 1;
                for (String author : uniqueAuthors.stream().sorted().collect(Collectors.toList())) {
                    systemMessage.append(authorIndex++).append(". ").append(author).append("\n");
                }
                
                systemMessage.append("\nCOMPLETE DOCUMENT LIST:\n");
                for (int i = 0; i < files.size(); i++) {
                    FileMetadata file = files.get(i);
                    systemMessage.append(i + 1).append(". \"").append(file.getFilename()).append("\" by ")
                            .append(file.getAuthor()).append(" (").append(file.getFileType()).append(", ")
                            .append(String.format("%.2f", file.getSize() / 1024.0)).append(" KB, uploaded: ")
                            .append(formatDate(file.getUploadTime())).append(")\n");
                }
                
                systemMessage.append("\nCRITICAL INSTRUCTIONS:\n");
                systemMessage.append("- You have READ-ONLY access to this data\n");
                systemMessage.append("- ALWAYS base your answers on the data above - DO NOT hallucinate or make up information\n");
                systemMessage.append("- If a user asks about documents, authors, or statistics, count and reference the data above\n");
                systemMessage.append("- When asked \"how many\", count from the lists above\n");
                systemMessage.append("- When asked about specific files or authors, search in the lists above\n");
                systemMessage.append("- If information is not in the data above, say \"I don't have information about that in the current database\"\n");
                systemMessage.append("- You CANNOT execute SQL queries or modify the database\n");
                systemMessage.append("- All data is pre-fetched and sanitized for security\n\n");
                systemMessage.append("Remember this information for the entire conversation. Use it to answer all questions about documents.");
            }
        } catch (Exception e) {
            logger.error("Failed to fetch file metadata for context", e);
            systemMessage.append("\n\nNote: Could not fetch document database information. Please try again later.");
        }
        
        return systemMessage.toString();
    }
    
    private String formatDate(Instant instant) {
        if (instant == null) return "N/A";
        return DateTimeFormatter.ofPattern("MM/dd/yyyy")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}
