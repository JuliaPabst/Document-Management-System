package org.rest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rest.config.TestcontainersConfiguration;
import org.rest.dto.ChatCompletionRequestDto;
import org.rest.service.OpenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ChatController
 * Tests the HTTP request/response flow: Request → Controller → OpenAI Service (mocked) → Response
 * OpenAI is mocked to avoid external API dependencies in tests
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672",
        "openai.api.key=test-key"
})
@DisplayName("Chat Controller Integration Tests")
class ChatControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OpenAIService openAIService;

    @BeforeEach
    void setUp() {
        reset(openAIService);
    }

    @Test
    @DisplayName("Should generate chat completion successfully")
    void generateChatCompletion() throws Exception {
        // Arrange
        ChatCompletionRequestDto request = new ChatCompletionRequestDto(
                "How many documents do I have?",
                null
        );

        when(openAIService.generateChatCompletion(any(ChatCompletionRequestDto.class)))
                .thenReturn("You have 5 documents in your system.");

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("You have 5 documents in your system."));

        verify(openAIService, times(1)).generateChatCompletion(any(ChatCompletionRequestDto.class));
    }

    @Test
    @DisplayName("Should handle chat completion with conversation history")
    void generateChatCompletionWithHistory() throws Exception {
        // Arrange
        List<ChatCompletionRequestDto.ConversationMessage> history = Arrays.asList(
                new ChatCompletionRequestDto.ConversationMessage("user", "Hello"),
                new ChatCompletionRequestDto.ConversationMessage("assistant", "Hi! How can I help you?")
        );

        ChatCompletionRequestDto request = new ChatCompletionRequestDto(
                "What documents are in the system?",
                history
        );

        when(openAIService.generateChatCompletion(any(ChatCompletionRequestDto.class)))
                .thenReturn("Based on your previous question, you have several documents including reports and invoices.");

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").isNotEmpty())
                .andExpect(jsonPath("$.response", containsString("documents")));

        verify(openAIService, times(1)).generateChatCompletion(any(ChatCompletionRequestDto.class));
    }

    @Test
    @DisplayName("Should return error when OpenAI service fails")
    void generateChatCompletionWithServiceError() throws Exception {
        // Arrange
        ChatCompletionRequestDto request = new ChatCompletionRequestDto(
                "Tell me about my files",
                null
        );

        when(openAIService.generateChatCompletion(any(ChatCompletionRequestDto.class)))
                .thenThrow(new RuntimeException("OpenAI API error"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        verify(openAIService, times(1)).generateChatCompletion(any(ChatCompletionRequestDto.class));
    }

    @Test
    @DisplayName("Should handle empty message in request")
    void generateChatCompletionWithEmptyMessage() throws Exception {
        // Arrange
        ChatCompletionRequestDto request = new ChatCompletionRequestDto(
                "",
                null
        );

        when(openAIService.generateChatCompletion(any(ChatCompletionRequestDto.class)))
                .thenReturn("How can I help you today?");

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(openAIService, times(1)).generateChatCompletion(any(ChatCompletionRequestDto.class));
    }

    @Test
    @DisplayName("Should handle multi-turn conversation with context")
    void handleMultiTurnConversation() throws Exception {
        // Arrange
        List<ChatCompletionRequestDto.ConversationMessage> history = Arrays.asList(
                new ChatCompletionRequestDto.ConversationMessage("user", "What's the largest file?"),
                new ChatCompletionRequestDto.ConversationMessage("assistant", "The largest file is important-report.pdf at 1MB."),
                new ChatCompletionRequestDto.ConversationMessage("user", "Who uploaded it?")
        );

        ChatCompletionRequestDto request = new ChatCompletionRequestDto(
                "Can you tell me more about this file?",
                history
        );

        when(openAIService.generateChatCompletion(any(ChatCompletionRequestDto.class)))
                .thenReturn("The file important-report.pdf was uploaded by Manager. It's a PDF file with a size of 1MB.");

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response", containsString("Manager")))
                .andExpect(jsonPath("$.response", containsString("important-report.pdf")));

        verify(openAIService, times(1)).generateChatCompletion(any(ChatCompletionRequestDto.class));
    }

    @Test
    @DisplayName("Should handle request with null conversation history")
    void generateChatCompletionWithNullHistory() throws Exception {
        // Arrange
        ChatCompletionRequestDto request = new ChatCompletionRequestDto();
        request.setMessage("Hello");
        request.setConversationHistory(null);

        when(openAIService.generateChatCompletion(any(ChatCompletionRequestDto.class)))
                .thenReturn("Hello! How can I help you with your documents today?");

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").isNotEmpty());

        verify(openAIService, times(1)).generateChatCompletion(any(ChatCompletionRequestDto.class));
    }
}
