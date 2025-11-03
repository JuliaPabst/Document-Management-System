package org.rest.repository;

import org.rest.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // Get all messages ordered by timestamp (oldest first)
    List<ChatMessage> findByOrderByTimestampAsc();
    
    // Get messages by session ID ordered by timestamp
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
    
    // Get recent messages (limit)
    @Query("SELECT c FROM ChatMessage c ORDER BY c.timestamp DESC")
    List<ChatMessage> findRecentMessages();
    
    // Delete messages by session ID
    void deleteBySessionId(String sessionId);
}
