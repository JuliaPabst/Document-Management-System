package org.rest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rest.model.ChatMessage;
import org.rest.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatMessageService {
    
    private final ChatMessageRepository chatMessageRepository;
    
    public ChatMessage saveChatMessage(ChatMessage chatMessage) {
        log.info("Saving chat message with role: {}", chatMessage.getRole());
        ChatMessage saved = chatMessageRepository.save(chatMessage);
        log.info("Chat message saved with ID: {}", saved.getId());
        return saved;
    }
    
    @Transactional(readOnly = true)
    public List<ChatMessage> getAllChatMessages() {
        log.info("Retrieving all chat messages");
        return chatMessageRepository.findByOrderByTimestampAsc();
    }
    
    @Transactional(readOnly = true)
    public List<ChatMessage> getChatMessagesBySession(String sessionId) {
        log.info("Retrieving chat messages for session: {}", sessionId);
        return chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }
    
    public void deleteAllChatMessages() {
        log.info("Deleting all chat messages");
        chatMessageRepository.deleteAll();
        log.info("All chat messages deleted");
    }
    
    public void deleteChatMessagesBySession(String sessionId) {
        log.info("Deleting chat messages for session: {}", sessionId);
        chatMessageRepository.deleteBySessionId(sessionId);
        log.info("Chat messages deleted for session: {}", sessionId);
    }
}
