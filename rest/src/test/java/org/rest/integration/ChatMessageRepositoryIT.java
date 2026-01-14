package org.rest.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rest.config.TestcontainersConfiguration;
import org.rest.model.ChatMessage;
import org.rest.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ChatMessageRepository
 * Uses real PostgreSQL via Testcontainers
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("ChatMessage Repository Integration Tests")
class ChatMessageRepositoryIT {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save and retrieve chat message")
    void saveAndRetrieveChatMessage() {
        // Arrange
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent("Hello, how are you?");
        message.setSessionId("session-123");
        message.setTimestamp(Instant.now());

        // Act
        ChatMessage saved = chatMessageRepository.save(message);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo("user");
        assertThat(saved.getContent()).isEqualTo("Hello, how are you?");
        assertThat(saved.getSessionId()).isEqualTo("session-123");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should auto-generate timestamp on persist if not set")
    void autoGenerateTimestampOnPersist() {
        // Arrange
        ChatMessage message = new ChatMessage();
        message.setRole("assistant");
        message.setContent("I'm doing well!");
        message.setSessionId("session-456");
        // Don't set timestamp

        // Act
        ChatMessage saved = chatMessageRepository.save(message);

        // Assert
        assertThat(saved.getTimestamp()).isNotNull();
        assertThat(saved.getTimestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("Should find all messages ordered by timestamp ascending")
    void findByOrderByTimestampAsc() {
        // Arrange
        Instant now = Instant.now();
        ChatMessage msg1 = createMessage("user", "First", "session-1", now.minusSeconds(120));
        ChatMessage msg2 = createMessage("assistant", "Second", "session-1", now.minusSeconds(60));
        ChatMessage msg3 = createMessage("user", "Third", "session-1", now);
        
        chatMessageRepository.saveAll(List.of(msg3, msg1, msg2)); // Save out of order

        // Act
        List<ChatMessage> messages = chatMessageRepository.findByOrderByTimestampAsc();

        // Assert
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getContent()).isEqualTo("First");
        assertThat(messages.get(1).getContent()).isEqualTo("Second");
        assertThat(messages.get(2).getContent()).isEqualTo("Third");
    }

    @Test
    @DisplayName("Should find messages by session ID ordered by timestamp")
    void findBySessionIdOrderByTimestampAsc() {
        // Arrange
        Instant now = Instant.now();
        ChatMessage session1Msg1 = createMessage("user", "Session 1 - Message 1", "session-1", now.minusSeconds(60));
        ChatMessage session1Msg2 = createMessage("assistant", "Session 1 - Message 2", "session-1", now);
        ChatMessage session2Msg1 = createMessage("user", "Session 2 - Message 1", "session-2", now.minusSeconds(30));
        
        chatMessageRepository.saveAll(List.of(session2Msg1, session1Msg1, session1Msg2));

        // Act
        List<ChatMessage> session1Messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc("session-1");

        // Assert
        assertThat(session1Messages).hasSize(2);
        assertThat(session1Messages.get(0).getContent()).isEqualTo("Session 1 - Message 1");
        assertThat(session1Messages.get(1).getContent()).isEqualTo("Session 1 - Message 2");
        assertThat(session1Messages).allMatch(msg -> "session-1".equals(msg.getSessionId()));
    }

    @Test
    @DisplayName("Should return empty list when session has no messages")
    void findByNonexistentSessionId() {
        // Arrange
        ChatMessage message = createMessage("user", "Message", "session-1", Instant.now());
        chatMessageRepository.save(message);

        // Act
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc("nonexistent-session");

        // Assert
        assertThat(messages).isEmpty();
    }

    @Test
    @DisplayName("Should find recent messages ordered by timestamp descending")
    void findRecentMessages() {
        // Arrange
        Instant now = Instant.now();
        ChatMessage msg1 = createMessage("user", "Oldest", "session-1", now.minusSeconds(180));
        ChatMessage msg2 = createMessage("assistant", "Middle", "session-1", now.minusSeconds(60));
        ChatMessage msg3 = createMessage("user", "Newest", "session-1", now);
        
        chatMessageRepository.saveAll(List.of(msg1, msg2, msg3));

        // Act
        List<ChatMessage> recentMessages = chatMessageRepository.findRecentMessages();

        // Assert
        assertThat(recentMessages).hasSize(3);
        // Should be in descending order (newest first)
        assertThat(recentMessages.get(0).getContent()).isEqualTo("Newest");
        assertThat(recentMessages.get(1).getContent()).isEqualTo("Middle");
        assertThat(recentMessages.get(2).getContent()).isEqualTo("Oldest");
    }

    @Test
    @DisplayName("Should delete messages by session ID")
    void deleteBySessionId() {
        // Arrange
        ChatMessage session1Msg1 = createMessage("user", "Session 1 - Message 1", "session-1", Instant.now());
        ChatMessage session1Msg2 = createMessage("assistant", "Session 1 - Message 2", "session-1", Instant.now());
        ChatMessage session2Msg = createMessage("user", "Session 2 - Message 1", "session-2", Instant.now());
        
        chatMessageRepository.saveAll(List.of(session1Msg1, session1Msg2, session2Msg));
        assertThat(chatMessageRepository.findAll()).hasSize(3);

        // Act
        chatMessageRepository.deleteBySessionId("session-1");

        // Assert
        List<ChatMessage> remaining = chatMessageRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getSessionId()).isEqualTo("session-2");
    }

    @Test
    @DisplayName("Should handle multiple sessions with interleaved timestamps")
    void handleMultipleSessionsWithInterleavedTimestamps() {
        // Arrange - Create messages from different sessions with interleaved timestamps
        Instant now = Instant.now();
        ChatMessage session1Msg1 = createMessage("user", "S1-M1", "session-1", now.minusSeconds(100));
        ChatMessage session2Msg1 = createMessage("user", "S2-M1", "session-2", now.minusSeconds(90));
        ChatMessage session1Msg2 = createMessage("assistant", "S1-M2", "session-1", now.minusSeconds(80));
        ChatMessage session2Msg2 = createMessage("assistant", "S2-M2", "session-2", now.minusSeconds(70));
        ChatMessage session1Msg3 = createMessage("user", "S1-M3", "session-1", now.minusSeconds(60));
        
        chatMessageRepository.saveAll(List.of(session1Msg1, session2Msg1, session1Msg2, session2Msg2, session1Msg3));

        // Act
        List<ChatMessage> session1Messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc("session-1");
        List<ChatMessage> session2Messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc("session-2");

        // Assert
        assertThat(session1Messages).hasSize(3);
        assertThat(session1Messages.get(0).getContent()).isEqualTo("S1-M1");
        assertThat(session1Messages.get(1).getContent()).isEqualTo("S1-M2");
        assertThat(session1Messages.get(2).getContent()).isEqualTo("S1-M3");

        assertThat(session2Messages).hasSize(2);
        assertThat(session2Messages.get(0).getContent()).isEqualTo("S2-M1");
        assertThat(session2Messages.get(1).getContent()).isEqualTo("S2-M2");
    }

    @Test
    @DisplayName("Should preserve message order within same timestamp second")
    void preserveMessageOrderWithinSameSecond() {
        // Arrange - Create messages with very close timestamps
        Instant baseTime = Instant.now();
        ChatMessage msg1 = createMessage("user", "First", "session-1", baseTime);
        ChatMessage msg2 = createMessage("assistant", "Second", "session-1", baseTime.plusMillis(100));
        ChatMessage msg3 = createMessage("user", "Third", "session-1", baseTime.plusMillis(200));
        
        chatMessageRepository.saveAll(List.of(msg1, msg2, msg3));

        // Act
        List<ChatMessage> messages = chatMessageRepository.findByOrderByTimestampAsc();

        // Assert
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getContent()).isEqualTo("First");
        assertThat(messages.get(1).getContent()).isEqualTo("Second");
        assertThat(messages.get(2).getContent()).isEqualTo("Third");
    }

    @Test
    @DisplayName("Should delete all messages")
    void deleteAllMessages() {
        // Arrange
        ChatMessage msg1 = createMessage("user", "Message 1", "session-1", Instant.now());
        ChatMessage msg2 = createMessage("assistant", "Message 2", "session-1", Instant.now());
        ChatMessage msg3 = createMessage("user", "Message 3", "session-2", Instant.now());
        
        chatMessageRepository.saveAll(List.of(msg1, msg2, msg3));
        assertThat(chatMessageRepository.findAll()).hasSize(3);

        // Act
        chatMessageRepository.deleteAll();

        // Assert
        assertThat(chatMessageRepository.findAll()).isEmpty();
    }

    // Helper method to create chat messages
    private ChatMessage createMessage(String role, String content, String sessionId, Instant timestamp) {
        ChatMessage message = new ChatMessage();
        message.setRole(role);
        message.setContent(content);
        message.setSessionId(sessionId);
        message.setTimestamp(timestamp);
        return message;
    }
}
