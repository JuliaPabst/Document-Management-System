package org.rest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rest.config.TestcontainersConfiguration;
import org.rest.dto.ChatCompletionRequestDto;
import org.rest.dto.ChatMessageRequestDto;
import org.rest.model.ChatMessage;
import org.rest.repository.ChatMessageRepository;
import org.rest.service.OpenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Complete End-to-End Integration Test for Chat Conversation Flow
 * 
 * This test validates the entire chat workflow:
 * 1. User sends a message and saves it to database
 * 2. System retrieves conversation history from database
 * 3. Message is sent to OpenAI with conversation context
 * 4. Assistant response is received and saved to database
 * 5. Full conversation history can be retrieved
 * 6. Conversation can be deleted (new chat)
 * 
 * Uses real PostgreSQL via Testcontainers
 * OpenAI service is mocked to avoid external API costs
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "openai.api.key=test-key"
})
@DisplayName("Chat Conversation E2E Integration Test")
class ChatConversationE2EIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @MockitoBean
    private OpenAIService openAIService;

    private static final String SESSION_ID = "test-session-123";

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
    }

    @Test
    @DisplayName("Should complete full chat conversation flow with multiple messages")
    void completeConversationFlow() throws Exception {
        // =====================================================================
        // STEP 1: User sends first message - save to database
        // =====================================================================
        ChatMessageRequestDto userMessage1 = ChatMessageRequestDto.builder()
                .role("user")
                .content("Hello! How many documents do I have?")
                .sessionId(SESSION_ID)
                .build();

        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userMessage1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.role").value("user"))
                .andExpect(jsonPath("$.content").value("Hello! How many documents do I have?"))
                .andExpect(jsonPath("$.sessionId").value(SESSION_ID));

        // Verify message saved in database
        List<ChatMessage> messagesAfterStep1 = chatMessageRepository.findAll();
        assertThat(messagesAfterStep1).hasSize(1);
        assertThat(messagesAfterStep1.get(0).getContent()).isEqualTo("Hello! How many documents do I have?");

        // =====================================================================
        // STEP 2: Send message to OpenAI (with empty history for first message)
        // =====================================================================
        when(openAIService.generateChatCompletion(any(ChatCompletionRequestDto.class)))
                .thenReturn("You have 5 documents in your system.");

        ChatCompletionRequestDto chatRequest1 = new ChatCompletionRequestDto(
                "Hello! How many documents do I have?",
                null // No history yet
        );

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("You have 5 documents in your system."));

        // =====================================================================
        // STEP 3: Save assistant response to database
        // =====================================================================
        ChatMessageRequestDto assistantMessage1 = ChatMessageRequestDto.builder()
                .role("assistant")
                .content("You have 5 documents in your system.")
                .sessionId(SESSION_ID)
                .build();

        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assistantMessage1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("assistant"));

        // Verify conversation history in database
        List<ChatMessage> messagesAfterStep3 = chatMessageRepository.findBySessionIdOrderByTimestampAsc(SESSION_ID);
        assertThat(messagesAfterStep3).hasSize(2);
        assertThat(messagesAfterStep3.get(0).getRole()).isEqualTo("user");
        assertThat(messagesAfterStep3.get(1).getRole()).isEqualTo("assistant");

        // =====================================================================
        // STEP 4: User sends follow-up message
        // =====================================================================
        ChatMessageRequestDto userMessage2 = ChatMessageRequestDto.builder()
                .role("user")
                .content("Can you summarize them?")
                .sessionId(SESSION_ID)
                .build();

        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userMessage2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Can you summarize them?"));

        // =====================================================================
        // STEP 5: Retrieve conversation history for context
        // =====================================================================
        mockMvc.perform(get("/api/v1/chat-messages/session/{sessionId}", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[0].content").value("Hello! How many documents do I have?"))
                .andExpect(jsonPath("$[1].role").value("assistant"))
                .andExpect(jsonPath("$[1].content").value("You have 5 documents in your system."))
                .andExpect(jsonPath("$[2].role").value("user"))
                .andExpect(jsonPath("$[2].content").value("Can you summarize them?"));

        // =====================================================================
        // STEP 6: Send follow-up to OpenAI with conversation history
        // =====================================================================
        List<ChatCompletionRequestDto.ConversationMessage> history = Arrays.asList(
                new ChatCompletionRequestDto.ConversationMessage("user", "Hello! How many documents do I have?"),
                new ChatCompletionRequestDto.ConversationMessage("assistant", "You have 5 documents in your system.")
        );

        when(openAIService.generateChatCompletion(any(ChatCompletionRequestDto.class)))
                .thenReturn("Your documents include: 2 invoices, 2 reports, and 1 contract.");

        ChatCompletionRequestDto chatRequest2 = new ChatCompletionRequestDto(
                "Can you summarize them?",
                history
        );

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your documents include: 2 invoices, 2 reports, and 1 contract."));

        // =====================================================================
        // STEP 7: Save second assistant response
        // =====================================================================
        ChatMessageRequestDto assistantMessage2 = ChatMessageRequestDto.builder()
                .role("assistant")
                .content("Your documents include: 2 invoices, 2 reports, and 1 contract.")
                .sessionId(SESSION_ID)
                .build();

        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assistantMessage2)))
                .andExpect(status().isCreated());

        // =====================================================================
        // STEP 8: Verify complete conversation in database
        // =====================================================================
        List<ChatMessage> finalConversation = chatMessageRepository.findBySessionIdOrderByTimestampAsc(SESSION_ID);
        assertThat(finalConversation).hasSize(4);
        assertThat(finalConversation.get(0).getContent()).isEqualTo("Hello! How many documents do I have?");
        assertThat(finalConversation.get(1).getContent()).isEqualTo("You have 5 documents in your system.");
        assertThat(finalConversation.get(2).getContent()).isEqualTo("Can you summarize them?");
        assertThat(finalConversation.get(3).getContent()).isEqualTo("Your documents include: 2 invoices, 2 reports, and 1 contract.");

        // Verify all messages have timestamps
        finalConversation.forEach(msg -> assertThat(msg.getTimestamp()).isNotNull());

        // =====================================================================
        // STEP 9: User starts new chat - delete conversation
        // =====================================================================
        mockMvc.perform(delete("/api/v1/chat-messages/session/{sessionId}", SESSION_ID))
                .andExpect(status().isNoContent());

        // Verify conversation deleted
        List<ChatMessage> afterDelete = chatMessageRepository.findBySessionIdOrderByTimestampAsc(SESSION_ID);
        assertThat(afterDelete).isEmpty();

        // =====================================================================
        // STEP 10: Verify new conversation can start
        // =====================================================================
        ChatMessageRequestDto newMessage = ChatMessageRequestDto.builder()
                .role("user")
                .content("Tell me about file management")
                .sessionId(SESSION_ID)
                .build();

        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMessage)))
                .andExpect(status().isCreated());

        List<ChatMessage> newConversation = chatMessageRepository.findBySessionIdOrderByTimestampAsc(SESSION_ID);
        assertThat(newConversation).hasSize(1);
        assertThat(newConversation.get(0).getContent()).isEqualTo("Tell me about file management");

        System.out.println("=".repeat(70));
        System.out.println("CHAT CONVERSATION E2E TEST COMPLETED!");
        System.out.println("=".repeat(70));
    }

    @Test
    @DisplayName("Should handle multiple concurrent sessions independently")
    void handleMultipleSessions() throws Exception {
        String session1 = "session-1";
        String session2 = "session-2";

        // Create messages for session 1
        ChatMessageRequestDto session1Msg1 = ChatMessageRequestDto.builder()
                .role("user")
                .content("Session 1 - Message 1")
                .sessionId(session1)
                .build();

        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(session1Msg1)))
                .andExpect(status().isCreated());

        // Create messages for session 2
        ChatMessageRequestDto session2Msg1 = ChatMessageRequestDto.builder()
                .role("user")
                .content("Session 2 - Message 1")
                .sessionId(session2)
                .build();

        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(session2Msg1)))
                .andExpect(status().isCreated());

        // Verify session 1 has only its messages
        mockMvc.perform(get("/api/v1/chat-messages/session/{sessionId}", session1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("Session 1 - Message 1"))
                .andExpect(jsonPath("$[0].sessionId").value(session1));

        // Verify session 2 has only its messages
        mockMvc.perform(get("/api/v1/chat-messages/session/{sessionId}", session2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("Session 2 - Message 1"))
                .andExpect(jsonPath("$[0].sessionId").value(session2));

        // Delete session 1
        mockMvc.perform(delete("/api/v1/chat-messages/session/{sessionId}", session1))
                .andExpect(status().isNoContent());

        // Verify session 1 deleted but session 2 still exists
        mockMvc.perform(get("/api/v1/chat-messages/session/{sessionId}", session1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/chat-messages/session/{sessionId}", session2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Should validate message constraints")
    void validateMessageConstraints() throws Exception {
        // Missing role
        ChatMessageRequestDto invalidMsg1 = ChatMessageRequestDto.builder()
                .content("Hello")
                .sessionId(SESSION_ID)
                .build();

        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidMsg1)))
                .andExpect(status().isBadRequest());

        // Missing content
        ChatMessageRequestDto invalidMsg2 = ChatMessageRequestDto.builder()
                .role("user")
                .sessionId(SESSION_ID)
                .build();

        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidMsg2)))
                .andExpect(status().isBadRequest());

        // Verify nothing was saved
        assertThat(chatMessageRepository.findAll()).isEmpty();
    }
}
