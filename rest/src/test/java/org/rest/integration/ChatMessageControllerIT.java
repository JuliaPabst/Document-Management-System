package org.rest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rest.config.TestcontainersConfiguration;
import org.rest.dto.ChatMessageRequestDto;
import org.rest.model.ChatMessage;
import org.rest.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ChatMessageController
 * Tests CRUD operations for chat conversation history
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672"
})
@DisplayName("ChatMessage Controller Integration Tests")
class ChatMessageControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save chat message successfully")
    @Transactional
    void saveChatMessage() throws Exception {
        // Arrange
        ChatMessageRequestDto request = ChatMessageRequestDto.builder()
                .role("user")
                .content("Hello, how are you?")
                .sessionId("session-123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.role").value("user"))
                .andExpect(jsonPath("$.content").value("Hello, how are you?"))
                .andExpect(jsonPath("$.sessionId").value("session-123"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        // Verify database
        List<ChatMessage> messages = chatMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getRole()).isEqualTo("user");
        assertThat(messages.get(0).getContent()).isEqualTo("Hello, how are you?");
        assertThat(messages.get(0).getSessionId()).isEqualTo("session-123");
    }

    @Test
    @DisplayName("Should return 400 when role is missing")
    void saveChatMessageWithMissingRole() throws Exception {
        // Arrange
        ChatMessageRequestDto request = ChatMessageRequestDto.builder()
                .content("Hello")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify nothing was saved
        assertThat(chatMessageRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should return 400 when content is missing")
    void saveChatMessageWithMissingContent() throws Exception {
        // Arrange
        ChatMessageRequestDto request = ChatMessageRequestDto.builder()
                .role("user")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify nothing was saved
        assertThat(chatMessageRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should retrieve all chat messages ordered by timestamp")
    @Transactional
    void getAllChatMessages() throws Exception {
        // Arrange - Create multiple messages
        ChatMessage msg1 = createChatMessage("user", "First message", "session-1", Instant.now().minusSeconds(120));
        ChatMessage msg2 = createChatMessage("assistant", "Second message", "session-1", Instant.now().minusSeconds(60));
        ChatMessage msg3 = createChatMessage("user", "Third message", "session-2", Instant.now());
        chatMessageRepository.saveAll(List.of(msg1, msg2, msg3));

        // Act & Assert
        mockMvc.perform(get("/api/v1/chat-messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].content").value("First message"))
                .andExpect(jsonPath("$[1].content").value("Second message"))
                .andExpect(jsonPath("$[2].content").value("Third message"));
    }

    @Test
    @DisplayName("Should retrieve chat messages by session ID")
    @Transactional
    void getChatMessagesBySession() throws Exception {
        // Arrange - Create messages with different sessions
        ChatMessage msg1 = createChatMessage("user", "Session 1 - Message 1", "session-1", Instant.now().minusSeconds(60));
        ChatMessage msg2 = createChatMessage("assistant", "Session 1 - Message 2", "session-1", Instant.now());
        ChatMessage msg3 = createChatMessage("user", "Session 2 - Message 1", "session-2", Instant.now());
        chatMessageRepository.saveAll(List.of(msg1, msg2, msg3));

        // Act & Assert - Get messages for session-1
        mockMvc.perform(get("/api/v1/chat-messages")
                        .param("sessionId", "session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].sessionId", everyItem(is("session-1"))))
                .andExpect(jsonPath("$[0].content").value("Session 1 - Message 1"))
                .andExpect(jsonPath("$[1].content").value("Session 1 - Message 2"));
    }

    @Test
    @DisplayName("Should return empty array when no messages exist for session")
    @Transactional
    void getChatMessagesByNonexistentSession() throws Exception {
        // Arrange
        ChatMessage msg = createChatMessage("user", "Message", "session-1", Instant.now());
        chatMessageRepository.save(msg);

        // Act & Assert
        mockMvc.perform(get("/api/v1/chat-messages")
                        .param("sessionId", "nonexistent-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return empty array when no messages exist")
    void getAllChatMessagesWhenEmpty() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/chat-messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should delete all chat messages")
    @Transactional
    void deleteAllChatMessages() throws Exception {
        // Arrange - Create messages
        ChatMessage msg1 = createChatMessage("user", "Message 1", "session-1", Instant.now());
        ChatMessage msg2 = createChatMessage("assistant", "Message 2", "session-1", Instant.now());
        ChatMessage msg3 = createChatMessage("user", "Message 3", "session-2", Instant.now());
        chatMessageRepository.saveAll(List.of(msg1, msg2, msg3));

        // Verify messages exist
        assertThat(chatMessageRepository.findAll()).hasSize(3);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/chat-messages"))
                .andExpect(status().isNoContent());

        // Verify all messages deleted
        assertThat(chatMessageRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should delete chat messages by session ID")
    @Transactional
    void deleteChatMessagesBySession() throws Exception {
        // Arrange - Create messages with different sessions
        ChatMessage msg1 = createChatMessage("user", "Session 1 - Message 1", "session-1", Instant.now());
        ChatMessage msg2 = createChatMessage("assistant", "Session 1 - Message 2", "session-1", Instant.now());
        ChatMessage msg3 = createChatMessage("user", "Session 2 - Message 1", "session-2", Instant.now());
        chatMessageRepository.saveAll(List.of(msg1, msg2, msg3));

        // Verify messages exist
        assertThat(chatMessageRepository.findAll()).hasSize(3);

        // Act & Assert - Delete session-1 messages
        mockMvc.perform(delete("/api/v1/chat-messages")
                        .param("sessionId", "session-1"))
                .andExpect(status().isNoContent());

        // Verify only session-1 messages deleted
        List<ChatMessage> remaining = chatMessageRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getSessionId()).isEqualTo("session-2");
    }

    @Test
    @DisplayName("Should save and retrieve conversation sequence")
    @Transactional
    void saveAndRetrieveConversationSequence() throws Exception {
        // Simulate a full conversation
        String sessionId = "conversation-test";

        // User message
        ChatMessageRequestDto userMsg1 = ChatMessageRequestDto.builder()
                .role("user")
                .content("How many documents do I have?")
                .sessionId(sessionId)
                .build();
        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userMsg1)))
                .andExpect(status().isCreated());

        // Assistant response
        ChatMessageRequestDto assistantMsg1 = ChatMessageRequestDto.builder()
                .role("assistant")
                .content("You have 10 documents in your system.")
                .sessionId(sessionId)
                .build();
        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assistantMsg1)))
                .andExpect(status().isCreated());

        // User follow-up
        ChatMessageRequestDto userMsg2 = ChatMessageRequestDto.builder()
                .role("user")
                .content("What about PDFs?")
                .sessionId(sessionId)
                .build();
        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userMsg2)))
                .andExpect(status().isCreated());

        // Assistant follow-up
        ChatMessageRequestDto assistantMsg2 = ChatMessageRequestDto.builder()
                .role("assistant")
                .content("You have 7 PDF documents.")
                .sessionId(sessionId)
                .build();
        mockMvc.perform(post("/api/v1/chat-messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assistantMsg2)))
                .andExpect(status().isCreated());

        // Retrieve full conversation
        mockMvc.perform(get("/api/v1/chat-messages")
                        .param("sessionId", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[0].content").value("How many documents do I have?"))
                .andExpect(jsonPath("$[1].role").value("assistant"))
                .andExpect(jsonPath("$[1].content").value("You have 10 documents in your system."))
                .andExpect(jsonPath("$[2].role").value("user"))
                .andExpect(jsonPath("$[2].content").value("What about PDFs?"))
                .andExpect(jsonPath("$[3].role").value("assistant"))
                .andExpect(jsonPath("$[3].content").value("You have 7 PDF documents."));
    }

    @Test
    @DisplayName("Should handle multiple concurrent sessions")
    @Transactional
    void handleMultipleConcurrentSessions() throws Exception {
        // Create messages for different sessions
        ChatMessage session1Msg1 = createChatMessage("user", "Session 1 Q1", "session-1", Instant.now().minusSeconds(10));
        ChatMessage session2Msg1 = createChatMessage("user", "Session 2 Q1", "session-2", Instant.now().minusSeconds(9));
        ChatMessage session1Msg2 = createChatMessage("assistant", "Session 1 A1", "session-1", Instant.now().minusSeconds(8));
        ChatMessage session2Msg2 = createChatMessage("assistant", "Session 2 A1", "session-2", Instant.now().minusSeconds(7));
        ChatMessage session1Msg3 = createChatMessage("user", "Session 1 Q2", "session-1", Instant.now().minusSeconds(6));
        
        chatMessageRepository.saveAll(List.of(session1Msg1, session2Msg1, session1Msg2, session2Msg2, session1Msg3));

        // Verify session 1 has 3 messages
        mockMvc.perform(get("/api/v1/chat-messages")
                        .param("sessionId", "session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].sessionId", everyItem(is("session-1"))));

        // Verify session 2 has 2 messages
        mockMvc.perform(get("/api/v1/chat-messages")
                        .param("sessionId", "session-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].sessionId", everyItem(is("session-2"))));

        // Verify all messages returns 5
        mockMvc.perform(get("/api/v1/chat-messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    // Helper method to create test chat messages
    private ChatMessage createChatMessage(String role, String content, String sessionId, Instant timestamp) {
        ChatMessage message = new ChatMessage();
        message.setRole(role);
        message.setContent(content);
        message.setSessionId(sessionId);
        message.setTimestamp(timestamp);
        return message;
    }
}
